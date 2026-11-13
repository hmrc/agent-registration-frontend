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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.nonincorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import uk.gov.hmrc.agentregistrationfrontend.forms.NumberOfPartnersResponsibleForTaxForm
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndContinue
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxPage

class NumberOfPartnersResponsibleForTaxPageSpec
extends ViewSpec:

  val viewTemplate: NumberOfPartnersResponsibleForTaxPage = app.injector.instanceOf[NumberOfPartnersResponsibleForTaxPage]
  private val agencyName = tdAll.TdAgencyName.generalPartnership

  val doc: Document = Jsoup.parse(viewTemplate(
    form = NumberOfPartnersResponsibleForTaxForm.form,
    entityName = agencyName
  ).body)

  private val heading: String = "Partners who are also ‘relevant individuals’"

  "NumberOfPartnersResponsibleForTaxPage" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        s"""
           |Partners and other relevant individuals
           |Partners who are also ‘relevant individuals’
           |We need to know which of the partners at $agencyName meet the legal definition of relevant individuals, as defined in Chapter 226(2) of the Finance Act 2026 (opens in a new tab).
           |The definition of relevant individuals
           |The definition of a ‘relevant individual’ depends on the size of the business.
           |For a business with 6 or more partners, someone is a relevant individual if they work for the business and play a significant role in:
           |making decisions about how the business manages or organises all of its tax adviser activities, or a substantial part of those activities
           |actually managing or organising all of the tax adviser activities for the business, or a substantial part of those activities
           |How many partners are also relevant individuals?
           |Save and continue
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
           |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "render a form with an input for capturing the number of partners" in:
      val form = doc.mainContent.selectOrFail("form").selectOnlyOneElementOrFail()
      form.attr("action") shouldBe AppRoutes.apply.listdetails.nonincorporated.NumberOfPartnersResponsibleForTaxController.submit.url
      form.selectOrFail("input[name='numberOfPartnersResponsibleForTax']").selectOnlyOneElementOrFail()

    "render a save and continue button within the form" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value='${SaveAndContinue.toString}']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render a save and come back later button within the form" in:
      doc
        .mainContent
        .selectOrFail(s"form button[value=${SaveAndComeBackLater.toString}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "render a form error when the form contains an error" in:
      val errorMessage: String = "Enter how many partners are also relevant individuals"
      val formWithError: Form[Int] = NumberOfPartnersResponsibleForTaxForm
        .form
        .bind(Map(
          NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax -> ""
        ))
      behavesLikePageWithErrorHandling(
        field = NumberOfPartnersResponsibleForTaxForm.numberOfPartnersResponsibleForTax,
        errorMessage = errorMessage,
        errorDoc = Jsoup.parse(viewTemplate(
          form = formWithError,
          entityName = agencyName
        ).body),
        heading = heading
      )
