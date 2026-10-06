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
import uk.gov.hmrc.agentregistration.shared.upload.UploadId
import uk.gov.hmrc.agentregistration.shared.util.RestDateFormats
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.Upload
import uk.gov.hmrc.agentregistrationfrontend.testsupport.MongoISpec

class UploadRepoSpec
extends MongoISpec:

  "upsert" should:

    "store createdAt as a BSON date" in:
      uploadRepo.upsert(upload).futureValue

      rawUpload.get("createdAt").getBsonType shouldBe BsonType.DATE_TIME

  "findById" should:

    // TODO: remove with the ISO-string fallback in MongoDateFormats once the dates migration has run in every environment
    "read an upload stored before its date was migrated to a BSON date" in:
      storeWithIsoStringDate(upload)

      uploadRepo.findById(upload.uploadId).futureValue.value shouldBe upload

  "findLatestByInternalUserId" should:

    "return the upload created last" in:
      val olderUpload: Upload = tdAll.uploadInProgress.copy(_id = UploadId("upload-id-older"), createdAt = upload.createdAt.minusSeconds(60))
      uploadRepo.upsert(upload).futureValue
      uploadRepo.upsert(olderUpload).futureValue

      uploadRepo.findLatestByInternalUserId(upload.internalUserId).futureValue.value shouldBe upload

  private lazy val uploadRepo: UploadRepo = app.injector.instanceOf[UploadRepo]

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

  private def rawUpload: BsonDocument =
    uploadRepo
      .collection
      .withDocumentClass[BsonDocument]()
      .find(Filters.eq("_id", upload.uploadId.value))
      .headOption()
      .futureValue
      .value
