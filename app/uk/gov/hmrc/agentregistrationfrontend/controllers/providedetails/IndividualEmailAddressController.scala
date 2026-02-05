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
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.action.individual.llp.IndividualProvideDetailsRequest

import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualEmailAddressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.IndividualEmailLockedPage
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistration.shared.llp.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class IndividualEmailAddressController @Inject() (
  appConfig: AppConfig,
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  individualEmailAddressView: IndividualEmailAddressPage,
  individualEmailLockedView: IndividualEmailLockedPage,
  individualProvideDetailsService: IndividualProvideDetailsService,
  emailVerificationService: EmailVerificationService,
  placeholder: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[IndividualProvideDetailsRequest, AnyContent] = actions.DELETEMEgetProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualTelephoneNumberController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(individualEmailAddressView(
        IndividualEmailAddressForm.form
          .fill:
            request
              .individualProvidedDetails
              .emailAddress.map(_.emailAddress)
      ))

  def submit: Action[AnyContent] = baseAction
    .ensureValidForm[EmailAddress](
      IndividualEmailAddressForm.form,
      implicit r => individualEmailAddressView(_)
    )
    .async:
      implicit request: (IndividualProvideDetailsRequest[AnyContent] & FormValue[EmailAddress]) =>
        val emailAddressFromForm: EmailAddress = request.formValue
        val updatedProvidedDetails: IndividualProvidedDetailsToBeDeleted = request
          .individualProvidedDetails
          .modify(_.emailAddress)
          .using {
            case Some(details) =>
              Some(IndividualVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                // avoid unsetting verified status of any unchanged email if we are not ignoring verification
                isVerified =
                  appConfig.ignoreEmailVerification ||
                    ((emailAddressFromForm === details.emailAddress) && details.isVerified)
              ))
            case None =>
              Some(IndividualVerifiedEmailAddress(
                emailAddress = emailAddressFromForm,
                isVerified = appConfig.ignoreEmailVerification
              ))
          }
        individualProvideDetailsService
          .upsert(updatedProvidedDetails)
          .map: _ =>
            Redirect(
              AppRoutes.providedetails.IndividualEmailAddressController.verify
            )

  def verify: Action[AnyContent] = actions.DELETEMEgetProvideDetailsInProgress
    .ensure(
      _.individualProvidedDetails.emailAddress.isDefined,
      implicit request =>
        // Individual email has not been provided, redirecting to email address page
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show)
    )
    .ensure(
      _.individualProvidedDetails
        .getEmailAddress
        .isVerified === false,
      implicit request =>
        // Individual email has already been provided and verified, redirecting to nino page
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show)
    )
    .async:
      implicit request =>
        val emailToVerify = request.individualProvidedDetails.getEmailAddress.emailAddress.value
        val credId = request.credentials.providerId
        emailVerificationService.checkEmailVerificationStatus(
          credId = credId,
          email = emailToVerify
        ).flatMap:
          case EmailVerificationStatus.Verified =>
            logger.info(s"[checkEmailVerificationStatus] Verified status received for individualEmail")
            onEmailVerified()
          case EmailVerificationStatus.Unverified =>
            logger.info(s"[checkEmailVerificationStatus] Unverified status received for individualEmail")
            onEmailUnverified(credId, emailToVerify)
          case EmailVerificationStatus.Locked =>
            logger.info(s"[checkEmailVerificationStatus] Locked status received for individualEmail")
            onEmailLocked()
          case EmailVerificationStatus.Error =>
            logger.info(s"[checkEmailVerificationStatus] Error received for individualEmail")
            onEmailError()

  private def onEmailVerified()(implicit request: IndividualProvideDetailsRequest[AnyContent]): Future[Result] =
    val updatedProvidedDetails = request
      .individualProvidedDetails
      .modify(
        _.emailAddress
          .each.isVerified
      )
      .setTo(true)
    individualProvideDetailsService.upsert(updatedProvidedDetails).map { _ =>
      logger.info("Individual email status reported as verified, redirecting to Nino page")
      Redirect(AppRoutes.providedetails.CheckYourAnswersController.show)
    }

  private def onEmailUnverified(
    credId: String,
    emailToVerify: String
  )(implicit request: IndividualProvideDetailsRequest[AnyContent]): Future[Result] = emailVerificationService.verifyEmail(
    credId = credId,
    maybeEmail = Some(
      Email(
        address = emailToVerify,
        enterUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.IndividualEmailAddressController.show.url
      )
    ),
    continueUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.IndividualEmailAddressController.verify.url,
    maybeBackUrl = None,
    accessibilityStatementUrl = appConfig.accessibilityStatementPath,
    lang = messagesApi.preferred(request).lang.code
  )

  private def onEmailLocked()(implicit request: IndividualProvideDetailsRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(individualEmailLockedView())
  )

  private def onEmailError()(implicit request: IndividualProvideDetailsRequest[AnyContent]): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address verification error", bodyText = Some("placeholder for error during email verification")))
  )
