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
import com.softwaremill.quicklens.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.EmailAddress
import uk.gov.hmrc.agentregistration.shared.TelephoneNumber
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantEmailAddress
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
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

  private val completeApplication: AgentApplication = tdAll.llpAgentApplication
    .modify(_.applicantContactDetails)
    .setTo(Some(
      ApplicantContactDetails(
        applicantName = ApplicantName.NameOfAuthorised(name = Some("First Last")),
        telephoneNumber = Some(TelephoneNumber(tdAll.telephoneNumber)),
        applicantEmailAddress = Some(ApplicantEmailAddress(
          emailAddress = EmailAddress(tdAll.applicantEmailAddress),
          isVerified = true
        ))
      )
    ))

  private val heading: String = "Check your answers"

  "CheckYourAnswersPage for complete Applicant Contact Details" should:
    implicit val agentApplicationHmrcRequest: AgentApplicationRequest[AnyContent] =
      new AgentApplicationRequest(
        request = request,
        agentApplication = completeApplication,
        internalUserId = tdAll.internalUserId,
        groupId = tdAll.groupId,
        credentials = tdAll.credentials
      )

    val doc: Document = Jsoup.parse(viewTemplate().body)
    "contain content" in:
      doc.mainContent shouldContainContent
        """
          |Applicant contact details
          |Check your answers
          |Member of the limited liability partnership
          |No, but I’m authorised by them to set up this account
          |Change Member of the limited liability partnership
          |Name
          |First Last
          |Change Name
          |Telephone number
          |041 427 1125
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
            key = "Member of the limited liability partnership",
            value = "No, but I’m authorised by them to set up this account",
            action = "/agent-registration/apply/applicant/llp-member"
          ),
          TestSummaryRow(
            key = "Name",
            value = "First Last",
            action = "/agent-registration/apply/applicant/applicant-name"
          ),
          TestSummaryRow(
            key = "Telephone number",
            value = "041 427 1125",
            action = "/agent-registration/apply/applicant/telephone-number"
          ),
          TestSummaryRow(
            key = "Email address",
            value = "user@test.com",
            action = "/agent-registration/apply/applicant/email-address"
          )
        )
      )
      doc.mainContent.extractSummaryList() shouldBe expectedSummaryList

    "render a confirm and continue button" in:
      doc.extractLinkButton(1).text shouldBe "Confirm and continue"
