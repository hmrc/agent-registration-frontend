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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.companieshouse.*
import uk.gov.hmrc.agentregistration.shared.util.Errors.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionFormType
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms.toOfficerSelection
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.MatchedMemberPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.MatchedMembersPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class CompaniesHouseMatchingController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  agentRegistrationService: AgentRegistrationService,
  companiesHouseService: CompaniesHouseService,
  memberProvideDetailsService: MemberProvideDetailsService,
  matchedMemberView: MatchedMemberPage,
  matchedMembersView: MatchedMembersPage,
  noMemberNameMatchesView: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.companiesHouseMatch.isDefined,
      implicit request =>
        logger.info("Redirecting to member name page due to missing memberNameQuery value")
        Redirect(routes.CompaniesHouseNameQueryController.show)
    )

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request: MemberProvideDetailsRequest[AnyContent] =>
        fetchOfficers.map:
          case Nil =>
            logger.info("No Companies House officers found matching member name query, rendering noMemberNameMatchesView")
            Ok(noMemberNameMatchesView(
              h1 = "No member name matches",
              bodyText = Some("placeholder for no matches")
            ))

          case officer :: Nil =>
            logger.info(s"Found one Companies House officer matching member name query, rendering matchedMemberView")
            val form = ChOfficerSelectionForms.yesNoForm
              .fill(request.memberProvidedDetails
                .getCompaniesHouseMatch
                .companiesHouseOfficer
                .filter(_ === officer).map(_ => YesNo.Yes))
            Ok(matchedMemberView(form, officer))

          case officers: Seq[CompaniesHouseOfficer] =>
            logger.info(s"Found ${officers.size} Companies House officers matching member name query, rendering matchedMembersView")
            Ok(matchedMembersView(
              form = ChOfficerSelectionForms
                .officerSelectionForm(officers)
                .fill(request.memberProvidedDetails
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
      implicit request: (MemberProvideDetailsRequest[AnyContent] & FormValue[ChOfficerSelectionFormType]) =>
        request.formValue match {
          case ChOfficerSelectionFormType.YesNoForm => handleYesNoForm
          case ChOfficerSelectionFormType.OfficerSelectionForm => handleOfficerSelectionForm
        }

  def handleYesNoForm(using request: MemberProvideDetailsRequest[?]): Future[Result] = fetchOfficers.flatMap: officers =>
    val officer: CompaniesHouseOfficer = officers
      .headOption
      .getOrThrowExpectedDataMissing(
        s"Unexpected response from companies house, expected one officer but got: ${officers.size}"
      )
    ChOfficerSelectionForms.yesNoForm.bindFromRequest().fold(
      hasErrors = formWithErrors => Future.successful(BadRequest(matchedMemberView(formWithErrors, officer))),
      success =
        case YesNo.Yes => updateProvidedDetails(officer)
        case YesNo.No =>
          // TODO: do we need to reset data here?
          Future.successful(
            Redirect(routes.CompaniesHouseNameQueryController.show.url)
          )
    )

  def handleOfficerSelectionForm(using request: MemberProvideDetailsRequest[?]): Future[Result] = fetchOfficers.flatMap: officers =>
    Errors.require(officers.size > 1, s"Unexpected response from companies house, expected more then 1 officer but got: ${officers.size}")

    ChOfficerSelectionForms
      .officerSelectionForm(officers)
      .bindFromRequest()
      .fold(
        hasErrors = formWithErrors => Future.successful(BadRequest(matchedMembersView(formWithErrors, officers))),
        success =
          officerSelection =>
            val officer: CompaniesHouseOfficer = officers
              .find(_.toOfficerSelection === officerSelection)
              .getOrThrowExpectedDataMissing("Unexpected response from companies house, could not find selected officer")
            updateProvidedDetails(officer)
      )

  private def fetchOfficers(using request: MemberProvideDetailsRequest[?]): Future[Seq[CompaniesHouseOfficer]] =
    for {
      agentApplication <- agentRegistrationService
        .findApplication(request.memberProvidedDetails.agentApplicationId) // starting to think it may be better to have the application in the request
      officers <- companiesHouseService.getLlpOfficers(
        companyRegistrationNumber =
          agentApplication
            .getOrThrowExpectedDataMissing("Agent application not found")
            .asLlpApplication
            .getCrn,
        lastName =
          request.memberProvidedDetails.companiesHouseMatch
            .getOrThrowExpectedDataMissing("Companies House match is not defined")
            .memberNameQuery
            .lastName
      )
    } yield officers

  private def updateProvidedDetails(
    officer: CompaniesHouseOfficer
  )(using request: MemberProvideDetailsRequest[?]): Future[Result] = memberProvideDetailsService
    .upsert(
      memberProvidedDetails = request.memberProvidedDetails
        .modify(_.companiesHouseMatch.each.companiesHouseOfficer)
        .setTo(Some(officer))
    )
    .map(_ => Redirect(routes.TelephoneNumberController.show.url))
