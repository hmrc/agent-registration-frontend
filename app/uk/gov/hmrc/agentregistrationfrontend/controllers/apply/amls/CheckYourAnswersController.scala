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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsDetails
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: CheckYourAnswersPage
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] =
    actions
      .Applicant
      .getApplicationInProgress
      .ensure(
        r => r.agentApplication.amlsDetails.exists(_.isComplete),
        implicit request =>
          logger.warn(s"Cannot display Check Your Answers page - incomplete AMLS details.")
          request.agentApplication.amlsDetails match {
            case Some(AmlsDetails(_, None, _, _)) => Redirect(AppRoutes.apply.amls.AmlsRegistrationNumberController.show)
            case Some(AmlsDetails(
                  AmlsCode(amlsCode),
                  Some(_),
                  None,
                  _
                )) if !amlsCode.contains("HMRC") =>
              Redirect(AppRoutes.apply.amls.AmlsExpiryDateController.show)
            case Some(AmlsDetails(
                  AmlsCode(amlsCode),
                  Some(_),
                  Some(_),
                  _
                )) if !amlsCode.contains("HMRC") =>
              Redirect(AppRoutes.apply.amls.AmlsEvidenceUploadController.showAmlsEvidenceUploadPage)
            case _ => Redirect(AppRoutes.apply.amls.AmlsSupervisorController.show)
          }
      ):
        implicit request => Ok(view(request.agentApplication))
