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

package uk.gov.hmrc.agentregistrationfrontend.views.individual.riskingoutcome.fixablefailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistrationfrontend.model.FixableIndividualTaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.model.TaskStatus
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.FixableTaskListPage

import scala.collection.immutable.TreeMap

class FixableTaskListPageSpec
extends ViewSpec:

  val viewTemplate: FixableTaskListPage = app.injector.instanceOf[FixableTaskListPage]
  val fixesComplete: Boolean = tdAll.riskingOutcomeIndividualFailedFixableAllCodes.fixes.forall(_.isConfirmed.contains(true))
  val fixableTaskListStatus = FixableIndividualTaskListStatus(
    fixableTasks = TreeMap.from(
      tdAll.riskingOutcomeIndividualFailedFixableAllCodes.fixes.map: fix =>
        fix.toString -> TaskStatus(
          canStart = true,
          isComplete = fix.isConfirmed.contains(true)
        )
    ),
    declaration = TaskStatus(
      canStart = fixesComplete,
      isComplete = false
    )
  )

  val doc: Document = Jsoup.parse(
    viewTemplate(
      taskListStatus = fixableTaskListStatus,
      correctiveActionExpiryDate = "17 August 2026",
      linkId = tdAll.linkId
    ).body
  )
  "FixableTaskListPage with all possible, non-identity based, individual fixes" should:
    "have expected content" in:
      doc.mainContent shouldContainContent
        """
          |Take action: You have not met the registration conditions
          |!
          |Warning
          |You can still take action to meet the conditions. Each item on this list needs to be completed by 17 August 2026 or the application will be deleted.
          |File your missing Self Assessment returns
          |Incomplete
          |File your missing VAT returns
          |Incomplete
          |File your missing PAYE reports
          |Incomplete
          |Pay your Self Assessment liability
          |Incomplete
          |Pay your VAT liability
          |Incomplete
          |Pay your PAYE liability
          |Incomplete
          |Pay your civil penalty liability
          |Incomplete
          |Pay your Stamp Duty liability
          |Incomplete
          |Pay your Capital Gains Tax liability
          |Incomplete
          |Pay your relevant anti-avoidance penalty liability
          |Incomplete
          |Confirm your responses are final
          |Cannot start yet
          |Save and come back later
        """.stripMargin

    s"have the correct h1" in:
      doc.h1 shouldBe "Take action: You have not met the registration conditions"

    s"have a save for later button link" in:
      doc.select("a.govuk-button--secondary")
        .attr("href") shouldBe s"${AppRoutes.providedetails.riskingoutcome.fixablefailures.SaveForLaterController.show(tdAll.linkId).url}"
