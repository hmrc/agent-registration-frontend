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

import org.bson.BsonDocument
import org.bson.BsonType
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import uk.gov.hmrc.agentregistration.shared.util.RestDateFormats
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.testsupport.MongoISpec
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.duration.*

class DatesMigratorStarterSpec
extends MongoISpec:

  "start" should:

    "run the dates migrator when no other instance holds the lock" in:
      storeWithIsoStringDate(upload)

      starter.start().futureValue shouldBe Some(1L)

      createdAtType shouldBe BsonType.DATE_TIME

    "do nothing while another instance holds the lock" in:
      storeWithIsoStringDate(upload)
      mongoLockRepository.takeLock(
        lockId = DatesMigratorStarter.lockId,
        owner = "another-instance",
        ttl = 1.hour
      ).futureValue shouldBe defined

      starter.start().futureValue shouldBe None

      createdAtType shouldBe BsonType.STRING

  private lazy val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
  private lazy val starter: DatesMigratorStarter = app.injector.instanceOf[DatesMigratorStarter]
  private lazy val mongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]

  private lazy val upload: Upload = tdAll.uploadUploadedSuccessfully

  private def storeWithIsoStringDate(upload: Upload): Unit =
    uploadRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .insertOne(BsonDocument.parse(Upload.makeFormat(using RestDateFormats.instantFormat).writes(upload).toString))
      .toFuture()
      .futureValue
    ()

  private def createdAtType: BsonType =
    uploadRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", upload.uploadId.value))
      .headOption()
      .futureValue
      .value
      .get("createdAt")
      .getBsonType
