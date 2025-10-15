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
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantContactDetails
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseDateOfBirth
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails.routes
import uk.gov.hmrc.agentregistrationfrontend.forms.ChOfficerSelectionForms
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MatchedMembersPage

class MatchedMembersPageSpec
extends ViewSpec:

  val viewTemplate: MatchedMembersPage = app.injector.instanceOf[MatchedMembersPage]
  implicit val agentApplicationRequest: AgentApplicationRequest[AnyContent] =
    new AgentApplicationRequest(
      request = request,
      agentApplication = tdAll.agentApplicationAfterCreated
        .modify(_.businessDetails)
        .setTo(Some(tdAll.llpBusinessDetails))
        .modify(_.applicantContactDetails)
        .setTo(Some(ApplicantContactDetails(
          applicantName = ApplicantName.NameOfMember(
            memberNameQuery = Some(CompaniesHouseNameQuery(
              firstName = "First",
              lastName = "Last"
            ))
          )
        ))),
      internalUserId = tdAll.internalUserId,
      groupId = tdAll.groupId
    )
  private val singleHeading: String = "Are these your details?"
  private val multipleHeading: String = "2 records match this name"

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

  private val multipleOfficers = Seq(
    CompaniesHouseOfficer(
      name = "First Last",
      dateOfBirth = Some(CompaniesHouseDateOfBirth(
        day = None,
        month = 1,
        year = 1990
      ))
    ),
    CompaniesHouseOfficer(
      name = "First Alt Last",
      dateOfBirth = Some(CompaniesHouseDateOfBirth(
        day = None,
        month = 4,
        year = 1980
      ))
    )
  )

  "MatchedMembersPage with a single match" should:
    val doc: Document = Jsoup.parse(viewTemplate(ChOfficerSelectionForms.officerSelectionForm(singleOfficer), singleOfficer).body)
    "have the correct title for a single match" in:
      doc.title() shouldBe s"$singleHeading - Apply for an agent services account - GOV.UK"

    "render a form with yes/no radios for a single match" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe routes.CompaniesHouseMatchingController.submit.url
      form
        .selectOrFail("label[for=companiesHouseOfficer]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Yes"
      form
        .selectOrFail(s"input[name=${ChOfficerSelectionForms.key}][type=radio][value='${singleOfficer.head.toString}']")
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
      val formWithError = ChOfficerSelectionForms.officerSelectionForm(singleOfficer)
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, singleOfficer).body),
        heading = singleHeading
      )

  "MatchedMembersPage with multiple matches" should:
    val doc: Document = Jsoup.parse(viewTemplate(ChOfficerSelectionForms.officerSelectionForm(multipleOfficers), multipleOfficers).body)
    "have the correct title for multiple matches" in:
      doc.title() shouldBe s"$multipleHeading - Apply for an agent services account - GOV.UK"

    "render a form with radios for each of the multiple matches" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe routes.CompaniesHouseMatchingController.submit.url
      form
        .selectOrFail("label[for=companiesHouseOfficer]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "First Last Date of birth: January 1990"
      form
        .selectOrFail(s"input[name=${ChOfficerSelectionForms.key}][type=radio][value='${multipleOfficers.head.toString}']")
        .selectOnlyOneElementOrFail()
      form
        .selectOrFail(s"label[for=${ChOfficerSelectionForms.key}-2]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "First Alt Last Date of birth: April 1980"
      form
        .selectOrFail(s"input[name=${ChOfficerSelectionForms.key}][type=radio][value='${multipleOfficers.last.toString}']")
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
      val formWithError = ChOfficerSelectionForms.officerSelectionForm(singleOfficer)
        .withError(field, errorMessage)
      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(formWithError, singleOfficer).body),
        heading = singleHeading
      )
