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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplicationInProgress
    .ensure(
      _.agentApplication.applicantContactDetails.exists(_.isComplete),
      implicit request =>
        logger.warn("Because we don't have complete applicant contact details we are redirecting to where data is missing")
        request.agentApplication.applicantContactDetails match {
          case None => Redirect(AppRoutes.apply.applicantcontactdetails.ApplicantNameController.show)
          case Some(ApplicantContactDetails(_, None, _)) => Redirect(AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.show)
          case Some(ApplicantContactDetails(_, Some(_), _)) => Redirect(AppRoutes.apply.applicantcontactdetails.EmailAddressController.show)
        }
    )

  def show: Action[AnyContent] = baseAction:
    implicit request => Ok(view(request.agentApplication))
