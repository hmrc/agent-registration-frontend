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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.i18n.Lang
import play.api.i18n.MessagesApi
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.GrsConnector
import uk.gov.hmrc.agentregistrationfrontend.model.GrsJourneyConfig
import uk.gov.hmrc.agentregistrationfrontend.model.GrsResponse
import uk.gov.hmrc.agentregistrationfrontend.model.JourneyLabels
import uk.gov.hmrc.agentregistrationfrontend.model.TranslationLabels
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class GrsService @Inject() (
  grsConnector: GrsConnector,
  messagesApi: MessagesApi
)(implicit appConfig: AppConfig):

  def createGrsJourney(
    businessType: BusinessType,
    hasOwnership: Boolean
  )(using
    request: AuthorisedRequest[?]
  ): Future[String] = {
    val journeyConfig = createConfig(businessType, hasOwnership)

    grsConnector.createGrsJourney(journeyConfig, businessType)
  }

  def getGrsResponse(
    businessType: BusinessType,
    journeyId: String
  )(using request: AuthorisedRequest[?]): Future[GrsResponse] = grsConnector.getGrsResponse(businessType, journeyId)

  private def createConfig(
    businessType: BusinessType,
    hasOwnership: Boolean
  ): GrsJourneyConfig = {
    val callbackUrl = appConfig.grsJourneyCallbackUrl(businessType)

    val (fullNamePageLabel, welshFullNamePageLabel) =
      if (!hasOwnership && businessType == SoleTrader) {
        (
          messagesApi.translate("grs.optFullNamePageLabel", Nil)(using Lang("en")),
          messagesApi.translate("grs.optFullNamePageLabel", Nil)(using Lang("cy"))
        )
      }
      else {
        (None, None)
      }

    GrsJourneyConfig(
      continueUrl = callbackUrl,
      deskProServiceId = appConfig.contactFrontendId,
      signOutUrl = routes.SignOutController.signOut.url,
      accessibilityUrl = appConfig.accessibilityStatementPath,
      regime = appConfig.agentRegime,
      businessVerificationCheck = false,
      labels = Some(JourneyLabels(
        en = TranslationLabels(
          optServiceName = messagesApi.translate("service.name", Nil)(using Lang("en")),
          optFullNamePageLabel = fullNamePageLabel
        ),
        cy = TranslationLabels(
          optServiceName = messagesApi.translate("service.name", Nil)(using Lang("cy")),
          optFullNamePageLabel = welshFullNamePageLabel
        )
      ))
    )
  }
