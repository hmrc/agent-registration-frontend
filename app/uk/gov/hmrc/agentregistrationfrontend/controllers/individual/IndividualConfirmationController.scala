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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.StateOfAgreement
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.IndividualConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndividualConfirmationController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  individualConfirmationPage: IndividualConfirmationPage,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetails] = authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      _.get[IndividualProvidedDetails].hmrcStandardForAgentsAgreed === StateOfAgreement.Agreed,
      implicit request =>
        Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url)
    )

  def show(linkId: LinkId): Action[AnyContent] = baseAction(linkId).async:
    implicit request =>
      val agentApplication: AgentApplication = request.get
      val applicantName = agentApplication.getApplicantContactDetails.applicantName // TODO: do we really need this? It means task list must ensure contact details complete before we can unlock this task
      businessPartnerRecordService
        .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
        .map: optBpr =>
          Ok(individualConfirmationPage(
            applicantName = applicantName.value,
            entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR is missing")
          ))
