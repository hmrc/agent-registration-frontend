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

import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.SensitiveWrapper.*
import uk.gov.hmrc.crypto.json.JsonEncryption.sensitiveDecrypter
import uk.gov.hmrc.crypto.json.JsonEncryption.sensitiveEncrypter
import uk.gov.hmrc.crypto.Decrypter
import uk.gov.hmrc.crypto.Encrypter
import uk.gov.hmrc.crypto.Sensitive
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mdc.Mdc
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.cache.DataKey
import uk.gov.hmrc.mongo.cache.SessionCacheRepository as CacheRepository

import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Repository for managing [[ProvidedByApplicant]] data in the session cache.
  *
  * This repository is short-lived and tied to the user's session only. Data stored here will expire when the session ends or after the configured TTL
  * [[ProvidedByApplicantSessionStore]] configuration.
  */
class ProvidedByApplicantRepo @Inject() (providedByApplicantSessionStore: ProvidedByApplicantSessionStore)(using ExecutionContext):

  private val dataKey: DataKey[ProvidedByApplicant] = DataKey("providedByApplicant")

  def upsert(providedByApplicant: ProvidedByApplicant)(using RequestHeader): Future[Unit] = providedByApplicantSessionStore
    .putSession[ProvidedByApplicant](dataKey, providedByApplicant)
    .map(_ => ())

  def find()(using RequestHeader): Future[Option[ProvidedByApplicant]] = providedByApplicantSessionStore.getFromSession(dataKey)
  def delete()(using RequestHeader): Future[Unit] = providedByApplicantSessionStore.deleteFromSession(dataKey)

/** Low-level session cache repository used as a delegate to implement concrete session-based repositories.
  *
  * It is NOT intended to be used directly - instead, create specific repository classes (like [[ProvidedByApplicantRepo]]) that delegate to this repository for
  * their storage needs.
  */
@Singleton
class ProvidedByApplicantSessionStore @Inject() (
  val mongoComponent: MongoComponent,
  timestampSupport: TimestampSupport
)(using
  ec: ExecutionContext,
  @Named("aes") val crypto: Encrypter & Decrypter
)
extends CacheRepository(
  mongoComponent = mongoComponent,
  collectionName = "sessions",
  replaceIndexes = true,
  ttl = 15.minutes, // TODO: match with the session duration from the config
  timestampSupport = timestampSupport,
  sessionIdKey = SessionKeys.sessionId
):

  override def putSession[T: Writes](
    dataKey: DataKey[T],
    data: T
  )(using RequestHeader): Future[(String, String)] = Mdc.preservingMdc:
    super.putSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap), SensitiveWrapper(data))

  override def getFromSession[T: Reads](
    dataKey: DataKey[T]
  )(using RequestHeader): Future[Option[T]] = Mdc.preservingMdc:
    super.getFromSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap)).map(_.map(_.decryptedValue))

  override def deleteFromSession[T](
    dataKey: DataKey[T]
  )(using RequestHeader): Future[Unit] = Mdc.preservingMdc:
    super.deleteFromSession(DataKey[SensitiveWrapper[T]](dataKey.unwrap))

final case class SensitiveWrapper[T](override val decryptedValue: T)
extends Sensitive[T]

object SensitiveWrapper:

  given reads[T](using
    reads: Reads[T],
    crypto: Encrypter & Decrypter
  ): Reads[SensitiveWrapper[T]] = sensitiveDecrypter(SensitiveWrapper[T])

  given writes[T](using
    writes: Writes[T],
    crypto: Encrypter & Decrypter
  ): Writes[SensitiveWrapper[T]] = sensitiveEncrypter
