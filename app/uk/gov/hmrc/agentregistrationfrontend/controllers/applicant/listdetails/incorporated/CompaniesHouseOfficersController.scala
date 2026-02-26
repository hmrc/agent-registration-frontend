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
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=

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

  private val baseAction: ActionBuilderWithData[Seq[CompaniesHouseOfficer] *: List[IndividualProvidedDetails] *: IsIncorporated *: DataWithAuth] = actions
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
          officers <- getActiveOfficersForApplication(agentApplication)
        yield request
          .add[List[IndividualProvidedDetails]](individualsList)
          .add[Seq[CompaniesHouseOfficer]](officers)

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val agentApplication = request.get[IsIncorporated]
        val individuals = request.get[List[IndividualProvidedDetails]]
        val companiesHouseOfficers = request.get[Seq[CompaniesHouseOfficer]]

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
              val companiesHouseOfficers = request.get[Seq[CompaniesHouseOfficer]]

              getEntityName(agentApplication).map: entityName =>
                confirmCompaniesHouseOfficersPage(
                  form = formWithErrors,
                  entityName = entityName,
                  agentApplication = agentApplication,
                  companiesHouseOfficers = companiesHouseOfficers
                )
      )
      .async:
        implicit request =>
          val isCompaniesHouseOfficersListCorrect: Boolean = request.get[Boolean]
          val agentApplication: IsIncorporated = request.get[IsIncorporated]
          val individuals: List[IndividualProvidedDetails] = request.get[List[IndividualProvidedDetails]]
          val companiesHouseOfficers: Seq[CompaniesHouseOfficer] = request.get[Seq[CompaniesHouseOfficer]]

          isCompaniesHouseOfficersListCorrect match
            case false =>
              getEntityName(agentApplication).map: entityName =>
                Ok(updateCompaniesHouseOfficersPage(
                  entityName = entityName,
                  agentApplication = agentApplication
                ))
            case true =>
              val numberOfCompaniesHouseOfficers = FiveOrLessOfficers(
                companiesHouseOfficers.size,
                isCompaniesHouseOfficersListCorrect
              )

              val updatedApplication = updateApplicationWithOfficerCount(
                agentApplication,
                numberOfCompaniesHouseOfficers
              )

              for
                _ <- syncIndividualsWithCompaniesHouse(
                  agentApplication,
                  individuals,
                  companiesHouseOfficers
                )
                _ <- agentApplicationService.upsert(updatedApplication)
              yield Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  def submitSixOrMore: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[Int](
        form =
          request =>
            val companiesHouseOfficers = request.get[Seq[CompaniesHouseOfficer]]
            Future.successful(NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size))
        ,
        resultToServeWhenFormHasErrors =
          implicit request =>
            given RequestHeader = request
            formWithErrors =>
              val agentApplication = request.get[IsIncorporated]
              val companiesHouseOfficers = request.get[Seq[CompaniesHouseOfficer]]

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
          val companiesHouseOfficers: Seq[CompaniesHouseOfficer] = request.get[Seq[CompaniesHouseOfficer]]

          val numberOfCompaniesHouseOfficers = SixOrMoreOfficers(
            companiesHouseOfficers.size,
            numberOfOfficersResponsibleForTaxMatters
          )

          val updatedApplication = updateApplicationWithOfficerCount(
            agentApplication,
            numberOfCompaniesHouseOfficers
          )

          for
            _ <- agentApplicationService.upsert(updatedApplication)
          yield Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url)
      .redirectIfSaveForLater

  // Private helper methods

  private def getEntityName(agentApplication: IsIncorporated)(using RequestHeader): Future[String] = businessPartnerRecordService
    .getBusinessPartnerRecord(agentApplication.getUtr)
    .map(_.map(_.getEntityName).getOrThrowExpectedDataMissing("Business Partner Record is missing"))

  private def getActiveOfficersForApplication(agentApplication: IsIncorporated)(using RequestHeader): Future[Seq[CompaniesHouseOfficer]] =
    companiesHouseService.getActiveOfficers(
      agentApplication.getCrn,
      agentApplication.getCompaniesHouseOfficerRole
    )

  private def renderFiveOrLessPage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[CompaniesHouseOfficer],
    individuals: List[IndividualProvidedDetails]
  )(using RequestWithData[?]): Result =
    val companiesHouseListChanged = hasChangedCompaniesHouseOfficersList(
      individuals,
      companiesHouseOfficers.toList
    )

    val isCompaniesHouseOfficersListCorrect = agentApplication.getNumberOfCompaniesHouseOfficers.flatMap {
      case FiveOrLessOfficers(_, isCorrect) => Some(isCorrect)
      case _ => None
    }

    Ok(confirmCompaniesHouseOfficersPage(
      form =
        Option.when(!companiesHouseListChanged)(isCompaniesHouseOfficersListCorrect).flatten
          .fold(ConfirmCompaniesHouseOfficersForm.form)(ConfirmCompaniesHouseOfficersForm.form.fill),
      entityName = entityName,
      agentApplication = agentApplication,
      companiesHouseOfficers = companiesHouseOfficers
    ))

  private def renderSixOrMorePage(
    agentApplication: IsIncorporated,
    entityName: String,
    companiesHouseOfficers: Seq[CompaniesHouseOfficer]
  )(using RequestWithData[?]): Result = Ok(numberOfCompaniesHouseOfficersPage(
    form = NumberCompaniesHouseOfficersForm.form(companiesHouseOfficers.size),
    entityName = entityName,
    agentApplication = agentApplication,
    companiesHouseOfficersCount = companiesHouseOfficers.size
  ))

  private def updateApplicationWithOfficerCount(
    agentApplication: IsIncorporated,
    numberOfOfficers: NumberOfCompaniesHouseOfficers
  ): IsIncorporated =
    agentApplication match
      case application: AgentApplicationLimitedCompany => application.modify(_.numberOfIndividuals).setTo(Some(numberOfOfficers))
      case application: AgentApplicationLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfOfficers))
      case application: AgentApplicationLlp => application.modify(_.numberOfIndividuals).setTo(Some(numberOfOfficers))
      case application: AgentApplicationScottishLimitedPartnership => application.modify(_.numberOfIndividuals).setTo(Some(numberOfOfficers))

  private def syncIndividualsWithCompaniesHouse(
    agentApplication: IsIncorporated,
    existingIndividuals: List[IndividualProvidedDetails],
    companiesHouseOfficers: Seq[CompaniesHouseOfficer]
  )(using RequestHeader): Future[Unit] =
    for
      _ <-
        Future.traverse(existingIndividuals)(i =>
          individualProvideDetailsService.delete(i.individualProvidedDetailsId)
        )
      _ <-
        Future.traverse(companiesHouseOfficers.toList)(officer =>
          individualProvideDetailsService.upsertForApplication(
            individualProvideDetailsService.create(
              individualName = IndividualName(officer.name),
              isPersonOfControl = true,
              agentApplicationId = agentApplication.agentApplicationId
            )
          )
        )
    yield ()

  private def hasChangedCompaniesHouseOfficersList(
    individualProvidedDetailsList: List[IndividualProvidedDetails],
    companiesHouseOfficersNames: List[CompaniesHouseOfficer]
  ): Boolean =
    if individualProvidedDetailsList.isEmpty then false
    else
      def normalise(s: String): String = s.trim.toLowerCase.replaceAll("\\s+", " ")

      val provided = individualProvidedDetailsList.map(n => normalise(n.individualName.value)).toSet
      val officers = companiesHouseOfficersNames.map(n => normalise(n.name)).toSet

      provided =!= officers
