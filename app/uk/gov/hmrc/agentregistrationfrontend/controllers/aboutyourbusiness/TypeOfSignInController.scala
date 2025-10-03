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
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Request
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.TypeOfSignInPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.SignInWithAgentDetailsPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.CreateSignInDetailsPage
import sttp.model.Uri
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeSessionValue

import javax.inject.Inject
import javax.inject.Singleton

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

  val show: Action[AnyContent] = action:
    implicit request =>
      request.readBusinessType match
        case None => Redirect(routes.BusinessTypeSessionController.show)
        case Some(pt @ BusinessTypeSessionValue.PartnershipType) =>
          request.readPartnershipType match
            case None => Redirect(routes.PartnershipTypeController.show)
            case Some(_) => showTypeOfSignInForm(request)
        case Some(_) => showTypeOfSignInForm(request)

  private def showTypeOfSignInForm(implicit request: Request[?]) =
    val form: Form[TypeOfSignIn] =
      request.readTypeOfSignIn match
        case Some(data) => TypeOfSignInForm.form.fill(data)
        case None => TypeOfSignInForm.form
    Ok(view(form))

  val submit: Action[AnyContent] = action:
    implicit request =>
      request.readBusinessType match
        case None => Redirect(routes.BusinessTypeSessionController.show)
        case Some(pt @ BusinessTypeSessionValue.PartnershipType) =>
          request.readPartnershipType match
            case None => Redirect(routes.PartnershipTypeController.show)
            case Some(_) => redirectToChosenSignIn(request)
        case Some(_) => redirectToChosenSignIn(request)

  private def redirectToChosenSignIn(implicit request: Request[?]) = TypeOfSignInForm.form.bindFromRequest().fold(
    formWithErrors =>
      BadRequest(view(formWithErrors)),
    typeOfSignIn =>
      val (agentType: AgentType, businessType: BusinessType) = request.requireAgentTypeAndBusinessType
      val signInLink = appConfig.signInUri(
        continueUri =
          uri"${appConfig.thisFrontendBaseUrl + applicationRoutes.GrsController.setUpGrsFromSignIn(
              agentType = agentType,
              businessType = businessType
            ).url}"
      )
      Redirect(routes.TypeOfSignInController.redirectToChosenSignIn(signInLink.toString))
        .addTypeOfSignInToSession(typeOfSignIn)
  )

  def redirectToChosenSignIn(signInLink: String): Action[AnyContent] = action:
    implicit request =>
      request.readTypeOfSignIn match
        case Some(HmrcOnlineServices) => Ok(signInWithAgentDetailsPage(uri"$signInLink"))
        case Some(CreateSignInDetails) => Ok(createSignInDetailsPage(uri"$signInLink"))
        case None => Redirect(routes.TypeOfSignInController.show)
