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

package uk.gov.hmrc.agentregistrationfrontend.model

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.util.Errors.*

/** Individual record as provided by the applicant. This final case class represents the data entered by an applicant that can be used to hydrate the
  * IndividualProvidedDetails record for the given _id.
  */
final case class ProvidedByApplicant(
  individualProvidedDetailsId: IndividualProvidedDetailsId,
  individualName: IndividualName,
  individualDateOfBirth: Option[IndividualDateOfBirth] = None,
  telephoneNumber: Option[TelephoneNumber] = None,
  emailAddress: Option[EmailAddress] = None,
  individualNino: Option[IndividualNino] = None,
  individualSaUtr: Option[IndividualSaUtr] = None
):

  def getEmailAddress: EmailAddress = emailAddress.getOrThrowExpectedDataMissing("Email address")
  def getTelephoneNumber: TelephoneNumber = telephoneNumber.getOrThrowExpectedDataMissing("Telephone number")
  def getNino: IndividualNino = individualNino.getOrThrowExpectedDataMissing("Nino")
  def getSaUtr: IndividualSaUtr = individualSaUtr.getOrThrowExpectedDataMissing("SaUtr")
  def getDateOfBirth: IndividualDateOfBirth = individualDateOfBirth.getOrThrowExpectedDataMissing("Date of birth")

object ProvidedByApplicant:
  given format: OFormat[ProvidedByApplicant] = Json.format[ProvidedByApplicant]
