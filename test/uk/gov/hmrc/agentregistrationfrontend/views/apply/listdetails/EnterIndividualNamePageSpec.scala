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

package uk.gov.hmrc.agentregistrationfrontend.views.apply.listdetails

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.action.Requests.RequestWithData4
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.EnterIndividualNamePage

class EnterIndividualNamePageSpec
extends ViewSpec:

  private val firstPartnerKey = "first"
  private val nextPartnerKey = "subsequent"
  private val onlyPartnerKey = "only"

  object ExpectedStrings:

    val firstPartnerHeading = "What is the full name of the first partner?"
    val nextPartnerHeading = "What is the full name of the next partner?"
    val onlyPartnerHeading = "What is the full name of the partner?"

  val viewTemplate: EnterIndividualNamePage = app.injector.instanceOf[EnterIndividualNamePage]
  implicit val agentApplicationRequest: RequestWithData4[DataWithApplication] = tdAll.makeAgentApplicationRequest(
    agentApplication =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals
  )

  List(
    firstPartnerKey,
    nextPartnerKey,
    onlyPartnerKey
  ).foreach { ordinalKey =>
    s"EnterIndividualNamePage for ordinal key '$ordinalKey'" should {
      val doc: Document = Jsoup.parse(viewTemplate(
        form = IndividualNameForm.form,
        ordinalKey = ordinalKey
      ).body)

      val expectedHeading =
        ordinalKey match
          case `firstPartnerKey` => ExpectedStrings.firstPartnerHeading
          case `nextPartnerKey` => ExpectedStrings.nextPartnerHeading
          case `onlyPartnerKey` => ExpectedStrings.onlyPartnerHeading

      s"have the correct title for '$ordinalKey'" in:
        doc.title() shouldBe s"$expectedHeading - Apply for an agent services account - GOV.UK"

      s"render a save and continue button for '$ordinalKey'" in:
        doc
          .mainContent
          .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
          .selectOnlyOneElementOrFail()
          .text() shouldBe "Save and continue"

      s"render a save and come back later button for '$ordinalKey'" in:
        doc
          .mainContent
          .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
          .selectOnlyOneElementOrFail()
          .text() shouldBe "Save and come back later"

      s"render a form error when the form contains an error for '$ordinalKey'" in:
        val field = IndividualNameForm.key
        val errorMessage = "Enter the full name of the partner"
        val formWithError = IndividualNameForm.form
          .withError(field, errorMessage)
        behavesLikePageWithErrorHandling(
          field = field,
          errorMessage = errorMessage,
          errorDoc = Jsoup.parse(viewTemplate(
            form = formWithError,
            ordinalKey = ordinalKey
          ).body),
          heading = expectedHeading
        )
    }
  }
