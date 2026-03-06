/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.incorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.RemoveKeyIndividualPage

class RemoveKeyIndividualPageSpec
extends ViewSpec:

  val viewTemplate: RemoveKeyIndividualPage = app.injector.instanceOf[RemoveKeyIndividualPage]

  private val agentApplication: AgentApplication = tdAll.agentApplicationLlp.afterNumberOfConfirmCompaniesHouseOfficers

  private val individualProvidedDetails = tdAll.individualProvidedDetails
  private val individualName: String = individualProvidedDetails.individualName.value

  private val key: String = RemoveKeyIndividualForm.key
  private val caption: String = "LLP members and other tax adviser information"
  private val heading: String = s"Confirm that you want to remove $individualName from the list of partners"

  private def render(form: play.api.data.Form[YesNo]): Document = Jsoup.parse(viewTemplate(
    form = form,
    individualProvidedDetails = individualProvidedDetails,
    agentApplication = agentApplication
  ).body)

  "RemoveKeyIndividualPage" should:

    val doc: Document = render(RemoveKeyIndividualForm.form(individualName))

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |$caption
           |$heading
           |Yes
           |No
           |Save and continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
           |""".stripMargin
      )

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with correct action" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe AppRoutes.apply.listdetails.incoporated.RemoveCompaniesHouseOfficerController.submit(individualProvidedDetails._id).url

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
      val field = key
      val errorMessage = "Select yes if you want to remove"
      val formWithError = RemoveKeyIndividualForm
        .form(individualName)
        .withError(field, errorMessage)

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = render(formWithError),
        heading = heading
      )
