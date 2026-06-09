/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistration.shared

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.risking.EntityFailure
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

import java.time.LocalDate

class ApplicationStateSpec
extends UnitSpec:

  val date: LocalDate = LocalDate.of(2024, 1, 15)

  "serialize and deserialize Started" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"Started"}"""
    )
    Json.toJson[ApplicationState](ApplicationState.Started) shouldBe json
    json.as[ApplicationState] shouldBe ApplicationState.Started

  "serialize and deserialize GrsDataReceived" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"GrsDataReceived"}"""
    )
    Json.toJson[ApplicationState](ApplicationState.GrsDataReceived) shouldBe json
    json.as[ApplicationState] shouldBe ApplicationState.GrsDataReceived

  "serialize and deserialize SentForRisking" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"SentForRisking"}"""
    )
    Json.toJson[ApplicationState](ApplicationState.SentForRisking) shouldBe json
    json.as[ApplicationState] shouldBe ApplicationState.SentForRisking

  "serialize and deserialize RiskingInProgress" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"RiskingInProgress"}"""
    )
    Json.toJson[ApplicationState](ApplicationState.RiskingInProgress) shouldBe json
    json.as[ApplicationState] shouldBe ApplicationState.RiskingInProgress

  "serialize and deserialize Approved" in:
    val json: JsValue = Json.parse(
      // language=JSON
      """{"type":"Approved","riskingCompletedDate":"2024-01-15"}"""
    )
    Json.toJson[ApplicationState](ApplicationState.Approved(date)) shouldBe json
    json.as[ApplicationState] shouldBe ApplicationState.Approved(date)

  "serialize and deserialize FailedFixable" in:
    val state: ApplicationState = ApplicationState.FailedFixable(
      fixes = Seq(EntityFix.AmlsFix(None, None)),
      riskingCompletedDate = date,
      correctiveActionExpiryDate = Some(LocalDate.of(2024, 3, 31))
    )
    val json: JsValue = Json.toJson[ApplicationState](state)
    json.as[ApplicationState] shouldBe state

  "serialize and deserialize FailedNonFixable" in:
    val state: ApplicationState = ApplicationState.FailedNonFixable(
      failures = Seq(EntityFailure._3._1),
      riskingCompletedDate = date
    )
    val json: JsValue = Json.toJson[ApplicationState](state)
    json.as[ApplicationState] shouldBe state

  "read from legacy string format" in:
    Json.parse(""""Started"""").as[ApplicationState] shouldBe ApplicationState.Started
    Json.parse(""""GrsDataReceived"""").as[ApplicationState] shouldBe ApplicationState.GrsDataReceived
    Json.parse(""""SentForRisking"""").as[ApplicationState] shouldBe ApplicationState.SentForRisking
