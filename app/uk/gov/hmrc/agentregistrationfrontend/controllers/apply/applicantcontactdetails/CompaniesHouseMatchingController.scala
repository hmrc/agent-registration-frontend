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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionFormType
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms.toOfficerSelection
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MatchedMemberPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MatchedMembersPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import CompaniesHouseMatchingController.*
import play.api.data.Form
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

@Singleton
class CompaniesHouseMatchingController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  agentApplicationService: AgentApplicationService,
  companiesHouseService: CompaniesHouseService,
  matchedMemberView: MatchedMemberPage,
  matchedMembersView: MatchedMembersPage,
  noMemberNameMatchesView: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.asLlpApplication.memberNameQuery.isDefined,
      implicit request =>
        logger.info("Redirecting to member name page due to missing memberNameQuery value")
        Redirect(AppRoutes.apply.applicantcontactdetails.MemberNameController.show)
    )

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        // TODO: case when user clicks back and lands on this page.
        // Instead of calling CH we just render the page with prepopulated data.
        // On submission decide whether to call CH again

        companiesHouseService.getLlpOfficers(
          companyRegistrationNumber = request.agentApplication.asLlpApplication.getCrn,
          lastName = request.agentApplication.asLlpApplication.getLastNameFromQuery // safe due to ensuring memberNameQuery is defined first
        ).map:
          case Nil =>
            logger.info("No Companies House officers found matching member name query, rendering noMemberNameMatchesView")
            Ok(noMemberNameMatchesView(
              h1 = "No member name matches",
              bodyText = Some("placeholder for no matches")
            ))

          case officer :: Nil =>
            logger.info(s"Found one Companies House officer matching member name query, rendering matchedMemberView")
            val form: Form[YesNo] = ChOfficerSelectionForms.yesNoForm.fill(request.agentApplication.asLlpApplication.companyOfficer.filter(_ === officer).map(
              _ => YesNo.Yes
            ))
            Ok(matchedMemberView(form, officer))

          case officers: Seq[CompaniesHouseOfficer] =>
            logger.info(s"Found ${officers.size} Companies House officers matching member name query, rendering matchedMembersView")
            Ok(matchedMembersView(
              form = ChOfficerSelectionForms
                .officerSelectionForm(officers)
                .fill(request.agentApplication.asLlpApplication.companyOfficer.map(_.toOfficerSelection)),
              officers = officers
            ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[ChOfficerSelectionFormType](
        _ => ChOfficerSelectionForms.formType,
        implicit request => formWithErrors => Errors.throwBadRequestException(s"Unexpected errors in the FormType: $formWithErrors")
      )
      .async:
        implicit request =>
          request.formValue match {
            case ChOfficerSelectionFormType.YesNoForm => handleYesNoForm
            case ChOfficerSelectionFormType.OfficerSelectionForm => handleOfficerSelectionForm
          }
      .redirectIfSaveForLater

  private def handleYesNoForm(using request: AgentApplicationRequest[?]): Future[Result] = companiesHouseService.getLlpOfficers(
    companyRegistrationNumber = request.agentApplication.asLlpApplication.getCrn,
    lastName = request.agentApplication.asLlpApplication.getLastNameFromQuery
  )
    .flatMap: officers =>
      val officer: CompaniesHouseOfficer = officers
        .headOption
        .getOrThrowExpectedDataMissing(
          s"Unexpected response from companies house, expected one officer but got: ${officers.size}"
        )
      ChOfficerSelectionForms.yesNoForm.bindFromRequest().fold(
        hasErrors = formWithErrors => Future.successful(BadRequest(matchedMemberView(formWithErrors, officer))),
        success =
          case YesNo.Yes => updateApplicationAndRedirectToCya(officer)
          case YesNo.No =>
            // TODO: do we need to reset data here?
            Future.successful(
              Redirect(AppRoutes.apply.applicantcontactdetails.ApplicantRoleInLlpController.show.url)
            )
      )

  def handleOfficerSelectionForm(using request: AgentApplicationRequest[?]): Future[Result] = companiesHouseService.getLlpOfficers(
    companyRegistrationNumber = request.agentApplication.asLlpApplication.getCrn,
    lastName = request.agentApplication.asLlpApplication.getLastNameFromQuery
  )
    .flatMap: officers =>
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
              updateApplicationAndRedirectToCya(officer)
        )

  private def updateApplicationAndRedirectToCya(officer: CompaniesHouseOfficer)(using request: AgentApplicationRequest[?]) =
    val updatedApplication: AgentApplication = request.agentApplication.asLlpApplication
      .modify(_.applicantContactDetails.each.applicantName)
      .setTo(ApplicantName.NameOfMember(
        memberNameQuery = request.agentApplication.asLlpApplication.memberNameQuery,
        companiesHouseOfficer = Some(officer)
      ))
    agentApplicationService
      .upsert(updatedApplication)
      .map(_ => Redirect(AppRoutes.apply.applicantcontactdetails.CheckYourAnswersController.show.url))

object CompaniesHouseMatchingController:

  extension (agentApplication: AgentApplicationLlp)

    def getLastNameFromQuery: String =
      agentApplication.memberNameQuery.getOrThrowExpectedDataMissing("memberNameQuery is not defined")
        .lastName

    def companyOfficer: Option[CompaniesHouseOfficer] =
      for
        acd <- agentApplication.applicantContactDetails
        nameOfMember <- acd.applicantName.as[ApplicantName.NameOfMember]
        companyOfficer <- nameOfMember.companiesHouseOfficer
      yield companyOfficer

    def memberNameQuery: Option[CompaniesHouseNameQuery] =
      for
        acd <- agentApplication.applicantContactDetails
        nameOfMember <- acd.applicantName.as[ApplicantName.NameOfMember]
        query <- nameOfMember.memberNameQuery
      yield query
