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
import org.mongodb.scala.model.Updates
import uk.gov.hmrc.agentregistration.shared.util.RestDateFormats
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.testsupport.MongoISpec

class DatesMigratorSpec
extends MongoISpec:

  "migrate" should:

    "convert the date of an upload" in:
      storeWithIsoStringDate(upload)

      migrator.migrate().futureValue shouldBe 1L

      createdAtType shouldBe BsonType.DATE_TIME
      uploadRepo.findById(upload.uploadId).futureValue.value shouldBe upload

    "convert nothing when every date is a BSON date already" in:
      uploadRepo.upsert(upload).futureValue

      migrator.migrate().futureValue shouldBe 0L

    "keep only the milliseconds of a date stored with more precision, as a BSON date holds no more" in:
      storeWithIsoStringDate(upload.copy(createdAt = upload.createdAt.plusNanos(123456L)))

      migrator.migrate().futureValue shouldBe 1L

      uploadRepo.findById(upload.uploadId).futureValue.value shouldBe upload

    "leave a date it cannot convert" in:
      storeWithIsoStringDate(upload)
      uploadRepo
        .collection
        .updateOne(Filters.eq("_id", upload.uploadId.value), Updates.set("createdAt", "not-a-date"))
        .toFuture()
        .futureValue

      migrator.migrate().futureValue shouldBe 0L

      createdAtType shouldBe BsonType.STRING

    "convert nothing, rather than fail, when the update fails" in:
      storeWithIsoStringDate(upload)
      makeUploadsUpdateFail()

      migrator.migrate().futureValue shouldBe 0L

      createdAtType shouldBe BsonType.STRING

  private lazy val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]
  private lazy val migrator: DatesMigrator = app.injector.instanceOf[DatesMigrator]

  private lazy val upload: Upload = tdAll.uploadUploadedSuccessfully

  // stores the upload as written before the migration: with its date as an ISO string
  private def storeWithIsoStringDate(upload: Upload): Unit =
    uploadRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .insertOne(BsonDocument.parse(Upload.makeFormat(using RestDateFormats.instantFormat).writes(upload).toString))
      .toFuture()
      .futureValue
    ()

  private def makeUploadsUpdateFail(): Unit =
    mongoDatabase
      .runCommand(BsonDocument.parse(s"""{ "collMod": "${UploadRepo.collectionName}", "validator": { "createdAt": { "$$type": "string" } } }"""))
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
