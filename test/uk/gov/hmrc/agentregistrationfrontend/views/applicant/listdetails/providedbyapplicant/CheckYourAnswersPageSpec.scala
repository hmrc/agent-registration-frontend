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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.providedbyapplicant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  val individualName: IndividualName = tdAll.providedDetails.afterAccessConfirmed.individualName
  val individualNameValue: String = individualName.value

  object providedByApplicant:

    val complete: ProvidedByApplicant = ProvidedByApplicant(
      individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
      individualName = individualName,
      individualDateOfBirth = Some(tdAll.dateOfBirthProvided),
      telephoneNumber = Some(tdAll.telephoneNumber),
      emailAddress = Some(tdAll.individualEmailAddress),
      individualNino = Some(tdAll.ninoProvided),
      individualSaUtr = Some(tdAll.saUtrProvided)
    )
    val completeWithNoNinoOrSaUtr: ProvidedByApplicant = complete.copy(
      individualNino = Some(IndividualNino.NotProvided),
      individualSaUtr = Some(IndividualSaUtr.NotProvided)
    )

  private val heading: String = "Check your answers"
  private val serviceTitleSuffix: String = "Apply for an agent services account - GOV.UK"
  private val confirmAndContinueText: String = "Confirm and continue"

  private def pageTitle: String = s"$heading - $serviceTitleSuffix"

  private def renderDoc(providedByApplicantDetails: ProvidedByApplicant): Document = Jsoup.parse(
    viewTemplate(providedByApplicantDetails).body
  )

  private def summaryRow(
    key: String,
    value: String,
    action: String
  ): TestSummaryRow = TestSummaryRow(
    key = key,
    value = value,
    action = action,
    changeLinkAccessibleContent = s"Change $key"
  )

  "CheckYourAnswersPage for complete provided by applicant details" should:

    val doc: Document = renderDoc(providedByApplicant.complete)

    "contain correct content" in:
      doc.mainContent shouldContainContent
        s"""
           |Check your answers
           |Telephone number
           |(+44) 10794554342
           |Change Telephone number
           |Email address
           |member@test.com
           |Change Email address
           |Date of birth
           |1 January 2000
           |Change Date of birth
           |Do you know $individualNameValue’s National Insurance number?
           |Yes
           |Change Do you know $individualNameValue’s National Insurance number?
           |National Insurance number
           |AB123456C
           |Change National Insurance number
           |Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?
           |Yes
           |Change Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?
           |Self Assessment Unique Taxpayer Reference
           |1234567895
           |Change Self Assessment Unique Taxpayer Reference
           |Confirm and continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
              """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe pageTitle

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          summaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show.url
          ),
          summaryRow(
            key = "Email address",
            value = "member@test.com",
            action = AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
          ),
          summaryRow(
            key = "Date of birth",
            value = "1 January 2000",
            action = AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url
          ),
          summaryRow(
            key = s"Do you know $individualNameValue’s National Insurance number?",
            value = "Yes",
            action = AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show.url
          ),
          summaryRow(
            key = "National Insurance number",
            value = "AB123456C",
            action = AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show.url
          ),
          summaryRow(
            key = s"Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?",
            value = "Yes",
            action = AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show.url
          ),
          summaryRow(
            key = "Self Assessment Unique Taxpayer Reference",
            value = "1234567895",
            action = AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show.url
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe confirmAndContinueText

  "CheckYourAnswersPage for complete details with no Nino or SaUtr rows" should:

    val doc: Document = renderDoc(providedByApplicant.completeWithNoNinoOrSaUtr)

    "contain content" in:
      doc.mainContent shouldContainContent
        s"""
           |Check your answers
           |Telephone number
           |(+44) 10794554342
           |Change Telephone number
           |Email address
           |member@test.com
           |Change Email address
           |Date of birth
           |1 January 2000
           |Change Date of birth
           |Do you know $individualNameValue’s National Insurance number?
           |No
           |Change Do you know $individualNameValue’s National Insurance number?
           |Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?
           |No
           |Change Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?
           |Confirm and continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
              """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe pageTitle

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          summaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show.url
          ),
          summaryRow(
            key = "Email address",
            value = "member@test.com",
            action = AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
          ),
          summaryRow(
            key = "Date of birth",
            value = "1 January 2000",
            action = AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url
          ),
          summaryRow(
            key = s"Do you know $individualNameValue’s National Insurance number?",
            value = "No",
            action = AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show.url
          ),
          summaryRow(
            key = s"Do you know $individualNameValue’s Self Assessment Unique Taxpayer Reference?",
            value = "No",
            action = AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show.url
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe confirmAndContinueText
