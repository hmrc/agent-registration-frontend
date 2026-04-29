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

package uk.gov.hmrc.agentregistrationfrontend.repository

import org.mongodb.scala.ObservableFuture
import org.mongodb.scala.model.Filters
import play.api.libs.json.*
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.cache.DataKey

class SessionStoreEncryptionSpec
extends ISpec:

  override protected def configOverrides: Map[String, Any] = Map("fieldLevelEncryption.enable" -> true)

  val sessionStore: SessionStore = app.injector.instanceOf[SessionStore]
  val mongo: MongoComponent = app.injector.instanceOf[MongoComponent]

  case class TestData(secret: String)
  object TestData:
    given format: OFormat[TestData] = Json.format[TestData]

  "SessionStore with encryption enabled" should:

    "store and retrieve encrypted value in mongo" in:
      val sessionId = "session-encrypted-1"
      given rh: RequestHeader = FakeRequest().withSession(SessionKeys.sessionId -> sessionId)

      val key = DataKey[TestData]("testKey")
      val value = TestData("sensitive value")

      sessionStore.putSession[TestData](key, value).futureValue
      val retrievedTestData = sessionStore.getFromSession[TestData](key).futureValue
      retrievedTestData.value shouldBe value

      val sessionsCollection = mongo.database.getCollection("sessions")
      val rawDocs =
        sessionsCollection
          .find(Filters.equal("_id", sessionId))
          .toFuture()
          .futureValue
      val json = rawDocs.map(_.toJson())

      // JSON must NOT contain the plaintext secret because it should be encrypted
      json.headOption.getOrElse("sensitive value") shouldNot include("sensitive value")

      sessionsCollection.deleteOne(Filters.equal("_id", sessionId)).toFuture().futureValue
