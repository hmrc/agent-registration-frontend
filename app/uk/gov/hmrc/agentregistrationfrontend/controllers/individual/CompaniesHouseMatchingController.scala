/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.companieshouse.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionFormType
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms.toOfficerSelection
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.MatchedIndividualPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.MatchedIndividualsPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class CompaniesHouseMatchingController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  agentApplicationService: AgentApplicationService,
  companiesHouseService: CompaniesHouseService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  matchedIndividualView: MatchedIndividualPage,
  matchedIndividualsView: MatchedIndividualsPage,
  noIndividualNameMatchesView: SimplePage
)
extends FrontendController(mcc, actions):

  private type Officers = Seq[CompaniesHouseOfficer]
  private type Data =
    ChOfficerSelectionFormType
      *: Officers
      *: IndividualProvidedDetailsToBeDeleted
      *: DataWithAuth

  private val baseAction: ActionBuilderWithData[
    Officers
      *: IndividualProvidedDetailsToBeDeleted
      *: DataWithAuth
  ] = actions
    .getProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.companiesHouseMatch.isDefined,
      implicit request =>
        logger.info("Redirecting to individual name page due to missing memberNameQuery value")
        Redirect(AppRoutes.providedetails.CompaniesHouseNameQueryController.show)
    )
    .refine:
      implicit request =>
        fetchOfficers(request.individualProvidedDetails).map(request.add)

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      request.get[Officers] match
        case Nil =>
          logger.info("No Companies House officers found matching individual name query, rendering noMemberNameMatchesView")
          Ok(noIndividualNameMatchesView(
            h1 = "No member name matches",
            bodyText = Some("placeholder for no matches")
          ))

        case officer :: Nil =>
          logger.info(s"Found one Companies House officer matching individual name query, rendering matchedMemberView")
          val form = ChOfficerSelectionForms.yesNoForm
            .fill(request.individualProvidedDetails
              .getCompaniesHouseMatch
              .companiesHouseOfficer
              .filter(_ === officer).map(_ => YesNo.Yes))
          Ok(matchedIndividualView(form, officer))

        case officers: Seq[CompaniesHouseOfficer] =>
          logger.info(s"Found ${officers.size} Companies House officers matching individual name query, rendering matchedMembersView")
          Ok(matchedIndividualsView(
            form = ChOfficerSelectionForms
              .officerSelectionForm(officers)
              .fill(request.individualProvidedDetails
                .getCompaniesHouseMatch
                .companiesHouseOfficer
                .map(_.toOfficerSelection)),
            officers = officers
          ))

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[ChOfficerSelectionFormType](
      ChOfficerSelectionForms.formType,
      implicit request => formWithErrors => Errors.throwBadRequestException(s"Unexpected errors in the FormType: $formWithErrors")
    )
    .async:
      implicit request: (RequestWithData[Data]) =>
        request.get[ChOfficerSelectionFormType] match
          case ChOfficerSelectionFormType.YesNoForm => handleYesNoForm()
          case ChOfficerSelectionFormType.OfficerSelectionForm => handleOfficerSelectionForm()

  private def handleYesNoForm()(using request: RequestWithData[Data]): Future[Result] =
    val officers: Officers = request.get[Officers]
    val officer: CompaniesHouseOfficer = officers
      .headOption
      .getOrThrowExpectedDataMissing(
        s"Unexpected response from companies house, expected one officer but got: ${officers.size}"
      )
    ChOfficerSelectionForms.yesNoForm.bindFromRequest().fold(
      hasErrors = (formWithErrors: Form[YesNo]) => Future.successful(BadRequest(matchedIndividualView(formWithErrors, officer))),
      success =
        case YesNo.Yes => updateProvidedDetails(officer)
        case YesNo.No =>
          // TODO: do we need to reset data here?
          Future.successful(
            Redirect(AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url)
          )
    )

  private def handleOfficerSelectionForm()(using request: RequestWithData[Data]): Future[Result] =
    val officers = request.get[Officers]
    Errors.require(officers.size > 1, s"Unexpected response from companies house, expected more then 1 officer but got: ${officers.size}")

    ChOfficerSelectionForms
      .officerSelectionForm(officers)
      .bindFromRequest()
      .fold(
        hasErrors = formWithErrors => Future.successful(BadRequest(matchedIndividualsView(formWithErrors, officers))),
        success =
          officerSelection =>
            val officer: CompaniesHouseOfficer = officers
              .find(_.toOfficerSelection === officerSelection)
              .getOrThrowExpectedDataMissing("Unexpected response from companies house, could not find selected officer")
            updateProvidedDetails(officer)
      )

  private def fetchOfficers(individualProvidedDetails: IndividualProvidedDetailsToBeDeleted)(using request: RequestHeader): Future[Officers] =
    for
      agentApplication <- agentApplicationService
        .find(individualProvidedDetails.agentApplicationId) // starting to think it may be better to have the application in the request
      officers <- companiesHouseService.getLlpOfficers(
        companyRegistrationNumber =
          agentApplication
            .getOrThrowExpectedDataMissing("Agent application not found")
            .asLlpApplication
            .getCrn,
        lastName =
          individualProvidedDetails.companiesHouseMatch
            .getOrThrowExpectedDataMissing("Companies House match is not defined")
            .memberNameQuery
            .lastName
      )
    yield officers

  private def updateProvidedDetails(officer: CompaniesHouseOfficer)(using request: RequestWithData[Data]): Future[Result] = individualProvideDetailsService
    .upsert(
      individualProvidedDetails = request
        .individualProvidedDetails
        .modify(_.companiesHouseMatch.each.companiesHouseOfficer)
        .setTo(Some(officer))
    )
    .map(_ => Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url))
