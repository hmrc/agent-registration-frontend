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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.fixablefailures.amls

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.FixableAmlsDetailsPage

class FixableAmlsDetailsPageSpec
extends ViewSpec:

  val viewTemplate: FixableAmlsDetailsPage = app.injector.instanceOf[FixableAmlsDetailsPage]
  private val amlsFailureCodeHeadings: Map[String, String] = Map(
    "EntityFailure.3.1" -> "We could not match your details with a current record",
    "EntityFailure.3.2" -> "We have not been able to confirm your anti-money laundering supervision",
    "EntityFailure.3.3" -> "We could not accept your evidence",
    "EntityFailure.3.5" -> "Your membership does not include anti-money laundering supervision"
  )
  private val amlsFailureCodeContent: Map[String, String] = Map(
    "EntityFailure.3.1" -> """
                             |Anti-money laundering supervision details
                             |We could not match your details with a current record
                             |You told us that Test Company Name is supervised by Association of TaxationTechnicians (ATT) for anti-money laundering.
                             |The registration number you gave was ATT AML-1234-123456.
                             |However, this number is not in Association of TaxationTechnicians (ATT)’s register.
                             |Please check your details and try again.
                             |"""
      .stripMargin,
    "EntityFailure.3.2" -> """
                             |Anti-money laundering supervision details
                             |We have not been able to confirm your anti-money laundering supervision
                             |You gave us these supervisory details for Test Company Name:
                             |Association of TaxationTechnicians (ATT)
                             |registration number: ATT AML-1234-123456
                             |We could not find a record of this supervision.
                             |One or more of the details you provided might be incorrect.
                             |Please check your details and try again.
                             |"""
      .stripMargin,
    "EntityFailure.3.3" -> """
                             |Anti-money laundering supervision details
                             |We could not accept your evidence
                             |The evidence you uploaded did not meet our requirements.
                             |It did not prove that Association of TaxationTechnicians (ATT) is the current anti-money laundering supervisor for Test Company Name.
                             |This might be because the document did not contain:
                             |the name of your business
                             |a date to prove you paid the supervision fee within the last 12 months
                             |the name of the supervisory body
                             |clear enough writing to be read easily
                             |You need to upload the evidence we require on the next page.
                             |Continue
                             |"""
      .stripMargin,
    "EntityFailure.3.5" -> """
                             |Anti-money laundering supervision details
                             |Your membership does not include anti-money laundering supervision
                             |You gave us these supervisory details for Test Company Name:
                             |Association of TaxationTechnicians (ATT)
                             |registration number: ATT AML-1234-123456
                             |These records are for student membership. A student membership does not provide the level of anti-money laundering supervision you need as an agent with HMRC.
                             |You need to confirm that Test Company Name has membership of a professional body that includes anti-money laundering supervision.
                             |Continue
                             |"""
      .stripMargin
  )
  object agentApplication:

    val withNonHmrcAmls: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionAmls
        .whenSupervisorBodyIsNonHmrc
        .complete

    val withHmrcAmls: AgentApplication =
      tdAll
        .agentApplicationLlpSections
        .sectionAmls
        .whenSupervisorBodyIsHmrc
        .complete

  amlsFailureCodeContent.foreach:
    (
      failureCode: String,
      expectedContent: String
    ) =>
      val doc: Document = Jsoup.parse(
        viewTemplate(
          entityName = "Test Company Name",
          failureCode = failureCode,
          amlsDetails = agentApplication.withNonHmrcAmls.getAmlsDetails
        ).body
      )
      s"FailedNonFixablePage when individuals have failures with failure code $failureCode" should:
        "have expected content" in:
          doc.mainContent shouldContainContent expectedContent

      s"have the correct title for $failureCode" in:
        doc.title() shouldBe s"${amlsFailureCodeHeadings.getOrElse(failureCode, "")} - Apply for an agent services account - GOV.UK"

      s"have the correct h1 for $failureCode" in:
        doc.h1 shouldBe amlsFailureCodeHeadings.getOrElse(failureCode, "")
