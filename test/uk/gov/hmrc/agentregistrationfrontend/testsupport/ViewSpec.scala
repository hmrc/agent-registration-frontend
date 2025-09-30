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

package uk.gov.hmrc.agentregistrationfrontend.testsupport

import org.jsoup.nodes.Document
import org.scalatest.Assertion
import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.viewspecsupport.JsoupSupport

trait ViewSpec
extends ISpec:

  export JsoupSupport.*

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(request)

  /* Ensures that a page with form errors renders accessibly, with the expected
   * error prefix appended, summary list and links rendered correctly and in inline error message
   * rendered correctly.
   */
  def behavesLikePageWithErrorHandling(
    field: String,
    errorMessage: String,
    errorDoc: Document,
    heading: String,
    isWholeDateError: Boolean = false
  ): Assertion =
    val summaryLink = errorDoc.selectOrFail(errorSummaryLink).selectOnlyOneElementOrFail()
    val inlineError = errorDoc.selectOrFail(inlineErrorMessage).selectOnlyOneElementOrFail()
    val expectedSummaryLinkHref = if isWholeDateError then s"#$field.day" else s"#$field"
    errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
    errorDoc.selectOrFail(".govuk-error-summary__title").selectOnlyOneElementOrFail().text() shouldBe "There is a problem"
    summaryLink.text() shouldBe errorMessage
    summaryLink.selectAttrOrFail("href") shouldBe s"$expectedSummaryLinkHref"
    inlineError.text() shouldBe s"Error: $errorMessage"
