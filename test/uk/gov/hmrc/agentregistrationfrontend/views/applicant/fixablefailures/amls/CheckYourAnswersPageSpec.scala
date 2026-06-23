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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.fixablefailures.amls

import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val heading: String = "Check your answers"

  "CheckYourAnswersPage for complete fixable Hmrc Amls Details" should:

    val doc: Document = Jsoup.parse(viewTemplate(tdAll.completeAmlsDetails).body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Check your answers
          |Supervisory body
          |HM Revenue and Customs (HMRC)
          |Change Supervisory body
          |Registration number
          |XAML00000123456
          |Change Registration number
          |Confirm and continue
          |Save and come back later
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Supervisory body",
            value = "HM Revenue and Customs (HMRC)",
            action = AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show.url,
            changeLinkAccessibleContent = "Change Supervisory body"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "XAML00000123456",
            action = AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show.url,
            changeLinkAccessibleContent = "Change Registration number"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a form to submit confirmation of fixed Amls details" in:
      val form = doc.select("form")
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.submit.url
      form.select("button[type=submit]").text() shouldBe "Confirm and continue"

  "CheckYourAnswersPage for complete non-Hmrc Amls Details" should:

    val doc: Document = Jsoup.parse(viewTemplate(tdAll.completeAmlsDetailsAtt).body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Check your answers
          |Supervisory body
          |Association of TaxationTechnicians (ATT)
          |Change Supervisory body
          |Registration number
          |ATT AML-1-123456
          |Change Registration number
          |Evidence
          |certificate.pdf
          |Change Evidence
          |Confirm and continue
          |Save and come back later
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Supervisory body",
            value = "Association of TaxationTechnicians (ATT)",
            action = AppRoutes.fixablefailures.amlsfailure.AmlsSupervisorController.show.url,
            changeLinkAccessibleContent = "Change Supervisory body"
          ),
          TestSummaryRow(
            key = "Registration number",
            value = "ATT AML-1-123456",
            action = AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show.url,
            changeLinkAccessibleContent = "Change Registration number"
          ),
          TestSummaryRow(
            key = "Evidence",
            value = "certificate.pdf",
            action = AppRoutes.fixablefailures.amlsfailure.AmlsEvidenceUploadController.show.url,
            changeLinkAccessibleContent = "Change Evidence"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a form to submit confirmation of fixed Amls details" in:
      val form = doc.select("form")
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.submit.url
      form.select("button[type=submit]").text() shouldBe "Confirm and continue"
