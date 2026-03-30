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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.testdata.providedetails.individual.TdIndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.testdata.TdBase
import uk.gov.hmrc.agentregistration.shared.testdata.TdGrsBusinessDetails
import uk.gov.hmrc.agentregistration.shared.testdata.agentapplication.TdAgentApplicationSoleTraderRepresentative

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

/** Test Data (Td) composition trait that combines All available test data instances.
  *
  * Implemented as composable traits to allow flexible customization of test data. For example:
  *
  * {{{
  * val td = new TdAll {
  *   override val saUtr: SaUtr = SaUtr("666667777")
  * }
  *
  * td.
  * }}}
  *
  * This allows reusing default test data while overriding specific values as needed.
  */
trait TdAll
extends TdBase,
  TdRequest,
  TdGrsBusinessDetails,
  TdGrsJourneyData,
  TdAgentApplicationRequest,
  TdAgentApplicationLlp,
  TdAgentApplicationGeneralPartnership,
  TdAgentApplicationLimitedCompany,
  TdAgentApplicationLimitedPartnership,
  TdAgentApplicationScottishLimitedPartnership,
  TdAgentApplicationScottishPartnership,
  TdAgentApplicationSoleTrader,
  TdAgentApplicationSoleTraderRepresentative,
  TdAllSections,
  TdIndividualProvidedDetails:

  object agentApplicationLlpSections:

    export agentApplicationLlp.*

    val baseForSectionAmls = agentApplicationLlp.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationLlp.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationLlp.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails

  object agentApplicationGeneralPartnershipSections:

    export agentApplicationGeneralPartnership.*

    val baseForSectionAmls = agentApplicationGeneralPartnership.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationGeneralPartnership.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationGeneralPartnership.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails

  object agentApplicationScottishPartnershipSections:

    export agentApplicationScottishPartnership.*

    val baseForSectionAmls = agentApplicationScottishPartnership.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationScottishPartnership.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationScottishPartnership.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails

  object agentApplicationLimitedCompanySections:

    export agentApplicationLimitedCompany.*

    val baseForSectionAmls = agentApplicationLimitedCompany.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationLimitedCompany.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationLimitedCompany.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails

  object agentApplicationLimitedPartnershipSections:

    export agentApplicationLimitedPartnership.*

    val baseForSectionAmls = agentApplicationLimitedPartnership.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationLimitedPartnership.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationLimitedPartnership.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails

  object agentApplicationScottishLimitedPartnershipSections:

    export agentApplicationScottishLimitedPartnership.*

    val baseForSectionAmls = agentApplicationScottishLimitedPartnership.afterGrsDataReceived
    protected val _withSectionAmls = new AgentApplicationWithSectionAmls(baseForSectionAmls = baseForSectionAmls)
    export _withSectionAmls.sectionAmls

    protected val _withSectionContactDetails =
      new TdAgentApplicationWithSectionContactDetails(baseForSectionContactDetails = agentApplicationScottishLimitedPartnership.afterGrsDataReceived)
    export _withSectionContactDetails.sectionContactDetails

    protected val _withSectionAgentDetails =
      new TdAgentApplicationWithSectionAgentDetails(baseForSectionAgentDetails = agentApplicationScottishLimitedPartnership.afterContactDetailsComplete)
    export _withSectionAgentDetails.sectionAgentDetails
