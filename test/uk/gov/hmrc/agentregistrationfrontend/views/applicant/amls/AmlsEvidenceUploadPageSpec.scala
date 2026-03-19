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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector.UploadRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.UpscanInitiateConnector.UpscanInitiateResponse
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.FileUploadReference
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.amls.AmlsEvidenceUploadPage

class AmlsEvidenceUploadPageSpec
extends ViewSpec:

  val viewTemplate: AmlsEvidenceUploadPage = app.injector.instanceOf[AmlsEvidenceUploadPage]

  val doc: Document = Jsoup.parse(
    viewTemplate(
      upscanInitiateResponse = UpscanInitiateResponse(
        reference = FileUploadReference("reference"),
        uploadRequest = UploadRequest(
          href = "https://bucketName.s3.eu-west-2.amazonaws.com/upload",
          fields = Map("hiddenKey" -> "hiddenValue")
        )
      ),
      supervisoryBodyName = AmlsName("Gambling Commission")
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

    "render the correct values to control acceptable mime types for the file input" in:
      val fileInput = doc
        .mainContent
        .selectOrFail("form input[type='file']")
        .selectOnlyOneElementOrFail()
      fileInput.attr("name") shouldBe "file"
      fileInput.attr("accept") shouldBe "image/jpeg,image/png,image/tiff,application/pdf,text/plain,application/vnd.ms-outlook,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.oasis.opendocument.text,application/vnd.oasis.opendocument.spreadsheet,application/vnd.ms-powerpoint,application/vnd.openxmlformats-officedocument.presentationml.presentation,application/vnd.oasis.opendocument.presentation"

    "render config values correctly for JavaScript to use when enabled" in:
      val progressIndicator = doc
        .mainContent
        .selectOrFail("#file-upload-progress")
        .selectOnlyOneElementOrFail()
      progressIndicator.attr("data-max-file-size") shouldBe "5242880" // 5MiB in bytes
      progressIndicator.attr("data-check-upload-status-max-attempts") shouldBe "20"
      progressIndicator.attr("data-check-upload-status-interval-ms") shouldBe "1000"
      progressIndicator.attr(
        "data-check-upload-status-url"
      ) shouldBe AppRoutes.apply.amls.AmlsEvidenceUploadController.checkUploadStatusJs.url
      progressIndicator.attr("data-success") shouldBe s"$thisFrontendBaseUrl${AppRoutes.apply.amls.AmlsEvidenceUploadController.showUploadResult.url}"

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
