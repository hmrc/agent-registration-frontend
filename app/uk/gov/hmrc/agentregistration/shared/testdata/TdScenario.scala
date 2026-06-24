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

package uk.gov.hmrc.agentregistration.shared.testdata

import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.LlpScenarios.AfterConfirmTwoChOfficers.tdApplicationFixture

trait TdScenario:

  val agentApplication: AgentApplication
  val individual1: Option[IndividualProvidedDetails]
  val individual2: Option[IndividualProvidedDetails]

object LlpScenarios:

  object AfterStarted
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterStarted
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterGrsDataReceived
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterGrsDataReceived
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterRefusalToDealWithCheckPass
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterRefusalToDealWithCheckPass
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterRefusalToDealWithCheckFail
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterRefusalToDealWithCheckFail
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterUnifiedCustomerRegistryUpdateIdentifiers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterUnifiedCustomerRegistryUpdateIdentifiers
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterUnifiedCustomerRegistryUpdateEmptyIdentifiers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterUnifiedCustomerRegistryUpdateEmptyIdentifiers
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterGlobalAsaEnrolmentCheckPass
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterGlobalAsaEnrolmentCheckPass
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterGlobalAsaEnrolmentCheckFail
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterGlobalAsaEnrolmentCheckFail
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterContactDetailsComplete
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterContactDetailsComplete
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterAgentDetailsComplete
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterAgentDetailsComplete
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterAmlsComplete
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterAmlsComplete
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterHmrcStandardForAgentsAgreed
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterHmrcStandardForAgentsAgreed
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterZeroCompaniesHouseOfficers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterZeroCompaniesHouseOfficers
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterConfirmCompaniesHouseOfficersYes
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterConfirmCompaniesHouseOfficersYes
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterNumberOfConfirmCompaniesHouseOfficers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterNumberOfConfirmCompaniesHouseOfficers
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterConfirmTwoChOfficers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterConfirmTwoChOfficers

    override val individual1: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates1.precreated)
    override val individual2: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates2.precreated)

  object AfterConfirmSixChOfficers
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterConfirmSixChOfficers

    override val individual1: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates1.precreated)
    override val individual2: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates2.precreated)

  object AfterConfirmCompaniesHouseOfficersNo
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterConfirmCompaniesHouseOfficersNo
    override val individual1: Option[IndividualProvidedDetails] = None
    override val individual2: Option[IndividualProvidedDetails] = None

  object AfterConfirmOtherRelevantTaxAdvisersNo
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterConfirmOtherRelevantTaxAdvisersNo
    override val individual1: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates1.precreated)
    override val individual2: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates2.precreated)


  object BeforeDeclarationSubmitted:
    def (i1Status, i2Status)
    object I1PrecreatedI2Precreated extends TdScenario
    object I1afterAccessConfirmedI2Precreated extends TdScenario
    object afterStartedConfirmedI2Precreated extends TdScenario
    object afterTelephoneNumberProvidedConfirmedI2Precreated extends TdScenario

  object AfterDeclarationSubmitted
  extends TdScenario:

    val tdApplicationFixture: TdApplicationFixture = TdApplicationFixture.make(this.toString)
    override val agentApplication: AgentApplication = tdApplicationFixture.applicationLlpInStates.afterDeclarationSubmitted
    override val individual1: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates1.afterFinished)
    override val individual2: Option[IndividualProvidedDetails] = Some(tdApplicationFixture.tdIndividualsInStates.invididualInStates2.afterFinished)
