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

      object whenApplicantIsAMember:

        val firstNameQuery: String = "Tay"
        val lastNameQuery: String = "Reed"

        private object ApplicantNameHelper:

          val afterRoleSelected: ApplicantName.NameOfMember = ApplicantName.NameOfMember(
            memberNameQuery = None,
            companiesHouseOfficer = None
          )

          val afterQuery: ApplicantName.NameOfMember = afterRoleSelected.copy(
            memberNameQuery = Some(
              CompaniesHouseNameQuery(
                firstName = firstNameQuery,
                lastName = lastNameQuery
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

        val afterRoleSelected: AgentApplicationLlp = baseForSectionContactDetails
          .modify(_.applicantContactDetails)
          .setTo(Some(ApplicantContactDetails(
            applicantName = ApplicantNameHelper.afterRoleSelected,
            telephoneNumber = None
          )))

        val afterNameQueryProvided: AgentApplicationLlp = afterRoleSelected
          .modify(_.applicantContactDetails.each.applicantName)
          .setTo(ApplicantNameHelper.afterQuery)

        val afterOfficerChosen: AgentApplicationLlp = afterRoleSelected
          .modify(_.applicantContactDetails.each.applicantName)
          .setTo(ApplicantNameHelper.afterChosenOfficer)

        val afterTelephoneNumberProvided: AgentApplicationLlp = afterOfficerChosen
          .modify(_.applicantContactDetails.each.telephoneNumber)
          .setTo(Some(dependencies.telephoneNumber))

      object whenApplicantIsAuthorised:

        private object ApplicantNameHelper:

          val afterRoleSelected: ApplicantName.NameOfAuthorised = ApplicantName.NameOfAuthorised(name = None)
          val afterNameDeclared: ApplicantName.NameOfAuthorised = afterRoleSelected.copy(name = Some("Miss Alexa Fantastic"))

        val afterRoleSelected: AgentApplicationLlp = baseForSectionContactDetails
          .modify(_.applicantContactDetails)
          .setTo(Some(ApplicantContactDetails(
            applicantName = ApplicantNameHelper.afterRoleSelected,
            telephoneNumber = None
          )))

        val afterNameDeclared: AgentApplicationLlp = afterRoleSelected
          .modify(_.applicantContactDetails.each.applicantName)
          .setTo(ApplicantNameHelper.afterNameDeclared)

        val afterTelephoneNumberProvided: AgentApplicationLlp = afterNameDeclared
          .modify(_.applicantContactDetails.each.telephoneNumber)
          .setTo(Some(dependencies.telephoneNumber))

}
