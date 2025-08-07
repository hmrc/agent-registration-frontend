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

package uk.gov.hmrc.agentregistrationfrontend.controllers

import com.softwaremill.quicklens.*
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole.Owner
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsFailed
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsNotCalled
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsRegistered
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistration
import uk.gov.hmrc.agentregistrationfrontend.model.GrsResponse
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.GrsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class GrsController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  grsService: GrsService,
  applicationService: ApplicationService,
  placeholder: SimplePage
)(implicit ec: ExecutionContext)
extends FrontendController(mcc)
with I18nSupport:

  def startJourney: Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      (request.agentApplication.aboutYourApplication.businessType, request.agentApplication.aboutYourApplication.userRole) match {
        case (Some(businessType), Some(userRole)) =>
          grsService
            .createGrsJourney(businessType, userRole == Owner)
            .map(Redirect(_))
        case _ => Future.successful(Redirect(routes.AgentApplicationController.startRegistration))
      }

  def journeyCallback(
    businessType: BusinessType,
    journeyId: String
  ): Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      grsService
        .getGrsResponse(businessType, journeyId)
        .map {
          case _ if businessType != request.agentApplication.getBusinessType => Redirect(routes.AgentApplicationController.startRegistration) // User changed answer while on GRS
          case grsResponse if grsResponse.identifiersMatch && grsResponse.registration.registeredBusinessPartnerId.nonEmpty =>
            applicationService
              .upsert(
                request
                  .agentApplication
                  .modify(_.utr)
                  .setTo(Some(grsResponse.utr))
                  .modify(_.businessDetails)
                  .setTo(Some(grsResponse.toBusinessDetails(businessType)))
              )
            Ok(Json.prettyPrint(Json.toJson(grsResponse)))
          case grsResponse if !grsResponse.identifiersMatch && grsResponse.registration.registrationStatus.equals(GrsNotCalled) =>
            Ok(placeholder(
              h1 = "Identifiers did not match...",
              bodyText = Some(
                "Placeholder for the Identifier match failure page..."
              )
            ))
          case grsResponse if grsResponse.identifiersMatch && grsResponse.registration.registrationStatus.equals(GrsFailed) =>
            Ok(placeholder(
              h1 = "Registration call on GRS failed...",
              bodyText = Some(
                "Placeholder for the Business registration grs error page..."
              )
            ))
          case _ => throw new Exception(s"[GrsController] Unexpected response from GRS for journey: $journeyId")
        }
