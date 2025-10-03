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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourbusiness

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.PartnershipTypeForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.PartnershipTypePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnershipTypeController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: PartnershipTypePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[Request, AnyContent] = action
    .ensure(
      _.readBusinessType match {
        case Some(BusinessTypeAnswer.PartnershipType) => true
        case _ => false
      },
      implicit request =>
        logger.info(s"Redirecting to business type page due to missing or invalid business type selection: ${request.readBusinessType}")
        Redirect(routes.BusinessTypeSessionController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      val form: Form[BusinessType.Partnership] =
        request.readPartnershipType match
          case Some(data) => PartnershipTypeForm.form.fill(data)
          case None => PartnershipTypeForm.form
      Ok(view(form))

  def submit: Action[AnyContent] =
    baseAction.ensureValidForm(PartnershipTypeForm.form, implicit r => view(_)):
      implicit request =>
        val partnershipType: BusinessType.Partnership = request.formValue
        Redirect(
          routes.TypeOfSignInController.show
        ).addPartnershipTypeToSession(partnershipType)
