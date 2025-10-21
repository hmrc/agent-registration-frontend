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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

import uk.gov.hmrc.agentregistration.shared.ApplicationState.GrsDataReceived
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.contactdetails.*
import com.softwaremill.quicklens.*

trait TdAgentApplicationLlp { dependencies: TdBase =>

  object agentApplicationLlp:

    val afterStarted: AgentApplicationLlp = AgentApplicationLlp(
      internalUserId = dependencies.internalUserId,
      createdAt = dependencies.instant,
      applicationState = ApplicationState.Started,
      saUtr = Some(dependencies.saUtr),
      businessDetails = None,
      applicantContactDetails = None,
      amlsDetails = None
    )

    private val businessDetailsLlp: BusinessDetailsLlp = BusinessDetailsLlp(
      safeId = dependencies.safeId,
      saUtr = dependencies.saUtr,
      companyProfile = Some(dependencies.companyProfile)
    )

    val afterGrsDataReceived: AgentApplicationLlp = afterStarted.copy(
      businessDetails = Some(businessDetailsLlp),
      applicationState = GrsDataReceived
    )

    object whenApplicantIsAMember:

      object name:

        val afterRoleSelected: ApplicantName.NameOfMember = ApplicantName.NameOfMember(
          memberNameQuery = None,
          companiesHouseOfficer = None
        )
        val afterQuery: ApplicantName.NameOfMember = afterRoleSelected.copy(
          memberNameQuery = Some(
            CompaniesHouseNameQuery(
              firstName = "Tay",
              lastName = "Reed"
            )
          )
        )

        val afterChosenOfficer: ApplicantName.NameOfMember = afterQuery.copy(
          companiesHouseOfficer = Some(
            CompaniesHouseOfficer(
              name = "Taylor Reed",
              dateOfBirth = Some(CompaniesHouseDateOfBirth(
                day = Some(12),
                month = 11,
                year = 1990
              ))
            )
          )
        )

      val afterRoleSelected: AgentApplicationLlp = afterGrsDataReceived
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = name.afterRoleSelected,
          telephoneNumber = None
        )))

      val afterNameQueryProvided: AgentApplicationLlp = afterRoleSelected
        .modify(_.applicantContactDetails.each.applicantName)
        .setTo(name.afterQuery)

      val afterOfficerChosen: AgentApplicationLlp = afterRoleSelected
        .modify(_.applicantContactDetails.each.applicantName)
        .setTo(name.afterChosenOfficer)

      val afterTelephoneNumberProvided: AgentApplicationLlp = afterOfficerChosen
        .modify(_.applicantContactDetails.each.telephoneNumber)
        .setTo(Some(dependencies.telephoneNumber))

    object whenApplicantIsAuthorised:

      object name:

        val afterRoleSelected: ApplicantName.NameOfAuthorised = ApplicantName.NameOfAuthorised(
          name = None
        )

        val afterNameDeclared: ApplicantName.NameOfAuthorised = afterRoleSelected.copy(
          name = Some("Miss Alexa Fantastic")
        )

      val afterRoleSelected: AgentApplicationLlp = afterGrsDataReceived
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = name.afterRoleSelected,
          telephoneNumber = None
        )))

      val afterNameDeclared: AgentApplicationLlp = afterRoleSelected
        .modify(_.applicantContactDetails.each.applicantName)
        .setTo(name.afterNameDeclared)

      val afterTelephoneNumber: AgentApplicationLlp = afterRoleSelected
        .modify(_.applicantContactDetails.each.telephoneNumber)
        .setTo(dependencies.telephoneNumber)

}
