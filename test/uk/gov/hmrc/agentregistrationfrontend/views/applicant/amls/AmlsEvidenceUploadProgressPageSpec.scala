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
import uk.gov.hmrc.agentregistrationfrontend.model.upscan.UploadStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.amls.AmlsEvidenceUploadProgressPage

class AmlsEvidenceUploadProgressPageSpec
extends ViewSpec:

  val viewTemplate: AmlsEvidenceUploadProgressPage = app.injector.instanceOf[AmlsEvidenceUploadProgressPage]

  val inProgressDoc: Document = Jsoup.parse(
    viewTemplate(status = UploadStatus.InProgress).body
  )

  val successfulDoc: Document = Jsoup.parse(
    viewTemplate(
      status = tdAll.uploadUploadedSuccessfully.uploadStatus
    ).body
  )

  val virusDoc: Document = Jsoup.parse(
    viewTemplate(status = tdAll.uploadFailedWithVirus.uploadStatus).body
  )

  val failedDoc: Document = Jsoup.parse(
    viewTemplate(status = tdAll.uploadFailed.uploadStatus).body
  )

  private val inProgressHeading: String = "We are checking your upload"
  private val uploadSuccessfulHeading: String = "Your upload is complete"
  private val virusHeading: String = "Your upload has a virus"
  private val failedHeading: String = "Your upload has failed scanning"

  "AmlsEvidenceUploadProgressPage view" should:

    "contain expected content when status is InProgress" in:
      inProgressDoc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |We are checking your upload
          |Wait a few seconds and then select ‘Continue’.
          |Continue
          |""".stripMargin

    "have the correct title when status is InProgress" in:
      inProgressDoc.title() shouldBe s"$inProgressHeading - Apply for an agent services account - GOV.UK"

    "have the correct h1 when status is InProgress" in:
      inProgressDoc.h1 shouldBe inProgressHeading

    "render a continue button when status is InProgress" in:
      inProgressDoc
        .mainContent
        .selectOrFail(".govuk-button")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Continue"

    "contain expected content when status is UploadedSuccessfully" in:
      successfulDoc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Your upload is complete
          |Your file evidence.pdf has been uploaded successfully.
          |Continue
          |""".stripMargin

    "have the correct title when status is UploadedSuccessfully" in:
      successfulDoc.title() shouldBe s"$uploadSuccessfulHeading - Apply for an agent services account - GOV.UK"

    "have the correct h1 when status is UploadedSuccessfully" in:
      successfulDoc.h1 shouldBe uploadSuccessfulHeading

    "render a continue button when status is UploadedSuccessfully" in:
      successfulDoc
        .mainContent
        .selectOrFail(".govuk-button")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Continue"

    "contain expected content when status has failed scanning for unknown reason" in:
      failedDoc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Your upload has failed scanning
          |Your file upload has failed scanning. Try uploading another file.
          |Try again
          |""".stripMargin

    "have the correct title when status has failed scanning for unknown reason" in:
      failedDoc.title() shouldBe s"$failedHeading - Apply for an agent services account - GOV.UK"

    "have the correct h1 when status has failed scanning" in:
      failedDoc.h1 shouldBe failedHeading

    "render a Try again button when status has failed scanning for unknown reason" in:
      failedDoc
        .mainContent
        .selectOrFail(".govuk-button")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Try again"

    "contain expected content when status has failed scanning with a virus" in:
      virusDoc.mainContent shouldContainContent
        """
          |Anti-money laundering supervision details
          |Your upload has a virus
          |A virus has been detected in your uploaded file, try uploading another file.
          |Try again
          |""".stripMargin

    "have the correct title when status has failed scanning with a virus" in:
      virusDoc.title() shouldBe s"$virusHeading - Apply for an agent services account - GOV.UK"

    "have the correct h1 when status has failed scanning with a virus" in:
      virusDoc.h1 shouldBe virusHeading

    "render a Try again button when status has failed scanning with a virus" in:
      virusDoc
        .mainContent
        .selectOrFail(".govuk-button")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Try again"
