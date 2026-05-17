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
import StartOrContinueApplication.JourneyType
import play.api.libs.json.OWrites
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
)(using ExecutionContext)
extends RequestAwareLogging:

  def auditContinueApplication(agentApplication: AgentApplication)(using request: RequestHeader): Unit = sendEvent(StartOrContinueApplication(
    applicationReference = agentApplication.applicationReference,
    journeyType = JourneyType.Continue,
    entityType = agentApplication.businessType
  ))

  def auditStartApplication(agentApplication: AgentApplication)(using RequestHeader): Unit = sendEvent(StartOrContinueApplication(
    applicationReference = agentApplication.applicationReference,
    journeyType = JourneyType.Start,
    entityType = agentApplication.businessType
  ))

  def auditIndividualSubmission(
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails
  )(using RequestHeader): Future[Unit] =
    for
      individualsOnApplication <- individualProvidedDetailsConnector.findAllForMatching(individualProvidedDetails.agentApplicationId)
      lastIndividualResponse = individualsOnApplication.filterNot(_.personReference === individualProvidedDetails.personReference) forall (_.hasFinished)
      _ = sendEvent(IndividualSubmission(
        applicationReference = applicationReference,
        personReference = individualProvidedDetails.personReference,
        fullName = individualProvidedDetails.individualName,
        providedByApplicant = individualProvidedDetails.providedByApplicant.getOrElse(false),
        nino = individualProvidedDetails.individualNino,
        sautr = individualProvidedDetails.individualSaUtr,
        lastIndividualResponse = lastIndividualResponse
      ))
    yield ()

  def sendRiskingSubmissionEvent(
    agentApplication: AgentApplication,
    individualDetails: List[IndividualProvidedDetails]
  )(using RequestHeader): Unit = sendEvent(ApplicationSubmitted(
    applicationReference = agentApplication.applicationReference,
    linkId = agentApplication.linkId,
    isResubmission = false, // TODO Will need changing when risking resubmissions are added
    utr = agentApplication.getUtr,
    applicantDetails = agentApplication.getApplicantContactDetails,
    agentDetails = Some(agentApplication.getAgentDetails),
    amlsSupervisionDetails = agentApplication.amlsDetails,
    individualsList = individualDetails
  ))

  private def sendEvent[E <: AuditEvent](auditEvent: E)(using
    RequestHeader,
    OWrites[E]
  ): Unit =
    logger.debug(s"send ExplicitAudit event: ${auditEvent.auditType}...")
    auditConnector.sendExplicitAudit(
      auditType = auditEvent.auditType,
      detail = auditEvent
    )
