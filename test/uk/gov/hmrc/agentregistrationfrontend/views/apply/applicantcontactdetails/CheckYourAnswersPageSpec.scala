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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.applicantcontactdetails

import com.google.inject.AbstractModule
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.CheckYourAnswersPage

class CheckYourAnswersPageSpec
extends ViewSpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit = bind(classOf[AmlsCodes]).asEagerSingleton()

  val viewTemplate: CheckYourAnswersPage = app.injector.instanceOf[CheckYourAnswersPage]

  private val tdAll: TdAll = TdAll()

  object agentApplication:
    val complete: AgentApplication =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .afterEmailAddressVerified

  private val heading: String = "Check your answers"

  "CheckYourAnswersPage for complete Applicant Contact Details" should:
    given agentApplicationHmrcRequest: RequestWithData[DataWithApplication] = tdAll.makeAgentApplicationRequest(agentApplication.complete)

    val doc: Document = Jsoup.parse(viewTemplate(agentApplicationHmrcRequest.agentApplication).body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Applicant contact details
          |Check your answers
          |Name
          |Miss Alexa Fantastic
          |Change Name
          |Telephone number
          |(+44) 10794554342
          |Change Telephone number
          |Email address
          |user@test.com
          """.stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a summary row for each required answer" in:
      val expectedSummaryList: TestSummaryList = TestSummaryList(
        List(
          TestSummaryRow(
            key = "Name",
            value = "Miss Alexa Fantastic",
            action = AppRoutes.apply.applicantcontactdetails.ApplicantNameController.show.url,
            changeLinkAccessibleContent = "Change Name"
          ),
          TestSummaryRow(
            key = "Telephone number",
            value = "(+44) 10794554342",
            action = AppRoutes.apply.applicantcontactdetails.TelephoneNumberController.show.url,
            changeLinkAccessibleContent = "Change Telephone number"
          ),
          TestSummaryRow(
            key = "Email address",
            value = "user@test.com",
            action = AppRoutes.apply.applicantcontactdetails.EmailAddressController.show.url,
            changeLinkAccessibleContent = "Change Email address"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"
