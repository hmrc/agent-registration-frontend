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
import uk.gov.hmrc.agentregistration.shared.contactdetails.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdBase

trait TdSectionContactDetails {
  dependencies: TdBase =>

  class TdAgentApplicationLlpWithSectionContactDetails(baseForSectionContactDetails: AgentApplicationLlp):

    object sectionContactDetails:

      private object ApplicantNameHelper:

        val afterNameDeclared: ApplicantName = ApplicantName("Miss Alexa Fantastic")

      val afterNameDeclared: AgentApplicationLlp = baseForSectionContactDetails
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = ApplicantNameHelper.afterNameDeclared
        )))

      val afterTelephoneNumberProvided: AgentApplicationLlp = afterNameDeclared
        .modify(_.applicantContactDetails.each.telephoneNumber)
        .setTo(Some(dependencies.telephoneNumber))

      val afterEmailAddressProvided: AgentApplicationLlp = afterTelephoneNumberProvided
        .modify(_.applicantContactDetails.each.applicantEmailAddress)
        .setTo(Some(ApplicantEmailAddressHelper.afterEmailAddressProvided))

      val afterEmailAddressVerified: AgentApplicationLlp = afterEmailAddressProvided
        .modify(_.applicantContactDetails.each.applicantEmailAddress)
        .setTo(Some(ApplicantEmailAddressHelper.afterEmailAddressVerified))

  private object ApplicantEmailAddressHelper:

    val afterEmailAddressProvided: ApplicantEmailAddress = ApplicantEmailAddress(
      emailAddress = dependencies.applicantEmailAddress,
      isVerified = false
    )
    val afterEmailAddressVerified: ApplicantEmailAddress = afterEmailAddressProvided.copy(isVerified = true)

}
