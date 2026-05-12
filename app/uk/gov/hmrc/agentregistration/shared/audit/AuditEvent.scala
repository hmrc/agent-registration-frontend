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

package uk.gov.hmrc.agentregistration.shared.audit

import play.api.libs.json.Format
import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr

sealed trait AuditEvent:

  val applicationReference: ApplicationReference
  val auditType: String = this.getClass.getSimpleName

// The application does not support non-uk for now so isUkEntity is set to always true
final case class StartOrContinueApplication(
  applicationReference: ApplicationReference,
  journeyType: StartOrContinueApplication.JourneyType,
  entityType: BusinessType,
  isUkEntity: Boolean = true
)
extends AuditEvent

object StartOrContinueApplication:

  def make(
    agentApplication: AgentApplication,
    journeyType: JourneyType
  ): StartOrContinueApplication = StartOrContinueApplication(
    applicationReference = agentApplication.applicationReference,
    journeyType = journeyType,
    entityType = agentApplication.businessType
  )

  given format: Format[StartOrContinueApplication] = Json.format[StartOrContinueApplication]

  enum JourneyType:

    case Start
    case Continue

  object JourneyType:
    given format: Format[JourneyType] = JsonFormatsFactory.makeEnumFormat

final case class IndividualSubmission(
  applicationReference: ApplicationReference,
  personReference: PersonReference,
  fullName: String,
  providedByApplicant: Boolean,
  nino: Option[IndividualNino] = None,
  sautr: Option[IndividualSaUtr] = None,
  lastIndividualResponse: Boolean
)
extends AuditEvent

object IndividualSubmission:

  def make(
    applicationReference: ApplicationReference,
    individualProvidedDetails: IndividualProvidedDetails,
    providedByApplicant: Boolean,
    lastIndividualResponse: Boolean
  ): IndividualSubmission = IndividualSubmission(
    applicationReference = applicationReference,
    personReference = individualProvidedDetails.personReference,
    fullName = individualProvidedDetails.individualName.value,
    providedByApplicant = providedByApplicant,
    nino = individualProvidedDetails.individualNino,
    sautr = individualProvidedDetails.individualSaUtr,
    lastIndividualResponse = lastIndividualResponse
  )

  given format: Format[IndividualSubmission] = Json.format[IndividualSubmission]
