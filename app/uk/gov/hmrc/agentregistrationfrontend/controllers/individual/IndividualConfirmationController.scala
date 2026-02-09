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

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class IndividualConfirmationController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  individualConfirmationPage: IndividualConfirmationPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithAgentApplication] = actions.getSubmittedDetailsWithApplicationInProgress
    .ensure(
      _.individualProvidedDetails.hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed,
      implicit request =>
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      val applicantName = request.agentApplication.getApplicantContactDetails.applicantName
      val companyName = request.agentApplication.dontCallMe_getCompanyProfile.companyName
      Future successful Ok(individualConfirmationPage(
        applicantName = applicantName.value,
        companyName = companyName
      ))
