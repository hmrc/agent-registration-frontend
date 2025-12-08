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

import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailVerification.*
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberEmailAddressPage
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistration.shared.llp.MemberVerifiedEmailAddress
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MemberEmailAddressController @Inject() (
  appConfig: AppConfig,
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberEmailAddressPage,
  memberProvideDetailsService: MemberProvideDetailsService,
  emailVerificationService: EmailVerificationService,
  placeholder: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberTelephoneNumberController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        MemberEmailAddressForm.form
          .fill:
            request
              .memberProvidedDetails
              .emailAddress.map(_.emailAddress)
      ))

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[EmailAddress](
      MemberEmailAddressForm.form,
      implicit r => view(_)
    )
    .async:
      implicit request: (MemberProvideDetailsRequest[AnyContent] & FormValue[EmailAddress]) =>
        val emailAddressFromForm: EmailAddress = request.formValue
        val updatedProvidedDetails: MemberProvidedDetails = request
          .memberProvidedDetails
          .modify(_.emailAddress)
          .using {
            case Some(details) =>
              Some(MemberVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                // avoid unsetting verified status of any unchanged email if we are not ignoring verification
                isVerified = appConfig.ignoreEmailVerification || (emailAddressFromForm === details.emailAddress && details.isVerified)
              ))
            case None =>
              Some(MemberVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                isVerified = appConfig.ignoreEmailVerification
              ))
          }
        memberProvideDetailsService
          .upsert(updatedProvidedDetails)
          .map: _ =>
            Redirect(
              AppRoutes.providedetails.MemberEmailAddressController.verify
            )

  def verify: Action[AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.emailAddress.isDefined,
      implicit request =>
        // Member email has not been provided, redirecting to email address page
        Redirect(AppRoutes.providedetails.MemberEmailAddressController.show)
    )
    .ensure(
      _.memberProvidedDetails
        .getEmailAddress
        .isVerified === false,
      implicit request =>
        // Member email has already been provided and verified, redirecting to nino page
        Redirect(AppRoutes.providedetails.MemberNinoController.show)
    )
    .async:
      implicit request =>
        val emailToVerify = request.memberProvidedDetails.getEmailAddress.emailAddress.value
        val credId = request.credentials.providerId
        emailVerificationService.checkEmailVerificationStatus(
          credId = credId,
          email = emailToVerify
        ).flatMap:
          case EmailVerificationStatus.Verified =>
            logger.info(s"[checkEmailVerificationStatus] Verified status received for memberEmail")
            onEmailVerified()
          case EmailVerificationStatus.Unverified =>
            logger.info(s"[checkEmailVerificationStatus] Unverified status received for memberEmail")
            onEmailUnverified(credId, emailToVerify)
          case EmailVerificationStatus.Locked =>
            logger.info(s"[checkEmailVerificationStatus] Locked status received for memberEmail")
            onEmailLocked()
          case EmailVerificationStatus.Error =>
            logger.info(s"[checkEmailVerificationStatus] Error received for memberEmail")
            onEmailError()

  private def onEmailVerified()(implicit request: MemberProvideDetailsRequest[AnyContent]): Future[Result] =
    val updatedProvidedDetails = request
      .memberProvidedDetails
      .modify(
        _.emailAddress
          .each.isVerified
      )
      .setTo(true)
    memberProvideDetailsService.upsert(updatedProvidedDetails).map { _ =>
      logger.info("Member email status reported as verified, redirecting to Nino page")
      Redirect(AppRoutes.providedetails.MemberNinoController.show)
    }

  private def onEmailUnverified(
    credId: String,
    emailToVerify: String
  )(implicit request: MemberProvideDetailsRequest[AnyContent]): Future[Result] = emailVerificationService.verifyEmail(
    credId = credId,
    maybeEmail = Some(
      Email(
        address = emailToVerify,
        enterUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.MemberEmailAddressController.show.url
      )
    ),
    continueUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.MemberEmailAddressController.verify.url,
    maybeBackUrl = Some(appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.MemberEmailAddressController.show.url),
    accessibilityStatementUrl = appConfig.accessibilityStatementPath,
    lang = messagesApi.preferred(request).lang.code
  ).map(redirectUrl =>
    Redirect(appConfig.emailVerificationFrontendBaseUrl + redirectUrl)
  )

  private def onEmailLocked()(implicit request: MemberProvideDetailsRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address locked", bodyText = Some("placeholder for Your email address has been locked")))
  )

  private def onEmailError()(implicit request: MemberProvideDetailsRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address verification error", bodyText = Some("placeholder for error during email verification")))
  )
