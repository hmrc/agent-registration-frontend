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
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.EmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.Email
import uk.gov.hmrc.agentregistrationfrontend.model.EmailVerificationStatus
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.EmailAddressPage

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
  emailVerificationService: EmailVerificationService
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
          .emailAddress
          .map(_.emailToVerify)
      )))

  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request =>
          EmailAddressForm.form
            .bindFromRequest()
            .fold(
              formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
              emailAddress => {
                val updatedApplication = request.agentApplication
                  .modify(_.applicantContactDetails.each.emailAddress)
                  .using {
                    case Some(details) =>
                      Some(
                        details
                          .modify(_.emailToVerify)
                          .setTo(emailAddress)
                      )
                    case None =>
                      Some(ApplicantEmailAddress(
                        emailToVerify = emailAddress,
                        verifiedEmail = None
                      ))
                  }

                applicationService
                  .upsert(updatedApplication)
                  .map(_ =>
                    Redirect(
                      routes.EmailAddressController.verify
                    )
                  )
              }
            )
      .redirectIfSaveForLater

  def verify: Action[AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication
        .applicantContactDetails
        .map(_.emailAddress).isDefined,
      implicit request =>
        logger.warn("Applicant email has not been provided, redirecting to email address page")
        Redirect(routes.EmailAddressController.show)
    )
    .async:
      implicit request =>
        val emailToVerify = request.agentApplication.getApplicantEmailToVerify.value
        val credId = request.credentials.providerId
        request.agentApplication
          .getApplicantContactDetails
          .emailAddress
          .filter(_.isVerified) match
          case Some(_) => Future.successful(Redirect(routes.CheckYourAnswersController.show))
          case None =>
            emailVerificationService.checkStatus(
              credId = credId,
              email = emailToVerify
            ).flatMap {
              case EmailVerificationStatus.Verified =>
                // The email has just been verified. Update the application and redirect to CYA
                val updatedApplication = request.agentApplication
                  .modify(_.applicantContactDetails
                    .each.emailAddress
                    .each.verifiedEmail)
                  .setTo(Some(EmailAddress(emailToVerify)))
                applicationService.upsert(updatedApplication).map { _ =>
                  Redirect(routes.CheckYourAnswersController.show)
                }
              // The email is not yet verified. Start the verification journey
              case EmailVerificationStatus.Unverified =>
                emailVerificationService.verifyEmail(
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
                ).map {
                  case Some(redirectUrl) => Redirect(appConfig.emailVerificationFrontendBaseUrl + redirectUrl)
                  case None => {
                    logger.error(s"Could not start email verification journey for credId ${request.internalUserId.value}")
                    InternalServerError("Could not start email verification journey")
                  }
                }
              case _ =>
                // The email was either locked or there was an error. Redirect to the email address page to re-enter the email
                Future.successful(Redirect(routes.EmailAddressController.show))
            }
