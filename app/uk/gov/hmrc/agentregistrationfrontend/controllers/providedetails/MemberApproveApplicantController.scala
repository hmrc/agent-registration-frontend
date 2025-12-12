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
import com.softwaremill.quicklens.modify
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.MemberApproveApplicationForm
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberApproveApplicationPage
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toYesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo.toBoolean

import scala.concurrent.Future
import javax.inject.Inject

class MemberApproveApplicantController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: MemberApproveApplicationPage,
  memberProvideDetailsService: MemberProvideDetailsService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsRequest, AnyContent] = actions.Member.getProvideDetailsInProgress
    .ensure(
      _.memberProvidedDetails.memberSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberSaUtrController.show.url)
    )
    .ensure(
      _.memberProvidedDetails.emailAddress.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.providedetails.MemberEmailAddressController.show.url)
    )

  private def withAgentApplication[A](f: (
    String,
    String
  ) => Future[Result])(using request: MemberProvideDetailsRequest[A]): Future[Result] = agentApplicationService
    .find(request.memberProvidedDetails.agentApplicationId)
    .flatMap:
      case Some(agentApplication) =>
        val applicantName = agentApplication.asLlpApplication.getApplicantContactDetails.getApplicantName
        val companyName = agentApplication.asLlpApplication.getBusinessDetails.companyProfile.companyName

        f(applicantName, companyName)

      case None =>
        logger.info(s"Application for agent applicationId ${request.memberProvidedDetails.agentApplicationId} not found")
        Future.successful(Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url))

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      withAgentApplication:
        (
          applicantName,
          companyName
        ) =>
          val filledForm = MemberApproveApplicationForm
            .form(applicantName)
            .fill:
              request
                .memberProvidedDetails
                .hasApprovedApplication
                .map(_.toYesNo)

          Future.successful(
            Ok(
              view(
                filledForm,
                applicantName,
                companyName
              )
            )
          )

  private def handleSubmitWithAgentApplication(
    applicantName: String,
    companyName: String
  )(using
    baseRequest: MemberProvideDetailsRequest[AnyContent]
  ): Future[Result] = baseAction
    .ensureValidFormAndRedirectIfSaveForLater[YesNo](
      MemberApproveApplicationForm.form(applicantName),
      implicit r =>
        view(
          _,
          applicantName,
          companyName
        )
    )
    .async:
      implicit request: MemberProvideDetailsRequest[AnyContent] & FormValue[YesNo] =>
        val approved: Boolean = request.formValue.toBoolean

        val updatedApplication: MemberProvidedDetails = request.memberProvidedDetails
          .modify(_.hasApprovedApplication)
          .setTo(Some(approved))

        memberProvideDetailsService
          .upsert(updatedApplication)
          .map: _ =>
            if approved then
              Redirect(AppRoutes.providedetails.MemberAgreeStandardController.show.url)
            else
              Redirect(AppRoutes.providedetails.MemberConfirmStopController.show.url)
    .apply(baseRequest)

  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request =>
          withAgentApplication:
            (
              applicantName,
              companyName
            ) =>
              handleSubmitWithAgentApplication(applicantName, companyName)
      .redirectIfSaveForLater
