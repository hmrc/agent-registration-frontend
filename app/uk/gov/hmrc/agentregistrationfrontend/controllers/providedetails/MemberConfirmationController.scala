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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsWithApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.memberconfirmation.MemberConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class MemberConfirmationController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  memberConfirmationPage: MemberConfirmationPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[MemberProvideDetailsWithApplicationRequest, AnyContent] = actions.Member.getSubmitedDetailsWithApplicationInProgress
    .ensure(
      _.memberProvidedDetails.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed,
      implicit request =>
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      val applicantName = request.agentApplication.getApplicantContactDetails.applicantName

      // TODO: there are application types which have no company profile and this will throw exception for those.
      val companyName = request.agentApplication.getCompanyProfile.companyName
      Future successful Ok(memberConfirmationPage(
        applicantName = applicantName.value,
        companyName = companyName
      ))
