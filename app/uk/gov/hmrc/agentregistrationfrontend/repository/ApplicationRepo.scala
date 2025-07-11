/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.model.application.Application
import uk.gov.hmrc.agentregistrationfrontend.model.application.ApplicationId
import uk.gov.hmrc.agentregistrationfrontend.model.application.SessionId
import uk.gov.hmrc.agentregistrationfrontend.repository.ApplicationRepo.*
import uk.gov.hmrc.agentregistrationfrontend.repository.Repo.IdExtractor
import uk.gov.hmrc.agentregistrationfrontend.repository.Repo.IdString
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.Codecs

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import ApplicationRepo.given

@Singleton
final class ApplicationRepo @Inject() (
  mongoComponent: MongoComponent,
  config: AppConfig
)(using ec: ExecutionContext)
extends Repo[ApplicationId, Application](
  collectionName = "application",
  mongoComponent = mongoComponent,
  indexes = ApplicationRepo.indexes(config.ApplicationRepo.applicationRepoTtl),
  extraCodecs = Seq(Codecs.playFormatCodec(Application.format)),
  replaceIndexes = true
):

  def findBySessionId(sessionId: SessionId): Future[Option[Application]] = collection
    .find(filter = Filters.eq("sessionId", sessionId.value))
    .headOption()

object ApplicationRepo:

  given IdString[ApplicationId] =
    new IdString[ApplicationId]:
      override def idString(i: ApplicationId): String = i.value

  given IdExtractor[Application, ApplicationId] =
    new IdExtractor[Application, ApplicationId]:
      override def id(j: Application): ApplicationId = j.applicationId

  def indexes(cacheTtl: FiniteDuration): Seq[IndexModel] = Seq(
    IndexModel(
      keys = Indexes.ascending("lastUpdated"),
      indexOptions = IndexOptions().expireAfter(cacheTtl.toSeconds, TimeUnit.SECONDS).name("lastUpdatedIdx")
    ),
    IndexModel(
      keys = Indexes.ascending("sessionId"),
      IndexOptions().name("sessionIdIndex")
    ),
    IndexModel(
      keys = Indexes.ascending("utr"),
      IndexOptions().name("utr")
    )
  )
