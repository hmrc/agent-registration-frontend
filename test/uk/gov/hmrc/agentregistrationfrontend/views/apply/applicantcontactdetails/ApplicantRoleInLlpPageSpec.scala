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

import com.softwaremill.quicklens.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.forms.ApplicantRoleInLlpForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.ApplicantRoleInLlpPage

class ApplicantRoleInLlpPageSpec
extends ViewSpec:

  val viewTemplate: ApplicantRoleInLlpPage = app.injector.instanceOf[ApplicantRoleInLlpPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] =
    new AgentApplicationRequest(
      request = request.withSession("agentType" -> "UkTaxAgent", "businessType" -> "LimitedLiabilityPartnership"),
      agentApplication = tdAll.agentApplicationAfterCreated
        .modify(_.businessDetails)
        .setTo(Some(tdAll.llpBusinessDetails)),
      internalUserId = tdAll.internalUserId,
      groupId = tdAll.groupId,
      credentials = tdAll.credentials
    )
  val doc: Document = Jsoup.parse(viewTemplate(ApplicantRoleInLlpForm.form).body)
  private val heading: String = "Are you a member of the limited liability partnership?"

  "ApplicantRoleInLlpPage" should:

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Yes" -> "Member",
          "No, but I’m authorised by them to set up this account" -> "Authorised"
        ),
        hint = Some("Being a member means you are listed in Companies House as a current officer of Test Company Name.")
      )
      doc.mainContent.extractRadioGroup() shouldBe expectedRadioGroup

    "render a save and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render a save and come back later button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "render a form error when the form contains an error" in:
      val field = ApplicantRoleInLlpForm.key
      val errorMessage = "Select ‘yes’ if you are the owner of the business"
      val formWithError = ApplicantRoleInLlpForm.form
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError).body),
        heading = heading
      )
