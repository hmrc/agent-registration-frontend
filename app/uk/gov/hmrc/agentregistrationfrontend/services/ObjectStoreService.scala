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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.RequestHeader
import sttp.model.Uri
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc
import uk.gov.hmrc.objectstore.client.Path
import uk.gov.hmrc.objectstore.client.RetentionPeriod
import uk.gov.hmrc.objectstore.client.Sha256Checksum
import uk.gov.hmrc.objectstore.client.play.PlayObjectStoreClient

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class ObjectStoreService @Inject() (
  playObjectStoreClient: PlayObjectStoreClient
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def deleteObject(path: Path.File)(using request: RequestHeader): Future[Unit] = playObjectStoreClient
    .deleteObject(
      path
    ).recover:
      case e => logger.error(s"Failed to delete object $path", e)

  /** Transfers the file from Upscan to Object Store if the upload was successful. Returns the Object Store file path if the transfer was successful, None
    * otherwise.
    */
  def transferFileToObjectStore(
    fileReference: FileUploadReference,
    downloadUrl: Uri,
    mimeType: String,
    checksum: String,
    fileName: String
  )(using request: RequestHeader): Future[Path.File] =
    val fileLocation: Path.File = Path.File(s"${fileReference.value}/$fileName")
    val contentSha256 = Sha256Checksum.fromHex(checksum)
    playObjectStoreClient.uploadFromUrl(
      from = downloadUrl.toJavaUri.toURL,
      to = fileLocation,
      retentionPeriod = RetentionPeriod.SixMonths, // TODO: how long do we need to keep these files?
      contentType = Some(mimeType),
      contentSha256 = Some(contentSha256)
    )
      .map(_.location)
