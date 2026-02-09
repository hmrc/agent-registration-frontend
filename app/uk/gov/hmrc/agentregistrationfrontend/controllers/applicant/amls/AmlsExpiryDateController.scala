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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls

import com.softwaremill.quicklens.*
import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsExpiryDateForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.amls.AmlsExpiryDatePage

import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmlsExpiryDateController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: AmlsExpiryDatePage,
  applicationService: AgentApplicationService
)(using clock: Clock)
extends FrontendController(mcc, actions):

  val baseAction: ActionBuilderWithData[DataWithApplication] = actions
    .getApplicationInProgress
    .ensure(
      _.agentApplication.amlsDetails.exists(_.amlsRegistrationNumber.isDefined),
      implicit r =>
        logger.warn("Missing AmlsRegistrationNumber, redirecting to registration number page")
        Redirect(AppRoutes.apply.amls.AmlsRegistrationNumberController.show.url)
    )
    .ensure(
      !_.agentApplication.getAmlsDetails.isHmrc, // safe to getAmlsDetails as ensured above
      implicit r =>
        logger.warn("Expiry date is not required as supervisor is HMRC, redirecting to Check Your Answers")
        Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val form: Form[LocalDate] = AmlsExpiryDateForm.form().fill(request
        .agentApplication
        .getAmlsDetails
        .amlsExpiryDate)
      Ok(view(form))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[LocalDate](AmlsExpiryDateForm.form(), implicit request => view(_))
      .async:
        implicit request =>
          val amlsExpiryDate: LocalDate = request.get
          applicationService
            .upsert(
              request.agentApplication
                .modify(_.amlsDetails.each.amlsExpiryDate)
                .setTo(Some(amlsExpiryDate))
            )
            .map(_ => Redirect(AppRoutes.apply.amls.CheckYourAnswersController.show.url))
      .redirectIfSaveForLater
