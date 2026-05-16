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
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.getCompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfCompaniesHouseOfficers
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberCompaniesHouseOfficersForm
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.ConfirmCompaniesHouseOfficersPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.NumberOfCompaniesHouseOfficersPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.UpdateCompaniesHouseOfficersPage

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
  individualProvideDetailsService: IndividualProvideDetailsService,
  companiesHouseService: CompaniesHouseService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[
    Seq[IndividualName] *: List[IndividualProvidedDetails] *: BusinessPartnerRecordResponse *: IsIncorporated *: DataWithAuth
  ] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
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
          .add[List[IndividualProvidedDetails]](individualsList.filter(_.isPersonOfControl)) // important we don't include other relevant individuals
          .add[Seq[IndividualName]](companiesHouseOfficersNames)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication = request.get[IsIncorporated]
        val individuals = request.get[List[IndividualProvidedDetails]]
        val companiesHouseOfficers = request.get[Seq[IndividualName]]

        companiesHouseOfficers.size match
          case 0 =>
            val updatedApplication = updateApplicationWithZeroOfficers(
              agentApplication = agentApplication
            )
            agentApplicationService.upsert(updatedApplication).map: _ =>
              Redirect(AppRoutes.apply.listdetails.otherrelevantindividuals.MandatoryRelevantIndividualsController.show.url)
          case n if n >= 1 && n <= 5 =>
            renderFiveOrLessPage(
              agentApplication,
              request.get[BusinessPartnerRecordResponse].getEntityName,
              companiesHouseOfficers,
              individuals
            )
          case n if n >= 6 =>
            renderSixOrMorePage(
              agentApplication,
              request.get[BusinessPartnerRecordResponse].getEntityName,
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
              confirmCompaniesHouseOfficersPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                agentApplication = agentApplication,
                individualNameList = companiesHouseOfficers
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
                  numberOfCompaniesHouseOfficers = companiesHouseOfficers.size,
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
              yield Ok(updateCompaniesHouseOfficersPage(
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                agentApplication = agentApplication
              ))

            case true =>
              val updatedApplication = updateApplicationWithOfficerCount(
                agentApplication = agentApplication,
                numberOfCompaniesHouseOfficers = FiveOrLessOfficers(
                  numberOfCompaniesHouseOfficers = companiesHouseOfficers.size,
                  isCompaniesHouseOfficersListCorrect = isCompaniesHouseOfficersListCorrect
                )
              )

              val insertIndividualProvideDetails =
                if (individuals.nonEmpty)
                  Future.successful(())
                else
                  Future.traverse(companiesHouseOfficers.toList)(validName =>
                    for
                      personReference <- individualProvideDetailsService.generateNewPersonReference()
                      individualProvidedDetails <- individualProvideDetailsService.create(
                        individualName = validName,
                        isPersonOfControl = true,
                        agentApplicationId = agentApplication.agentApplicationId
                      )
                      _ = logger.debug(s"Inserting individual $personReference...")
                      _ <- individualProvideDetailsService.upsertForApplication(individualProvidedDetails)
                      _ = logger.debug(s"Inserted individual $personReference")
                    yield ()
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
            Future.successful(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size, businessTypeKey(request.get[IsIncorporated])))
        ,
        resultToServeWhenFormHasErrors =
          implicit request =>
            given RequestHeader = request
            formWithErrors =>
              val agentApplication = request.get[IsIncorporated]
              val companiesHouseOfficers = request.get[Seq[IndividualName]]
              numberOfCompaniesHouseOfficersPage(
                form = formWithErrors,
                entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
                companiesHouseOfficersCount = companiesHouseOfficers.size,
                businessTypeKey = businessTypeKey(agentApplication)
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
          yield Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  // Private helper methods

  private def renderFiveOrLessPage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[IndividualName],
    individuals: List[IndividualProvidedDetails]
  )(using RequestWithData[?]): Future[Result] =

    // If we present different list always confirm the list
    val listConfirmation: Option[Boolean] =
      if (individuals.nonEmpty)
        agentApplication
          .getNumberOfCompaniesHouseOfficers
          .collect { case FiveOrLessOfficers(_, isCorrect) => isCorrect }
      else
        None

    Future.successful(Ok(confirmCompaniesHouseOfficersPage(
      form =
        listConfirmation
          .fold(ConfirmCompaniesHouseOfficersForm.form)(ConfirmCompaniesHouseOfficersForm.form.fill),
      entityName = entityName,
      agentApplication = agentApplication,
      individualNameList = companiesHouseOfficers // we must present the list from CH - this is what user is confirming
    )))

  private def renderSixOrMorePage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[IndividualName]
  )(using RequestWithData[?]): Future[Result] = Future.successful(Ok(numberOfCompaniesHouseOfficersPage(
    form =
      agentApplication
        .getNumberOfCompaniesHouseOfficers
        .collect {
          case SixOrMoreOfficers(_, x) => x
        }.fold(NumberCompaniesHouseOfficersForm.form(
          companiesHouseOfficers.size,
          businessTypeKey(agentApplication)
        ))(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size, businessTypeKey(agentApplication)).fill),
    entityName = entityName,
    companiesHouseOfficersCount = companiesHouseOfficers.size,
    businessTypeKey = businessTypeKey(agentApplication)
  )))

  private def updateApplicationWithOfficerCount(
    agentApplication: IsIncorporated,
    numberOfCompaniesHouseOfficers: NumberOfCompaniesHouseOfficers
  ): IsIncorporated =
    agentApplication match
      case application: AgentApplicationLimitedCompany => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationLlp => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))
      case application: AgentApplicationScottishLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfCompaniesHouseOfficers))

  private def updateApplicationWithZeroOfficers(
    agentApplication: IsIncorporated
  ): IsIncorporated =
    val zeroOfficers = FiveOrLessOfficers(
      numberOfCompaniesHouseOfficers = 0,
      isCompaniesHouseOfficersListCorrect = true
    )
    agentApplication match
      case application: AgentApplicationLimitedCompany =>
        application
          .modify(_.numberOfIndividuals).setTo(Some(zeroOfficers))
          .modify(_.hasOtherRelevantIndividuals).setTo(Some(true))
      case application: AgentApplicationLimitedPartnership =>
        application
          .modify(_.numberOfIndividuals).setTo(Some(zeroOfficers))
          .modify(_.hasOtherRelevantIndividuals).setTo(Some(true))
      case application: AgentApplicationLlp =>
        application
          .modify(_.numberOfIndividuals).setTo(Some(zeroOfficers))
          .modify(_.hasOtherRelevantIndividuals).setTo(Some(true))
      case application: AgentApplicationScottishLimitedPartnership =>
        application
          .modify(_.numberOfIndividuals).setTo(Some(zeroOfficers))
          .modify(_.hasOtherRelevantIndividuals).setTo(Some(true))

  def businessTypeKey(agentApplication: IsIncorporated): String =
    agentApplication match
      case _: AgentApplicationLimitedCompany => "LimitedCompany"
      case _: AgentApplicationLlp => "LimitedLiabilityPartnership"
      case _: AgentApplication.IsPartnership => "Partnership"
