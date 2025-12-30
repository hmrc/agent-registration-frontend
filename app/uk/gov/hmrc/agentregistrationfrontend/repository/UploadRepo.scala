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

import org.mongodb.scala.model.Filters
import org.mongodb.scala.model.IndexModel
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.model.Indexes
import org.mongodb.scala.model.Sorts
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadId
import uk.gov.hmrc.agentregistrationfrontend.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationfrontend.repository.Repo.IdString
import uk.gov.hmrc.agentregistrationfrontend.repository.UploadRepoHelp.given
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@Singleton
final class UploadRepo @Inject() (
  mongoComponent: MongoComponent,
  appConfig: AppConfig
)(using ec: ExecutionContext)
extends Repo[UploadId, Upload](
  collectionName = "upload",
  mongoComponent = mongoComponent,
  indexes = UploadRepoHelp.indexes(appConfig.UploadRepo.ttl),
  extraCodecs = Seq(Codecs.playFormatCodec(Upload.format)),
  replaceIndexes = true
):

  def findLatestByInternalUserId(internalUserId: InternalUserId): Future[Option[Upload]] = collection
    .find(
      filter = Filters.eq("internalUserId", internalUserId.value)
    )
    .sort(Sorts.descending("createdAt"))
    .headOption()

// when named it UploadRepo, Scala 3 compiler complains
// about cyclic reference error during compilation ...
object UploadRepoHelp:

  given IdString[UploadId] =
    new IdString[UploadId]:
      override def idString(i: UploadId): String = i.value

  given IdExtractor[Upload, UploadId] =
    new IdExtractor[Upload, UploadId]:
      override def id(upload: Upload): UploadId = upload.uploadId

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("internalUserId"),
      IndexOptions()
        .unique(false)
        .name("internalUserId")
    ),
    IndexModel(
      keys = Indexes.ascending("createdAt"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("createdAtIdx")
    )
  )
