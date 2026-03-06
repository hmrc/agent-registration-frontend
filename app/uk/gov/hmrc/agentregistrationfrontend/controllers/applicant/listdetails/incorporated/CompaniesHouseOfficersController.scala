/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.incorporated

import com.softwaremill.quicklens.modify
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.getCompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfCompaniesHouseOfficers
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData.*
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.NumberOfCompaniesHouseOfficersPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.ConfirmCompaniesHouseOfficersPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.UpdateCompaniesHouseOfficersPage
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class CompaniesHouseOfficersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  confirmCompaniesHouseOfficersPage: ConfirmCompaniesHouseOfficersPage,
  numberOfCompaniesHouseOfficersPage: NumberOfCompaniesHouseOfficersPage,
  updateCompaniesHouseOfficersPage: UpdateCompaniesHouseOfficersPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  companiesHouseService: CompaniesHouseService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[Seq[IndividualName] *: List[IndividualProvidedDetails] *: IsIncorporated *: DataWithAuth] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsNotIncorporated =>
            logger.warn(
              "NotIncorporated businesses do not have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)

          case aa: IsIncorporated => request.replace[AgentApplication, IsIncorporated](aa)
    .refine:
      implicit request =>
        val agentApplication: IsIncorporated = request.get[IsIncorporated]
        for
          individualsList <- individualProvideDetailsService
            .findAllKeyIndividualsByApplicationId(agentApplication.agentApplicationId)

          companiesHouseOfficers <- companiesHouseService
            .getActiveOfficers(agentApplication.getCrn, agentApplication.getCompaniesHouseOfficerRole)

          companiesHouseOfficersNames = companiesHouseOfficers
            .map(x => CompaniesHouseOfficer.normaliseOfficerName(x.name))
            .map(IndividualName(_))
            .filter(_.isValidName)
        yield request
          .add[List[IndividualProvidedDetails]](individualsList)
          .add[Seq[IndividualName]](companiesHouseOfficersNames)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication = request.get[IsIncorporated]
        val individuals = request.get[List[IndividualProvidedDetails]]
        val companiesHouseOfficers = request.get[Seq[IndividualName]]

        getEntityName(agentApplication).map: entityName =>
          companiesHouseOfficers.size match
            case 0 =>
              Ok(updateCompaniesHouseOfficersPage(
                entityName = entityName,
                agentApplication = agentApplication
              ))
            case n if n >= 1 && n <= 5 =>
              renderFiveOrLessPage(
                agentApplication,
                entityName,
                companiesHouseOfficers,
                individuals
              )
            case n if n >= 6 =>
              renderSixOrMorePage(
                agentApplication,
                entityName,
                companiesHouseOfficers
              )

  def submitFiveOrLess: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Boolean](
        form = ConfirmCompaniesHouseOfficersForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            formWithErrors =>
              val agentApplication = request.get[IsIncorporated]
              val companiesHouseOfficers = request.get[Seq[IndividualName]]
              val individuals = request.get[List[IndividualProvidedDetails]]

              getEntityName(agentApplication).map: entityName =>
                confirmCompaniesHouseOfficersPage(
                  form = formWithErrors,
                  entityName = entityName,
                  agentApplication = agentApplication,
                  individualNameList =
                    if (individuals.nonEmpty)
                      individuals.map(_.individualName)
                    else
                      companiesHouseOfficers
                )
      )
      .async:
        implicit request =>
          val isCompaniesHouseOfficersListCorrect: Boolean = request.get[Boolean]
          val agentApplication: IsIncorporated = request.get[IsIncorporated]
          val individuals: List[IndividualProvidedDetails] = request.get[List[IndividualProvidedDetails]]
          val companiesHouseOfficers: Seq[IndividualName] = request.get[Seq[IndividualName]]

          isCompaniesHouseOfficersListCorrect match
            case false =>
              val updatedApplication = updateApplicationWithOfficerCount(
                agentApplication = agentApplication,
                numberOfCompaniesHouseOfficers = FiveOrLessOfficers(
                  numberOfCompaniesHouseOfficers =
                    if (individuals.nonEmpty)
                      individuals.size
                    else
                      companiesHouseOfficers.size,
                  isCompaniesHouseOfficersListCorrect = isCompaniesHouseOfficersListCorrect
                )
              )

              val deleteIndividualProvideDetails =
                if (individuals.isEmpty)
                  Future.successful(())
                else
                  Future.traverse(individuals)(i =>
                    individualProvideDetailsService.delete(i.individualProvidedDetailsId)
                  )

              for
                _ <- deleteIndividualProvideDetails
                _ <- agentApplicationService.upsert(updatedApplication)
                entityName <- getEntityName(agentApplication)
              yield Ok(updateCompaniesHouseOfficersPage(
                entityName = entityName,
                agentApplication = agentApplication
              ))

            case true =>
              val updatedApplication = updateApplicationWithOfficerCount(
                agentApplication = agentApplication,
                numberOfCompaniesHouseOfficers = FiveOrLessOfficers(
                  numberOfCompaniesHouseOfficers =
                    if (individuals.nonEmpty)
                      individuals.size
                    else
                      companiesHouseOfficers.size,
                  isCompaniesHouseOfficersListCorrect = isCompaniesHouseOfficersListCorrect
                )
              )

              val insertIndividualProvideDetails =
                if (individuals.nonEmpty)
                  Future.successful(())
                else
                  Future.traverse(companiesHouseOfficers.toList)(valideName =>
                    individualProvideDetailsService.upsertForApplication(
                      individualProvideDetailsService.create(
                        individualName = valideName,
                        isPersonOfControl = true,
                        agentApplicationId = agentApplication.agentApplicationId
                      )
                    )
                  )

              for
                _ <- insertIndividualProvideDetails
                _ <- agentApplicationService.upsert(updatedApplication)
              yield Redirect(AppRoutes.apply.listdetails.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  def submitSixOrMore: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Int](
        form =
          request =>
            val companiesHouseOfficers = request.get[Seq[IndividualName]]
            Future.successful(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size))
        ,
        resultToServeWhenFormHasErrors =
          implicit request =>
            given RequestHeader = request
            formWithErrors =>
              val agentApplication = request.get[IsIncorporated]
              val companiesHouseOfficers = request.get[Seq[IndividualName]]

              getEntityName(agentApplication).map: entityName =>
                numberOfCompaniesHouseOfficersPage(
                  form = formWithErrors,
                  entityName = entityName,
                  agentApplication = agentApplication,
                  companiesHouseOfficersCount = companiesHouseOfficers.size
                )
      )
      .async:
        implicit request =>
          val numberOfOfficersResponsibleForTaxMatters: Int = request.get[Int]
          val agentApplication: IsIncorporated = request.get[IsIncorporated]
          val companiesHouseOfficers: Seq[IndividualName] = request.get[Seq[IndividualName]]

          val numberOfCompaniesHouseOfficers = SixOrMoreOfficers(
            companiesHouseOfficers.size,
            numberOfOfficersResponsibleForTaxMatters
          )

          val updatedApplication = updateApplicationWithOfficerCount(
            agentApplication,
            numberOfCompaniesHouseOfficers
          )

          for _ <- agentApplicationService.upsert(updatedApplication)
          yield Redirect(AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.show.url)
      .redirectIfSaveForLater

  // Private helper methods

  private def getEntityName(agentApplication: IsIncorporated)(using RequestHeader): Future[String] = businessPartnerRecordService
    .getBusinessPartnerRecord(agentApplication.getUtr)
    .map(_.map(_.getEntityName).getOrThrowExpectedDataMissing("Business Partner Record is missing"))

  private def renderFiveOrLessPage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[IndividualName],
    individuals: List[IndividualProvidedDetails]
  )(using RequestWithData[?]): Result = {

    // If we present different list always confirm the list
    val listConfirmation: Option[Boolean] =
      if (individuals.nonEmpty)
        agentApplication
          .getNumberOfCompaniesHouseOfficers
          .collect { case FiveOrLessOfficers(_, isCorrect) => isCorrect }
      else
        None

    Ok(confirmCompaniesHouseOfficersPage(
      form =
        listConfirmation
          .fold(ConfirmCompaniesHouseOfficersForm.form)(ConfirmCompaniesHouseOfficersForm.form.fill),
      entityName = entityName,
      agentApplication = agentApplication,
      individualNameList =
        if (individuals.nonEmpty)
          individuals.map(_.individualName)
        else
          companiesHouseOfficers
    ))
  }

  private def renderSixOrMorePage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[IndividualName]
  )(using RequestWithData[?]): Result = {

    Ok(numberOfCompaniesHouseOfficersPage(
      form =
        agentApplication
          .getNumberOfCompaniesHouseOfficers
          .collect {
            case SixOrMoreOfficers(_, x) => x
          }.fold(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size))(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size).fill),
      entityName = entityName,
      agentApplication = agentApplication,
      companiesHouseOfficersCount = companiesHouseOfficers.size
    ))
  }

  private def updateApplicationWithOfficerCount(
    agentApplication: IsIncorporated,
    numberOfCompaniesHouseOfficers: NumberOfCompaniesHouseOfficers
  ): IsIncorporated =
    agentApplication match
      case application: AgentApplicationLimitedCompany => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationLlp => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationScottishLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
