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

package uk.gov.hmrc.agentregistrationfrontend.views.register

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.forms.UserRoleForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.UserRolePage

class UserRolePageSpec
extends ViewSpec:

  "UserRolePage" should:
    val heading: String = "Are you the owner of the business?"
    val viewTemplate: UserRolePage = app.injector.instanceOf[UserRolePage]
    val doc: Document = Jsoup.parse(viewTemplate(UserRoleForm.form).body)

    "contein expected content" in:
      doc.mainContent shouldContainContent
        """
          |About your application
          |Are you the owner of the business?
          |Yes
          |No, but I’m authorised by them to set up this account
          |Save and continue
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "Are you the owner of the business?"

    "render a radio button for each option" in:
      val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
        legend = heading,
        options = List(
          "Yes" -> "Owner",
          "No, but I’m authorised by them to set up this account" -> "Authorised"
        ),
        hint = None
      )

      doc
        .mainContent
        .extractRadioGroup() shouldBe expectedRadioGroup

    "render a save and continue button" in:
      doc.extractSubmitButtonText shouldBe "Save and continue"
//      doc.select("button[type=submit]").text() shouldBe "Save and continue"

    "render a form error when the form contains an error" in:
      // TODO: this tests doesn't test that correct form with errors is passed to the view.
      //  Also it doesn't verify if error summary list leads to the input field

      val viewTemplate: UserRolePage = app.injector.instanceOf[UserRolePage]
      val heading: String = "Are you the owner of the business?"

      val field = "userRole"
      val errorMessage = "Select ‘yes’ if you are the owner of the business"
      val formWithError = UserRoleForm.form
        .withError(field, errorMessage)
      val doc: Document = Jsoup.parse(viewTemplate(formWithError).body)

      doc.mainContent shouldContainContent
        """
          |There is a problem
          |Select ‘yes’ if you are the owner of the business
          |About your application
          |Are you the owner of the business?
          |Error:
          |Select ‘yes’ if you are the owner of the business
          |Yes
          |No, but I’m authorised by them to set up this account
          |Save and continue
          |""".stripMargin

      doc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"

      // TODO discuss what are those and if we could make them available in the ViewSelectors
      doc
        .mainContent
        .selectOrFail(".govuk-error-summary__title")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "There is a problem"

      doc
        .selectOrFail(".govuk-error-summary__list > li > a")
        .selectOnlyOneElementOrFail()
        .selectAttrOrFail("href") shouldBe s"#$field"

      doc
        .selectOrFail(".govuk-error-message")
        .selectOnlyOneElementOrFail()
        .text() shouldBe s"Error: $errorMessage"
