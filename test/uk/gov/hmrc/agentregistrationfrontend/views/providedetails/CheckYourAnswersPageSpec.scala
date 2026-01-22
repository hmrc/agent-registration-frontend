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

package uk.gov.hmrc.agentregistrationfrontend.views.providedetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.llp.IndividualSaUtr
import uk.gov.hmrc.agentregistration.shared.llp.IndividualNino
import uk.gov.hmrc.agentregistrationfrontend.individual.action.IndividualProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.individualconfirmation.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private object individualProvideDetails:

    val complete = tdAll.providedDetailsLlp.afterApproveAgentApplication
    val completeWithNinoAndSaUtrNotProvided = tdAll.providedDetailsLlp.afterApproveAgentApplication
      .copy(individualNino = Some(IndividualNino.NotProvided), individualSaUtr = Some(IndividualSaUtr.NotProvided))
    val completeWithNinoAndSaUtrFromHmrc = tdAll.providedDetailsLlp.afterApproveAgentApplication
      .copy(individualNino = Some(tdAll.ninoFromAuth), individualSaUtr = Some(tdAll.saUtrFromAuth))

  private val heading: String = "Check your answers"
  private val serviceTitleSuffix: String = "Apply for an agent services account - GOV.UK"
  private val confirmAndContinueText: String = "Confirm and continue"

  private def pageTitle: String = s"$heading - $serviceTitleSuffix"

  private def renderDoc()(using IndividualProvideDetailsRequest[AnyContent]): Document = Jsoup.parse(viewTemplate().body)

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

  "CheckYourAnswersPage for complete Individual Provided Details" should:
    given individualProvideDetailsRequest: IndividualProvideDetailsRequest[AnyContent] = tdAll.makeProvideDetailsRequest(individualProvideDetails.complete)

    val doc: Document = renderDoc()

    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |LLP member confirmation
          |Check your answers
          |Name
          |Taylor Leadenhall-Lane
          |Change Name
          |Telephone number
          |(+44) 10794554342
          |Change Telephone number
          |Email address
          |member@test.com
          |Change Email address
          |Do you have a National Insurance number?
          |Yes
          |Change Do you have a National Insurance number?
          |National Insurance number
          |AB123456C
          |Change National Insurance number
          |Do you have a Self Assessment Unique Taxpayer Reference?
          |Yes
          |Change Do you have a Self Assessment Unique Taxpayer Reference?
          |Self Assessment Unique Taxpayer Reference
          |1234567895
          |Change Self Assessment Unique Taxpayer Reference
          |Confirm and continue
          |Is this page not working properly? (opens in new tab)
              """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe pageTitle

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          summaryRow(
            key = "Name",
            value = "Taylor Leadenhall-Lane",
            action = AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url
          ),
          summaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.providedetails.IndividualTelephoneNumberController.show.url
          ),
          summaryRow(
            key = "Email address",
            value = "member@test.com",
            action = AppRoutes.providedetails.IndividualEmailAddressController.show.url
          ),
          summaryRow(
            key = "Do you have a National Insurance number?",
            value = "Yes",
            action = AppRoutes.providedetails.IndividualNinoController.show.url
          ),
          summaryRow(
            key = "National Insurance number",
            value = "AB123456C",
            action = AppRoutes.providedetails.IndividualNinoController.show.url
          ),
          summaryRow(
            key = "Do you have a Self Assessment Unique Taxpayer Reference?",
            value = "Yes",
            action = AppRoutes.providedetails.IndividualSaUtrController.show.url
          ),
          summaryRow(
            key = "Self Assessment Unique Taxpayer Reference",
            value = "1234567895",
            action = AppRoutes.providedetails.IndividualSaUtrController.show.url
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe confirmAndContinueText

  "CheckYourAnswersPage for incomplete individual Provided Details - when Nino and SaUtr coming from HMRC systems" should:
    given individualProvideDetailsRequest: IndividualProvideDetailsRequest[AnyContent] = tdAll.makeProvideDetailsRequest(
      individualProvideDetails.completeWithNinoAndSaUtrFromHmrc
    )

    val doc: Document = renderDoc()

    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |LLP member confirmation
          |Check your answers
          |Name
          |Taylor Leadenhall-Lane
          |Change Name
          |Telephone number
          |(+44) 10794554342
          |Change Telephone number
          |Email address
          |member@test.com
          |Change Email address
          |Confirm and continue
          |Is this page not working properly? (opens in new tab)
              """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe pageTitle

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          summaryRow(
            key = "Name",
            value = "Taylor Leadenhall-Lane",
            action = AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url
          ),
          summaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.providedetails.IndividualTelephoneNumberController.show.url
          ),
          summaryRow(
            key = "Email address",
            value = "member@test.com",
            action = AppRoutes.providedetails.IndividualEmailAddressController.show.url
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe confirmAndContinueText

  "CheckYourAnswersPage for incomplete individual Provided Details - when Nino and SaUtr not provided" should:
    given individualProvideDetailsRequest: IndividualProvideDetailsRequest[AnyContent] = tdAll.makeProvideDetailsRequest(
      individualProvideDetails.completeWithNinoAndSaUtrNotProvided
    )

    val doc: Document = renderDoc()

    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |LLP member confirmation
          |Check your answers
          |Name
          |Taylor Leadenhall-Lane
          |Change Name
          |Telephone number
          |(+44) 10794554342
          |Change Telephone number
          |Email address
          |member@test.com
          |Change Email address
          |Do you have a National Insurance number?
          |No
          |Change Do you have a National Insurance number?
          |Do you have a Self Assessment Unique Taxpayer Reference?
          |No
          |Change Do you have a Self Assessment Unique Taxpayer Reference?
          |Confirm and continue
          |Is this page not working properly? (opens in new tab)
              """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe pageTitle

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          summaryRow(
            key = "Name",
            value = "Taylor Leadenhall-Lane",
            action = AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url
          ),
          summaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.providedetails.IndividualTelephoneNumberController.show.url
          ),
          summaryRow(
            key = "Email address",
            value = "member@test.com",
            action = AppRoutes.providedetails.IndividualEmailAddressController.show.url
          ),
          summaryRow(
            key = "Do you have a National Insurance number?",
            value = "No",
            action = AppRoutes.providedetails.IndividualNinoController.show.url
          ),
          summaryRow(
            key = "Do you have a Self Assessment Unique Taxpayer Reference?",
            value = "No",
            action = AppRoutes.providedetails.IndividualSaUtrController.show.url
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractSubmitButtonText shouldBe confirmAndContinueText
