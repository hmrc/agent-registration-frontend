/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.data.Form
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.forms.SelectIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.SelectIndividualPage

class SelectIndividualPageSpec
extends ViewSpec:

  val viewTemplate: SelectIndividualPage = app.injector.instanceOf[SelectIndividualPage]

  val incompleteIndividuals: List[IndividualProvidedDetails] = List(
    tdAll.providedDetails.afterAccessConfirmed
  )

  def testOptions: Map[String, String] = Map(
    tdAll.providedDetails.afterAccessConfirmed._id.value -> tdAll.providedDetails.afterAccessConfirmed.individualName.value
  )

  val form: Form[IndividualProvidedDetails] = SelectIndividualForm.form(incompleteIndividuals)

  val doc: Document = Jsoup.parse(
    viewTemplate(
      form = form,
      incompleteIndividuals = incompleteIndividuals
    ).body
  )

  private val heading: String = "Which relevant individual do you need to tell us about?"

  "SelectIndividualPage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Relevant individual details
          |Which relevant individual do you need to tell us about?
          |Start to enter their name and choose it from the list.
          |Test Name
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have a select element" in:
      doc.select("select").size() shouldBe 1

    "render a select element with individual options" in:
      val expectedElement = TestSelect(
        "selectIndividual",
        Seq(("", "")) ++ testOptions.toSeq
      )
      doc
        .mainContent
        .selectOrFail("select")
        .selectOnlyOneElementOrFail()
        .toSelect shouldBe expectedElement

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

    "render an error message when form has errors" in:
      val field = "selectIndividual"
      val errorMessage = "Select which relevant individual you want to provide details for"
      val formWithError = form.withError(field, errorMessage)
      val errorDoc: Document = Jsoup.parse(viewTemplate(formWithError, incompleteIndividuals).body)
      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |Select which relevant individual you want to provide details for
          |Relevant individual details
          |Which relevant individual do you need to tell us about?
          |Start to enter their name and choose it from the list.
          |Error:
          |Select which relevant individual you want to provide details for
          |Test Name
          |Save and continue
          |Save and come back later
          |Is this page not working properly? (opens in new tab)
          |""".stripMargin

      behavesLikePageWithErrorHandling(
        field = field,
        errorMessage = errorMessage,
        errorDoc = errorDoc,
        heading = heading
      )
