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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContent
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails.routes as applicantcontactdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MatchedMemberPage

class MatchedMemberPageSpec
extends ViewSpec:

  val viewTemplate: MatchedMemberPage = app.injector.instanceOf[MatchedMemberPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationLlp
        .sectionContactDetails
        .whenApplicantIsAMember
        .afterNameQueryProvided
  )
  private val heading: String = "Are these your details?"

  private val singleOfficer = Seq(
    CompaniesHouseOfficer(
      name = "First Last",
      dateOfBirth = Some(CompaniesHouseDateOfBirth(
        day = None,
        month = 1,
        year = 1990
      ))
    )
  )

  "MatchedMembersPage with a single match" should:
    val doc: Document = Jsoup.parse(viewTemplate(ChOfficerSelectionForms.yesNoForm, singleOfficer.head).body)
    "have the correct title for a single match" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with yes/no radios for a single match" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe applicantcontactdetailsRoutes.CompaniesHouseMatchingController.submit.url
      form
        .selectOrFail(s"label[for=${ChOfficerSelectionForms.key}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Yes"
      form
        .selectOrFail(s"input[name=${ChOfficerSelectionForms.key}][type=radio][value='Yes']")
        .selectOnlyOneElementOrFail()
      form
        .selectOrFail(s"label[for=${ChOfficerSelectionForms.key}-2]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "No"
      form
        .selectOrFail(s"input[name=${ChOfficerSelectionForms.key}][type=radio][value='No']")
        .selectOnlyOneElementOrFail()

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

    "render a form error when passed in" in:
      val field = ChOfficerSelectionForms.key
      val errorMessage = "Select yes if these are your details"
      val formWithError = ChOfficerSelectionForms.yesNoForm
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, singleOfficer.head).body),
        heading = heading
      )
