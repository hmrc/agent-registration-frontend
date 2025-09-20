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

package uk.gov.hmrc.agentregistrationfrontend.testOnly.controllers

import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import com.softwaremill.quicklens.*
import sttp.model.Uri.UriContext
import uk.gov.hmrc.agentregistration.shared.upscan.*
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  applicationService: ApplicationService
)
extends FrontendController(mcc):

  val showAgentApplication: Action[AnyContent] = actions.getApplicationInProgress: request =>
    Ok(Json.prettyPrint(Json.toJson(request.agentApplication)))

  def setUploadToComplete(): Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>
      applicationService
        .upsert(
          request.agentApplication
            .modify(_.amlsDetails.each.amlsEvidence)
            .setTo(Some(UploadDetails(
              reference = request.agentApplication.getAmlsDetails.getAmlsEvidence.reference,
              status = UploadStatus.UploadedSuccessfully(
                name = "test.pdf",
                mimeType = "application/pdf",
                downloadUrl = ObjectStoreUrl(uri"http://example.com/download"),
                size = Some(12345),
                checksum = "checksum"
              )
            )))
        )
        .map(_ => Ok("upload set to complete"))
