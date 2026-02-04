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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import sttp.model.Uri
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn.*
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.CreateSignInDetailsPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.SignInWithAgentDetailsPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.TypeOfSignInPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class TypeOfSignInController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: TypeOfSignInPage,
  signInWithAgentDetailsPage: SignInWithAgentDetailsPage,
  createSignInDetailsPage: CreateSignInDetailsPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = action:
    implicit request =>
      Ok(view(TypeOfSignInForm.form.fill(request.readTypeOfSignIn)))

  def submit: Action[AnyContent] =
    action
      .ensureValidForm4(TypeOfSignInForm.form, implicit request => view(_)):
        implicit request =>
          val typeOfSignIn: TypeOfSignIn = request.get
          Redirect(AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.showSignInPage)
            .addToSession(typeOfSignIn)

  def showSignInPage: Action[AnyContent] =
    action
      .refine4(r => r.readFromSessionAgentType.fold(Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url))(r.add))
      .refine4(r => r.readBusinessType.fold(Redirect(AppRoutes.apply.aboutyourbusiness.BusinessTypeSessionController.show.url))(r.add))
      .refine4(r => r.readTypeOfSignIn.fold(Redirect(AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url))(r.add))
      .refine4(r => r.readUserRole.fold(Redirect(AppRoutes.apply.aboutyourbusiness.UserRoleController.show.url))(r.add)):
        implicit request =>
          val agentType: AgentType = request.get
          val businessType: BusinessType = request.get
          val typeOfSignIn: TypeOfSignIn = request.get
          val userRole: UserRole = request.get

          val signInLink: Uri = AppRoutes
            .apply
            .internal
            .InitiateAgentApplicationController
            .initiateAgentApplication(
              agentType = agentType,
              businessType = businessType,
              userRole = userRole
            )
            .url
            .pipe(initiateUrl => uri"${appConfig.thisFrontendBaseUrl + initiateUrl}")

          typeOfSignIn match
            case HmrcOnlineServices => Ok(signInWithAgentDetailsPage(signInLink))
            case CreateSignInDetails => Ok(createSignInDetailsPage(signInLink))
