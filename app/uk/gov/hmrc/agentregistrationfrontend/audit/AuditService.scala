/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.audit

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.audit.StartOrContinueApplicationAuditEvent
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.bootstrap.config.AppName
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AuditService @Inject (auditConnector: DefaultAuditConnector)(using
  ec: ExecutionContext,
  config: Configuration
)
extends RequestAwareLogging:

  private final val auditSource = AppName.fromConfiguration(config)

  def auditStartOrContinueApplication(auditEvent: StartOrContinueApplicationAuditEvent)(using RequestHeader): Future[AuditResult] =
    logger.info(s"Auditing application ${auditEvent.auditType} event")
    auditConnector.sendEvent(
      DataEvent(
        auditSource = auditSource,
        auditType = auditEvent.auditType.toString,
        detail = Map(
          "applicationReference" -> auditEvent.applicationReference.value,
          "journeyType" -> auditEvent.journeyType.toString,
          "entityType" -> auditEvent.entityType.toString,
          "isUkEntity" -> auditEvent.isUkEntity.toString
        )
      )
    )
