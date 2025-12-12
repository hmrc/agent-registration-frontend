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

import uk.gov.hmrc.agentregistration.shared.upscan.UploadDetails
import uk.gov.hmrc.agentregistration.shared.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class UpscanProgressService @Inject() (
  agentRegistrationConnector: AgentRegistrationConnector
):

  def initiate(uploadDetails: UploadDetails)(using request: AgentApplicationRequest[?]): Future[Unit] = agentRegistrationConnector
    .initiateUpscanUpload(uploadDetails)

  def getUpscanStatus()(using request: AgentApplicationRequest[?]): Future[Option[UploadStatus]] = agentRegistrationConnector
    .getUpscanStatus(
      request
        .agentApplication
        .getAmlsDetails
        .getAmlsEvidence
        .uploadId
    )
