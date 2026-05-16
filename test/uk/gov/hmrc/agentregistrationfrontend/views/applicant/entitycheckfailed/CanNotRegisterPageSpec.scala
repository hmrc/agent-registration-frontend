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
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.CanNotRegisterPage

class CanNotRegisterPageSpec
extends ViewSpec:

  val viewTemplate: CanNotRegisterPage = app.injector.instanceOf[CanNotRegisterPage]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val doc: Document = Jsoup.parse(
    viewTemplate("ABC Accountants").body
  )

  "CannotRegister" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |ABC Accountants does not meet the registration conditions
          |Your application to register ABC Accountants for an agent services account cannot be approved (refused under Section 230 of The Finance Act 2026).
          |This is because the business is on a list of organisations who cannot have an agent services account.
          |What to do if you disagree
          |If your circumstances change and you think you now meet the registration conditions, you can apply again.
          |If you disagree with the outcome, you can request a review or appeal the decision.
          |Finish and sign out
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "ABC Accountants does not meet the registration conditions - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "ABC Accountants does not meet the registration conditions"

    "have an apply again link to the gov.uk start page" in:
      val applyAgain = doc.mainContent.selectOrFail("a.govuk-link").toList.find(_.text() === "apply again").value
      applyAgain.attr("href") shouldBe appConfig.govukStartPageUrl

    "have a request a review or appeal the decision link to the gov.uk appeals guidance" in:
      val appeal = doc.mainContent.selectOrFail("a.govuk-link").toList.find(_.text() === "request a review or appeal the decision").value
      appeal.attr("href") shouldBe appConfig.guidanceForFailedNonFixableAppealsUrl

    "have a finish and sign out link" in:
      val signOut = doc.mainContent.selectOrFail("a.govuk-link").toList.find(_.text() === "Finish and sign out").value
      signOut.attr("href") shouldBe "/agent-registration/sign-out"
