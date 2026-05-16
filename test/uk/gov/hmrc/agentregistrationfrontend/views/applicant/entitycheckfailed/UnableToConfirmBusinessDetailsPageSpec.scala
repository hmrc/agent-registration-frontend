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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.entitycheckfailed

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.UnableToConfirmBusinessDetailsPage

class UnableToConfirmBusinessDetailsPageSpec
extends ViewSpec:

  val viewTemplate: UnableToConfirmBusinessDetailsPage = app.injector.instanceOf[UnableToConfirmBusinessDetailsPage]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val doc: Document = Jsoup.parse(viewTemplate().body)

  "UnableToConfirmBusinessDetails" should:

    "have the correct title" in:
      doc.title() shouldBe "We cannot confirm your business details - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "We cannot confirm your business details"

    "have the expected body content with bullet list" in:
      doc.mainContent shouldContainContent
        """
          |This might be because:
          |you provided incomplete details
          |you provided details that did not match the HMRC record
          |we could not find a record of the business
          |You cannot apply for an agent services account until we can confirm the business details.
          |If you think you gave incorrect answers, you can try to confirm the business details again.
          |Alternatively you can contact HMRC.
          |""".stripMargin

    "have a 'try to confirm the business details again' link to start the application again" in:
      val tryAgain = doc.mainContent.selectOrFail("a.govuk-link").toList.find(_.text() === "try to confirm the business details again").value
      tryAgain.attr("href") shouldBe "/agent-registration/apply"

    "have a 'contact HMRC' link to the gov.uk dedicated helplines guidance" in:
      val contact = doc.mainContent.selectOrFail("a.govuk-link").toList.find(_.text() === "contact HMRC").value
      contact.attr("href") shouldBe appConfig.guidanceForDedicatedHelplinesForAuthorisedAgentsUrl
