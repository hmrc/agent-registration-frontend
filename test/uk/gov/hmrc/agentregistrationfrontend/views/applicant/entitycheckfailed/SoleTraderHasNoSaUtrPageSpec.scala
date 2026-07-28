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
import org.jsoup.nodes.Element
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.checkfailed.SoleTraderHasNoSaUtrPage

class SoleTraderHasNoSaUtrPageSpec
extends ViewSpec:

  val viewTemplate: SoleTraderHasNoSaUtrPage = app.injector.instanceOf[SoleTraderHasNoSaUtrPage]
  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val doc: Document = Jsoup.parse(
    viewTemplate().body
  )

  val allLinks: List[Element] = doc.mainContent.select("a.govuk-link").toList

  "SoleTraderHasNoSaUtrPage" should:

    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |You cannot apply for an agent services account yet
          |You need to be registered for Self Assessment and have a Unique Taxpayer Reference (UTR) to apply for an agent services account.
          |If you already have a UTR, you can provide it by confirming your business details again.
          |If you are registered for Self Assessment, you can get help to find your UTR here.
          |Alternatively you can contact HMRC.
          |"""
          .stripMargin

    "have the correct title" in:
      doc.title() shouldBe "You cannot apply for an agent services account yet - Apply for an agent services account - GOV.UK"

    "have the correct h1" in:
      doc.h1 shouldBe "You cannot apply for an agent services account yet"

    "have a confirm your business details again link to the Agent Type page" in:
      val confirmAgain = allLinks.find(_.text() === "confirming your business details again").value
      confirmAgain.attr("href") shouldBe AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url

    "have a find your lost UTR link to the gov.uk guidance" in:
      val appeal = allLinks.find(_.text() === "get help to find your UTR here").value
      appeal.attr("href") shouldBe appConfig.findLostUtrUrl

    "have a contact HMRC link" in:
      val contactHmrc = allLinks.find(_.text() === "contact HMRC").value
      contactHmrc.attr("href") shouldBe appConfig.contactHmrcUrl
