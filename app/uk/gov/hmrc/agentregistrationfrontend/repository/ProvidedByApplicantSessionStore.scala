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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.mongo.cache.DataKey

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Repository for managing [[ProvidedByApplicant]] data.
  *
  * This repository is short-lived and tied to the user's session only. Data stored here will expire when the session ends or after the configured TTL
  * [[SessionStore]] configuration.
  */
class ProvidedByApplicantSessionStore @Inject() (sessionStore: SessionStore)(using ExecutionContext):

  private val dataKey: DataKey[ProvidedByApplicant] = DataKey("providedByApplicant")

  def upsert(providedByApplicant: ProvidedByApplicant)(using RequestHeader): Future[Unit] = sessionStore
    .putSession[ProvidedByApplicant](dataKey, providedByApplicant)
    .map(_ => ())

  def find()(using RequestHeader): Future[Option[ProvidedByApplicant]] = sessionStore.getFromSession(dataKey)
  def delete()(using RequestHeader): Future[Unit] = sessionStore.deleteFromSession(dataKey)
