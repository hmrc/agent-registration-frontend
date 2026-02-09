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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.agentdetails

import com.softwaremill.quicklens.*
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.agentdetails.AgentEmailAddressPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AgentEmailAddressController @Inject() (
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AgentEmailAddressPage,
  placeholder: SimplePage,
  agentApplicationService: AgentApplicationService,
  emailVerificationService: EmailVerificationService,
  businessPartnerRecordService: BusinessPartnerRecordService
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplicationInProgress
    .ensure(
      _
        .agentApplication
        .agentDetails
        .exists(_.telephoneNumber.isDefined),
      implicit request =>
        logger.warn("Because we don't have a telephone number selected we are redirecting to the telephone page")
        Redirect(AppRoutes.apply.agentdetails.AgentTelephoneNumberController.show)
    )

  def show: Action[AnyContent] = baseAction
    .getMaybeBusinessPartnerRecord
    .apply:
      implicit request =>
        val bprOpt: Option[BusinessPartnerRecordResponse] = request.get
        Ok(view(
          form = AgentEmailAddressForm.form.fill:
            request
              .agentApplication
              .agentDetails
              .flatMap(_.agentEmailAddress)
              .map(_.emailAddress)
          ,
          bprEmailAddress = bprOpt.flatMap(_.emailAddress),
          agentApplication = request.agentApplication
        ))

  def submit: Action[AnyContent] = baseAction
    .ensure(
      // because we cannot store any submitted email without checking it's verified status first
      // if user is saving and continuing then handle the submission normally else redirect to save for later
      SubmissionHelper.getSubmitAction(_) === SaveAndContinue,
      implicit request =>
        Redirect(AppRoutes.apply.SaveForLaterController.show)
    )
    .ensureValidForm[AgentEmailAddress](
      form = AgentEmailAddressForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            businessPartnerRecordService
              .getBusinessPartnerRecord(request.agentApplication.getUtr)
              .map: (bprOpt: Option[BusinessPartnerRecordResponse]) =>
                view(
                  form = formWithErrors,
                  bprEmailAddress = bprOpt.flatMap(_.emailAddress),
                  agentApplication = request.agentApplication
                )
    )
    .async:
      implicit request =>
        val emailAddressFromForm: AgentEmailAddress = request.get
        val updatedApplication: AgentApplication = request
          .agentApplication
          .modify(_.agentDetails.each.agentEmailAddress)
          .using {
            case Some(details) =>
              Some(AgentVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                // avoid unsetting verified status of any unchanged email if we are not ignoring verification
                // also if selecting a pre-verified email i.e. not "other" then set to true
                isVerified =
                  emailAddressFromForm.otherAgentEmailAddress.isEmpty ||
                    (emailAddressFromForm === details.emailAddress && details.isVerified)
              ))
            case None =>
              Some(AgentVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                isVerified = emailAddressFromForm.otherAgentEmailAddress.isEmpty
              ))
          }
        agentApplicationService
          .upsert(updatedApplication)
          .map: _ =>
            Redirect(
              AppRoutes.apply.agentdetails.AgentEmailAddressController.verify
            )

  def verify: Action[AnyContent] = actions
    .getApplicationInProgress
    .ensure(
      _.agentApplication
        .agentDetails
        .map(_.agentEmailAddress).isDefined,
      implicit request =>
        logger.info("Applicant email has not been provided, redirecting to email address page")
        Redirect(AppRoutes.apply.agentdetails.AgentEmailAddressController.show)
    )
    .ensure(
      _.agentApplication
        .getAgentDetails
        .getAgentEmailAddress
        .isVerified === false,
      implicit request =>
        logger.info("Agent email is already verified, redirecting to check your answers page")
        Redirect(AppRoutes.apply.agentdetails.CheckYourAnswersController.show)
    )
    .async:
      implicit request: (RequestWithData[DataWithApplication]) =>
        val emailToVerify =
          request
            .agentApplication
            .getAgentDetails
            .getAgentEmailAddress
            .getEmailAddress
        val credId = request.credentials.providerId
        emailVerificationService.checkEmailVerificationStatus(
          credId = credId,
          email = emailToVerify
        ).flatMap {
          case EmailVerificationStatus.Verified =>
            logger.info(s"[checkEmailVerificationStatus] Verified status received for applicantEmail using credId $credId and email $emailToVerify")
            onEmailVerified()
          case EmailVerificationStatus.Unverified =>
            logger.info(s"[checkEmailVerificationStatus] Unverified status received for applicantEmail using credId $credId and email $emailToVerify")
            onEmailUnverified(credId, emailToVerify)
          case EmailVerificationStatus.Locked =>
            logger.info(s"[checkEmailVerificationStatus] Locked status received for applicantEmail using credId $credId and email $emailToVerify")
            onEmailLocked()
          case EmailVerificationStatus.Error =>
            logger.info(s"[checkEmailVerificationStatus] Error received for applicantEmail using credId $credId and email $emailToVerify")
            onEmailError()
        }

  private def onEmailVerified()(implicit request: RequestWithData[DataWithApplication]): Future[Result] =
    val updatedApplication = request
      .agentApplication
      .modify(
        _.agentDetails
          .each.agentEmailAddress
          .each.isVerified
      )
      .setTo(true)
    agentApplicationService.upsert(updatedApplication).map { _ =>
      logger.info("Applicant email status reported as verified, redirecting to check your answers page")
      Redirect(AppRoutes.apply.agentdetails.CheckYourAnswersController.show)
    }

  private def onEmailUnverified(
    credId: String,
    emailToVerify: String
  )(implicit request: RequestHeader): Future[Result] = emailVerificationService.verifyEmail(
    credId = credId,
    maybeEmail = Some(
      Email(
        address = emailToVerify,
        enterUrl = appConfig.thisFrontendBaseUrl + AppRoutes.apply.agentdetails.AgentEmailAddressController.show.url
      )
    ),
    continueUrl = appConfig.thisFrontendBaseUrl + AppRoutes.apply.agentdetails.AgentEmailAddressController.verify.url,
    maybeBackUrl = None, // APB-10609 explicitly no back url for email verification as it will be used to hard code a back link which breaks the history chain
    accessibilityStatementUrl = appConfig.accessibilityStatementPath,
    lang = messagesApi.preferred(request).lang.code
  )

  private def onEmailLocked()(implicit request: RequestHeader): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address locked", bodyText = Some("placeholder for Your email address has been locked")))
  )

  private def onEmailError()(implicit request: RequestHeader): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address verification error", bodyText = Some("placeholder for error during email verification")))
  )
