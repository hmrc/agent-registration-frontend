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
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.audit.IndividualSubmission
import uk.gov.hmrc.agentregistration.shared.audit.StartOrContinueApplication
import uk.gov.hmrc.agentregistration.shared.audit.StartOrContinueApplication.JourneyType
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.connectors.IndividualProvidedDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport
import uk.gov.hmrc.play.audit.DefaultAuditConnector
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.hc

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class AuditService @Inject (
  auditConnector: DefaultAuditConnector,
  individualProvidedDetailsConnector: IndividualProvidedDetailsConnector
)(using
  ec: ExecutionContext
)
extends RequestAwareLogging:

  def auditContinueApplication(agentApplication: AgentApplication)(using request: RequestHeader): Unit =
    val auditEvent = StartOrContinueApplication.make(agentApplication, JourneyType.Continue)
    logger.info(s"Auditing continue application event")
    auditConnector.sendExplicitAudit(
      auditType = auditEvent.auditType,
      detail = auditEvent
    )

  def auditStartApplication(agentApplication: AgentApplication)(using RequestHeader): Unit =
    val auditEvent = StartOrContinueApplication.make(agentApplication, JourneyType.Start)
    logger.info(s"Auditing start application event")
    auditConnector.sendExplicitAudit(
      auditType = auditEvent.auditType,
      detail = auditEvent
    )

  def auditIndividualSubmission(
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails,
    providedByApplicant: Boolean
  )(using RequestHeader): Future[Unit] =
    for
      individualsOnApplication <- individualProvidedDetailsConnector.findAllForMatching(individualProvidedDetails.agentApplicationId)
      lastIndividualResponse = individualsOnApplication.filterNot(_.personReference === individualProvidedDetails.personReference) forall (_.hasFinished)
      auditEvent = IndividualSubmission.make(
        applicationReference,
        individualProvidedDetails,
        providedByApplicant = providedByApplicant,
        lastIndividualResponse = lastIndividualResponse
      )
      _ =
        logger.info(s"Auditing individual submission event")
        auditConnector.sendExplicitAudit(
          auditType = auditEvent.auditType,
          detail = auditEvent
        )
    yield ()
