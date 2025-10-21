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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails

import com.softwaremill.quicklens.each
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.EmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailVerification.*
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.EmailAddressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class EmailAddressController @Inject() (
  appConfig: AppConfig,
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: EmailAddressPage,
  applicationService: ApplicationService,
  emailVerificationService: EmailVerificationService,
  placeholder: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication.applicantContactDetails.map(_.telephoneNumber).isDefined,
      implicit request =>
        logger.warn("Because we don't have a telephone number we are redirecting to the telephone number page")
        Redirect(routes.TelephoneNumberController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(EmailAddressForm.form.fill(
        request.agentApplication
          .getApplicantContactDetails
          .applicantEmailAddress
          .map(_.emailAddress)
      )))

  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request =>
          EmailAddressForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
              emailAddress =>
                val updatedApplication = request.agentApplication
                  .modify(_.applicantContactDetails.each.applicantEmailAddress)
                  .using {
                    case Some(details) =>
                      Some(ApplicantEmailAddress(
                        emailAddress = emailAddress,
                        isVerified = emailAddress === details.emailAddress && details.isVerified
                      ))
                    case None =>
                      Some(ApplicantEmailAddress(
                        emailAddress = emailAddress,
                        isVerified = false
                      ))
                  }

                println(s"Updated application email address, application now reads: $updatedApplication")

                applicationService
                  .upsert(updatedApplication)
                  .map(_ =>
                    Redirect(
                      routes.EmailAddressController.verify
                    )
                  )
            )
      .redirectIfSaveForLater

  def verify: Action[AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication
        .applicantContactDetails
        .map(_.applicantEmailAddress).isDefined,
      implicit request =>
        logger.info("Applicant email has not been provided, redirecting to email address page")
        Redirect(routes.EmailAddressController.show)
    )
    .ensure(
      _.agentApplication
        .getApplicantEmailAddress
        .isVerified === false,
      implicit request =>
        logger.info("Applicant email is already verified, redirecting to check your answers page")
        Redirect(routes.CheckYourAnswersController.show)
    )
    .async:
      implicit request =>
        val emailToVerify = request.agentApplication.getApplicantEmailAddress.emailAddress.value
        val credId = request.credentials.providerId
        emailVerificationService.checkStatus(
          credId = credId,
          email = emailToVerify
        ).flatMap {
          case EmailVerificationStatus.Verified => onEmailVerified()
          case EmailVerificationStatus.Unverified => onEmailUnverified(credId, emailToVerify)
          case EmailVerificationStatus.Locked => onEmailLocked()
          case EmailVerificationStatus.Error => onEmailError()
        }

  private def onEmailVerified()(implicit request: AgentApplicationRequest[AnyContent]): Future[Result] =
    val updatedApplication = request.agentApplication
      .modify(
        _.applicantContactDetails
          .each.applicantEmailAddress
          .each.isVerified
      )
      .setTo(true)
    applicationService.upsert(updatedApplication).map { _ =>
      logger.info("Applicant email status reported as verified, redirecting to check your answers page")
      Redirect(routes.CheckYourAnswersController.show)
    }

  private def onEmailUnverified(
    credId: String,
    emailToVerify: String
  )(implicit request: AgentApplicationRequest[AnyContent]): Future[Result] = emailVerificationService.verifyEmail(
    credId = credId,
    maybeEmail = Some(
      Email(
        address = emailToVerify,
        enterUrl = appConfig.thisFrontendBaseUrl + routes.EmailAddressController.show.url
      )
    ),
    continueUrl = appConfig.thisFrontendBaseUrl + routes.EmailAddressController.verify.url,
    maybeBackUrl = Some(appConfig.thisFrontendBaseUrl + routes.EmailAddressController.show.url),
    accessibilityStatementUrl = appConfig.accessibilityStatementPath,
    lang = messagesApi.preferred(request).lang.code
  ).map(redirectUrl =>
    Redirect(appConfig.emailVerificationFrontendBaseUrl + redirectUrl)
  )

  private def onEmailLocked()(implicit request: AgentApplicationRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address locked", bodyText = Some("placeholder for Your email address has been locked")))
  )

  private def onEmailError()(implicit request: AgentApplicationRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address verification error", bodyText = Some("placeholder for error during email verification")))
  )
