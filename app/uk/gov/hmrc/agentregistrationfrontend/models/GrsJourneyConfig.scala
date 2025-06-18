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

package uk.gov.hmrc.agentregistrationfrontend.models

import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig

case class GrsJourneyConfig(continueUrl: String,
                            deskProServiceId: String,
                            signOutUrl: String,
                            accessibilityUrl: String,
                            regime: String,
                            businessVerificationCheck: Boolean,
                            labels: Option[JourneyLabels] = None)

object GrsJourneyConfig {
  implicit val format: Format[GrsJourneyConfig] = Json.format[GrsJourneyConfig]

  def createConfig(partyType: Option[String], isTransactor: Boolean, callbackUrl: String)
                  (implicit messagesApi: MessagesApi, appConfig: AppConfig): GrsJourneyConfig = {
    val (fullNamePageLabel, welshFullNamePageLabel) = if (isTransactor && (partyType.contains("sole-trader") || partyType.isEmpty)) {
      (
        messagesApi.translate("transactorName.optFullNamePageLabel", Nil)(Lang("en")),
        messagesApi.translate("transactorName.optFullNamePageLabel", Nil)(Lang("cy"))
      )
    } else {
      (None, None)
    }

    GrsJourneyConfig(
      continueUrl = callbackUrl,
      deskProServiceId = appConfig.contactFormServiceIdentifier, //placeholder
      signOutUrl = "/sign-out", //placeholder
      accessibilityUrl = "/a11y-statement", //placeholder
      regime = "AGNT", //placeholder
      businessVerificationCheck = true,
      labels = Some(JourneyLabels(
        en = TranslationLabels(
          optServiceName = messagesApi.translate("service.name", Nil)(Lang("en")),
          optFullNamePageLabel = fullNamePageLabel
        ),
        cy = TranslationLabels(
          optServiceName = messagesApi.translate("service.name", Nil)(Lang("cy")),
          optFullNamePageLabel = welshFullNamePageLabel
        )
      ))
    )
  }
}

case class JourneyLabels(en: TranslationLabels,
                         cy: TranslationLabels)

object JourneyLabels {
  implicit val format: OFormat[JourneyLabels] = Json.format[JourneyLabels]
}

// optFullNamePageLabel label is only supported by sole-trader-identification-frontend
case class TranslationLabels(optFullNamePageLabel: Option[String] = None,
                             optServiceName: Option[String])

object TranslationLabels {
  implicit val format: OFormat[TranslationLabels] = Json.format[TranslationLabels]
}