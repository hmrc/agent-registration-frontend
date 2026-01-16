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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
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
      .ensureValidForm(TypeOfSignInForm.form, implicit request => view(_)):
        implicit request =>
          val typeOfSignIn: TypeOfSignIn = request.formValue
          Redirect(AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.showSignInPage)
            .addToSession(typeOfSignIn)

  def showSignInPage: Action[AnyContent] =
    action
      .ensure(_.readAgentType.isDefined, Redirect(AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url))
      .ensure(_.readBusinessType.isDefined, Redirect(AppRoutes.apply.aboutyourbusiness.BusinessTypeSessionController.show.url))
      .ensure(_.readTypeOfSignIn.isDefined, Redirect(AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url)):
        implicit request =>
          val agentType: AgentType = request.getAgentType
          val businessType: BusinessType = request.getBusinessType
          val typeOfSignIn: TypeOfSignIn = request.getTypeOfSignIn
          val userRole: UserRole = request.readUserRole.getOrThrowExpectedDataMissing("UserRole is required to initiate agent application")

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
