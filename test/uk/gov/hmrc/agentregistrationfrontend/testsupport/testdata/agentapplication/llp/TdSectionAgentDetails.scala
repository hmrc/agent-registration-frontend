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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.agentapplication.llp

import com.softwaremill.quicklens.*
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentBusinessName
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentDetails
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentEmailAddress
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentTelephoneNumber
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentVerifiedEmailAddress
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdSectionAgentDetails {
  dependencies: TdBase =>

  class TdAgentApplicationLlpWithSectionAgentDetails(baseForSectionAgentDetails: AgentApplicationLlp):

    object sectionAgentDetails:

      object whenUsingExistingCompanyName:

        val afterBusinessNameProvided: AgentApplicationLlp = baseForSectionAgentDetails
          .modify(_.agentDetails)
          .setTo(Some(AgentDetails(
            businessName = AgentBusinessName(
              agentBusinessName = dependencies.companyName,
              otherAgentBusinessName = None
            )
          )))
        val afterBprTelephoneNumberSelected: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterBprTelephoneNumberSelected))
        val afterContactTelephoneSelected: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterContactTelephoneSelected))
        val afterOtherTelephoneNumberProvided: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterOtherTelephoneNumberProvided))
        val afterContactEmailAddressSelected: AgentApplicationLlp = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterContactEmailAddressSelected))
        val afterBprEmailAddressSelected: AgentApplicationLlp = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterBprEmailAddressSelected))
        val afterOtherEmailAddressSelected: AgentApplicationLlp = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterOtherEmailAddressSelected))
        val afterVerifiedEmailAddressSelected: AgentApplicationLlp = afterContactTelephoneSelected
          .modify(_.agentDetails.each.agentEmailAddress)
          .setTo(Some(AgentEmailAddressHelper.afterVerifiedEmailAddressSelected))

      object whenProvidingNewBusinessName:

        val newBusinessName: String = "New Agent Business Llp"
        val afterBusinessNameProvided: AgentApplicationLlp = baseForSectionAgentDetails
          .modify(_.agentDetails)
          .setTo(Some(AgentDetails(
            businessName = AgentBusinessName(
              agentBusinessName = "other",
              otherAgentBusinessName = Some(newBusinessName)
            )
          )))
        val afterBprTelephoneNumberSelected: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterBprTelephoneNumberSelected))
        val afterContactTelephoneSelected: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterContactTelephoneSelected))
        val afterOtherTelephoneNumberProvided: AgentApplicationLlp = afterBusinessNameProvided
          .modify(_.agentDetails.each.telephoneNumber)
          .setTo(Some(AgentTelephoneNumberHelper.afterOtherTelephoneNumberProvided))

    private object AgentTelephoneNumberHelper:

      val afterBprTelephoneNumberSelected: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = dependencies.bprPrimaryTelephoneNumber,
        otherAgentTelephoneNumber = None
      )

      val afterContactTelephoneSelected: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = dependencies.telephoneNumber.value,
        otherAgentTelephoneNumber = None
      )

      val afterOtherTelephoneNumberProvided: AgentTelephoneNumber = AgentTelephoneNumber(
        agentTelephoneNumber = "other",
        otherAgentTelephoneNumber = Some(dependencies.newTelephoneNumber)
      )

    private object AgentEmailAddressHelper:

      val afterContactEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = dependencies.applicantEmailAddress.value,
          otherAgentEmailAddress = None
        ),
        isVerified = true
      )

      val afterBprEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = dependencies.bprEmailAddress,
          otherAgentEmailAddress = None
        ),
        isVerified = true
      )

      val afterOtherEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = "other",
          otherAgentEmailAddress = Some(dependencies.newEmailAddress)
        ),
        isVerified = false
      )

      val afterVerifiedEmailAddressSelected: AgentVerifiedEmailAddress = AgentVerifiedEmailAddress(
        emailAddress = AgentEmailAddress(
          agentEmailAddress = "other",
          otherAgentEmailAddress = Some(dependencies.newEmailAddress)
        ),
        isVerified = true
      )

}
