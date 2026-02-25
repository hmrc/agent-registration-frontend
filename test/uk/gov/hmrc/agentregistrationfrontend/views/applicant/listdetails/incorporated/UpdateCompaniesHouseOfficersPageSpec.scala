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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.listdetails.incorporated

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.model.SubmitAction.SaveAndComeBackLater
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.UpdateCompaniesHouseOfficersPage

class UpdateCompaniesHouseOfficersPageSpec
extends ViewSpec:

  val viewTemplate: UpdateCompaniesHouseOfficersPage = app.injector.instanceOf[UpdateCompaniesHouseOfficersPage]

  val agentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .afterHmrcStandardForAgentsAgreed

  private val entityName: String = tdAll.companyName

  private val caption: String = "LLP member and tax adviser information"
  private val heading: String = "You need to update Companies House"

  private def render(): Document = Jsoup.parse(viewTemplate(
    entityName = entityName,
    agentApplication = agentApplication
  ).body)

  "UpdateCompaniesHousePage" should:

    val doc: Document = render()

    "contain expected content" in:
      doc.mainContent shouldContainContent (
        s"""
           |$caption
           |$heading
           |As part of this application process, we need a verifiable list of the current members of $entityName.
           |Update your Companies House so the correct details are showing on their website.
           |You can then continue with your application for an agent services account.
           |Save and come back later
           |Is this page not working properly? (opens in new tab)
           |""".stripMargin
      )

    "render a save and come back later button" in:
      doc
        .mainContent
        .selectOrFail(s"a.govuk-button, button[value=${SaveAndComeBackLater.toString}]")
        .selectOnlyOneElementOrFail()
        .text() shouldBe "Save and come back later"

    "have the correct title" in:
      doc.title() shouldBe s"$heading - Apply for an agent services account - GOV.UK"
