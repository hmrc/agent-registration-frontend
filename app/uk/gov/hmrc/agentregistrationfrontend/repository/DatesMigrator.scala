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
import org.bson.conversions.Bson
import org.mongodb.scala.MongoCollection
import org.mongodb.scala.SingleObservableFuture
import org.mongodb.scala.model.Filters
import play.api.Logging
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.repository.DatesMigrator.*

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

// TODO: remove, with DatesMigratorStarter and the dates-migrator config, once the dates migration has run in every environment
/** Converts the dates that `upload` records hold as ISO strings into BSON dates. */
@Singleton
class DatesMigrator @Inject() (uploadRepo: UploadRepo)(using ExecutionContext)
extends Logging:

  /** @return the number of documents converted; none if the migration failed */
  def migrate(): Future[Long] = runUntilTwoQuietRuns(
    collection = uploadRepo.collection,
    filter = uploadsWithStringDates,
    update = uploadDatesConverted
  )
    .map: converted =>
      logger.info(s"Migrating '${UploadRepo.collectionName}': converted $converted documents in total DONE")
      converted
    .recover:
      case ex =>
        logger.error(s"Migrating '${UploadRepo.collectionName}': FAILED", ex)
        0L

  private def runUntilTwoQuietRuns(
    collection: MongoCollection[?],
    filter: Bson,
    update: Bson
  ): Future[Long] =
    val collectionName: String = collection.namespace.getCollectionName
    logger.info(s"Migrating '$collectionName': Started...")

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def run(
      runNumber: Int,
      quietRuns: Int,
      converted: Long
    ): Future[Long] = collection
      .updateMany(filter = filter, update = Seq(update))
      .toFuture()
      .map(_.getModifiedCount)
      .flatMap: convertedInRun =>
        logger.info(s"Migrating '$collectionName': converted $convertedInRun documents in run $runNumber")
        val quietRunsNow: Int = if convertedInRun === 0L then quietRuns + 1 else 0
        if quietRunsNow === 2
        then Future.successful(converted)
        else
          run(
            runNumber = runNumber + 1,
            quietRuns = quietRunsNow,
            converted = converted + convertedInRun
          )

    run(
      runNumber = 1,
      quietRuns = 0,
      converted = 0L
    )

object DatesMigrator:

  private val uploadsWithStringDates: Bson = Filters.bsonType("createdAt", BsonType.STRING)

  private val uploadDatesConverted: Bson = BsonDocument.parse(
    // language=JSON
    """{ "$set": {
      |  "createdAt": { "$convert": { "input": "$createdAt", "to": "date", "onError": "$createdAt", "onNull": "$createdAt" } }
      |} }""".stripMargin
  )
