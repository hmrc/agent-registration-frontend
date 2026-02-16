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

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualEmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.emailverification.*
import uk.gov.hmrc.agentregistrationfrontend.services.EmailVerificationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualEmailAddressPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualEmailLockedPage
import uk.gov.hmrc.auth.core.retrieve.Credentials

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
  agentApplicationService: AgentApplicationService,
  placeholder: SimplePage
)
extends FrontendController(mcc, actions):

  private type DataWithApplicationFromLinkId = AgentApplication *: DataWithAuth

  private type DataWithIndividualProvidedDetails = IndividualProvidedDetails *: DataWithApplicationFromLinkId

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = actions
    .authorised
    .refine(implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) => request.add(agentApplication)
          case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    )
    .refine(implicit request =>
      individualProvideDetailsService
        .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
        .map[RequestWithData[DataWithIndividualProvidedDetails] | Result]:
          case list: List[IndividualProvidedDetails] =>
            list
              .find(_.internalUserId.contains(request.get[InternalUserId]))
              .map(request.add[IndividualProvidedDetails])
              .getOrElse(
                Redirect(AppRoutes.providedetails.ConfirmMatchToIndividualProvidedDetailsController.show(linkId))
              )
    )
    .ensure(
      _.get[IndividualProvidedDetails].telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.providedetails.IndividualTelephoneNumberController.show(linkId))
    )

  def show(linkId: LinkId): Action[AnyContent] =
    baseAction(linkId):
      implicit request =>
        Ok(individualEmailAddressView(
          form = IndividualEmailAddressForm.form
            .fill:
              request
                .get[IndividualProvidedDetails]
                .emailAddress.map(_.emailAddress)
          ,
          linkId = linkId
        ))

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .ensureValidForm[EmailAddress](
      IndividualEmailAddressForm.form,
      implicit r => individualEmailAddressView(_, linkId)
    )
    .async:
      implicit request =>
        val emailAddressFromForm: EmailAddress = request.get
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val updatedProvidedDetails: IndividualProvidedDetails = individualProvidedDetails
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
              AppRoutes.providedetails.IndividualEmailAddressController.verify(linkId)
            )

  def verify(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].emailAddress.isDefined,
      implicit request =>
        // Individual email has not been provided, redirecting to email address page
        Redirect(AppRoutes.providedetails.IndividualEmailAddressController.show(linkId))
    )
    .ensure(
      _.get[IndividualProvidedDetails]
        .getEmailAddress
        .isVerified === false,
      implicit request =>
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId))
    )
    .refine:
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val credentials: Credentials = request.get
        emailVerificationService
          .checkEmailVerificationStatus(
            credId = credentials.providerId,
            email = individualProvidedDetails.getEmailAddress.emailAddress.value
          )
          .map(request.add[EmailVerificationStatus])
    .async:
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val credentials: Credentials = request.get
        request.get[EmailVerificationStatus] match
          case EmailVerificationStatus.Verified =>
            logger.info(s"[checkEmailVerificationStatus] Verified status received for individualEmail")
            onEmailVerified(individualProvidedDetails, linkId)
          case EmailVerificationStatus.Unverified =>
            logger.info(s"[checkEmailVerificationStatus] Unverified status received for individualEmail")
            onEmailUnverified(
              credId = credentials.providerId,
              emailToVerify = individualProvidedDetails.getEmailAddress.emailAddress.value,
              linkId = linkId
            )
          case EmailVerificationStatus.Locked =>
            logger.info(s"[checkEmailVerificationStatus] Locked status received for individualEmail")
            onEmailLocked(linkId)
          case EmailVerificationStatus.Error =>
            logger.info(s"[checkEmailVerificationStatus] Error received for individualEmail")
            onEmailError()

  private def onEmailVerified(
    individualProvidedDetails: IndividualProvidedDetails,
    linkId: LinkId
  )(implicit request: RequestHeader): Future[Result] =
    val updatedProvidedDetails = individualProvidedDetails
      .modify(
        _.emailAddress
          .each.isVerified
      )
      .setTo(true)
    individualProvideDetailsService.upsert(updatedProvidedDetails).map { _ =>
      logger.info("Individual email status reported as verified, redirecting to Nino page")
      Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId))
    }

  private def onEmailUnverified(
    credId: String,
    emailToVerify: String,
    linkId: LinkId
  )(implicit request: RequestHeader): Future[Result] = emailVerificationService.verifyEmail(
    credId = credId,
    maybeEmail = Some(
      Email(
        address = emailToVerify,
        enterUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.IndividualEmailAddressController.show(linkId).url
      )
    ),
    continueUrl = appConfig.thisFrontendBaseUrl + AppRoutes.providedetails.IndividualEmailAddressController.verify(linkId).url,
    maybeBackUrl = None,
    accessibilityStatementUrl = appConfig.accessibilityStatementPath,
    lang = messagesApi.preferred(request).lang.code
  )

  private def onEmailLocked(linkId: LinkId)(implicit request: RequestHeader): Future[Result] = Future.successful(
    Ok(individualEmailLockedView(linkId))
  )

  private def onEmailError()(implicit request: RequestHeader): Future[Result] = Future.successful(
    Ok(placeholder(h1 = "Email address verification error", bodyText = Some("placeholder for error during email verification")))
  )
