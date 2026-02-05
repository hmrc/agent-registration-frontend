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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  // this CYA page is only viewable once business details have been captured from GRS, the task list provides a link
  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplicationInProgress
    .ensure4(
      _.get[AgentApplication].applicationState === ApplicationState.GrsDataReceived,
      implicit request =>
        logger.warn("Because we don't have business details we are redirecting to where they can be captured")
        Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show)
    )

  def show: Action[AnyContent] = baseAction
    .refine4:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map((bprOpt: Option[BusinessPartnerRecordResponse]) =>
            request.add(bprOpt.getOrThrowExpectedDataMissing(
              s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"
            ))
          )
    .apply:
      implicit request =>
        Ok(
          view(
            request.businessPartnerRecordResponse,
            request.agentApplication
          )
        )
