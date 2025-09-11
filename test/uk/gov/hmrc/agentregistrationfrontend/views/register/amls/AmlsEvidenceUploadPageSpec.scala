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

package uk.gov.hmrc.agentregistrationfrontend.views.register.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.register.amls.AmlsEvidenceUploadPage
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UpscanInitiateResponse
import uk.gov.hmrc.agentregistration.shared.upscan.Reference

class AmlsEvidenceUploadPageSpec
extends ViewSpec:

  val viewTemplate: AmlsEvidenceUploadPage = app.injector.instanceOf[AmlsEvidenceUploadPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      upscanInitiateResponse = UpscanInitiateResponse(
        fileReference = Reference("reference"),
        postTarget = "https://bucketName.s3.eu-west-2.amazonaws.com/upload",
        formFields = Map("hiddenKey" -> "hiddenValue")
      ),
      errorMessage = None,
      supervisoryBodyName = "Gambling Commission"
    ).body
  )

  private val heading: String = "Evidence of your anti-money laundering supervision"

  "AmlsEvidenceUploadPage view" should:

    "contain expected content" in:
      doc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Evidence of your anti-money laundering supervision
          |Upload evidence to show that Gambling Commission is your current anti-money laundering supervisor.
          |You can choose what evidence to upload.
          |Suitable evidence might be a letter, email or payment receipt from your supervisory body, confirming you’re covered.
          |The file must be smaller than 5MB.
          |Types of file we can accept
          |These file types are allowed:
          |image (.jpg, .jpeg, .png or .tiff)
          |PDF (.pdf)
          |email (.txt or .msg)
          |Microsoft (Word, Excel or PowerPoint)
          |Open Document Format (ODF)
          |Choose your file
          |Save and continue
          |""".stripMargin

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe heading

    "have only one file input element in the form" in:
      doc
        .mainContent
        .selectOrFail("form input[type='file']")
        .selectOnlyOneElementOrFail()

    "have hidden fields for upscan initiate form fields" in:
      val hiddenFields = doc
        .mainContent
        .selectOrFail("form input[type='hidden']")
      hiddenFields.size() shouldBe 1
      hiddenFields.eachAttr("name") should contain("hiddenKey")
      hiddenFields.eachAttr("value") should contain("hiddenValue")

    "have the form with the correct action" in:
      val form = doc
        .mainContent
        .selectOrFail("form")
        .selectOnlyOneElementOrFail()
      form.attr("action") shouldBe "https://bucketName.s3.eu-west-2.amazonaws.com/upload"
      form.attr("method") shouldBe "post"
      form.attr("enctype") shouldBe "multipart/form-data"

    "render a save and continue button" in:
      doc
        .mainContent
        .selectOrFail(s"form button[id='upload-button']")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and continue"

    "render an error message when error passed in" in:
      val errorMessage: String = "The file you uploaded was infected with a virus"
      val errorDoc: Document = Jsoup.parse(
        viewTemplate(
          upscanInitiateResponse = UpscanInitiateResponse(
            fileReference = Reference("reference"),
            postTarget = "https://bucketName.s3.eu-west-2.amazonaws.com/upload",
            formFields = Map("hiddenKey" -> "hiddenValue")
          ),
          errorMessage = Some(errorMessage),
          supervisoryBodyName = "Test Supervisory Body"
        ).body
      )
      errorDoc.mainContent shouldContainContent
        """
          |There is a problem
          |The file you uploaded was infected with a virus
          |Anti-money laundering supervision details
          |Evidence of your anti-money laundering supervision
          |Upload evidence to show that Test Supervisory Body is your current anti-money laundering supervisor.
          |You can choose what evidence to upload.
          |Suitable evidence might be a letter, email or payment receipt from your supervisory body, confirming you’re covered.
          |The file must be smaller than 5MB.
          |Types of file we can accept
          |These file types are allowed:
          |image (.jpg, .jpeg, .png or .tiff)
          |PDF (.pdf)
          |email (.txt or .msg)
          |Microsoft (Word, Excel or PowerPoint)
          |Open Document Format (ODF)
          |Choose your file
          |Error:
          |The file you uploaded was infected with a virus
          |Save and continue
          |""".stripMargin

      errorDoc.title() shouldBe s"Error: $heading - Apply for an agent services account - GOV.UK"
      errorDoc
        .selectOrFail("main > div > div > .govuk-error-summary .govuk-error-summary__title")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "There is a problem"
      errorDoc
        .selectOrFail("main > div > div > .govuk-error-summary .govuk-error-summary__list > li > a")
        .selectOnlyOneElementOrFail()
        .selectAttrOrFail("href") shouldBe "#fileToUpload"
      errorDoc
        .selectOrFail("form .govuk-error-message")
        .selectOnlyOneElementOrFail()
        .text() shouldBe s"Error: $errorMessage"
