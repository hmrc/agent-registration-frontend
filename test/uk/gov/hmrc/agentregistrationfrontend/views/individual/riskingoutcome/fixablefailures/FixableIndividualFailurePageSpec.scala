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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingoutcome.fixablefailures.FixableIndividualFailurePage

class FixableIndividualFailurePageSpec
extends ViewSpec:

  val viewTemplate: FixableIndividualFailurePage = app.injector.instanceOf[FixableIndividualFailurePage]
  private val checkLevelFormLegends: Map[String, String] = Map(
    "IndividualFix.4" -> "Have all overdue returns been filed?",
    "IndividualFix.5" -> "Have all overdue liabilities been paid or included in a payment plan?",
    "IndividualFix.8" -> "Have all overdue liabilities been paid or included in a payment plan?"
  )
  private val individualFixFailureCodeHeadings: Map[String, String] = Map(
    "IndividualFix.4.1" -> "Your Self Assessment returns",
    "IndividualFix.4.3" -> "Your VAT returns",
    "IndividualFix.4.4" -> "Your PAYE reports",
    "IndividualFix.5.1" -> "You have an overdue Self Assessment liability",
    "IndividualFix.5.3" -> "You have an overdue VAT liability",
    "IndividualFix.5.4" -> "You have an overdue PAYE liability",
    "IndividualFix.5.5" -> "You have an overdue civil penalty liability",
    "IndividualFix.5.6" -> "You have an overdue Stamp Duty liability",
    "IndividualFix.5.7" -> "You have an overdue Capital Gains Tax liability",
    "IndividualFix.8.5" -> "You have an overdue relevant anti-avoidance penalty liability",
    "IndividualFix.8.7" -> "You have an overdue relevant anti-avoidance penalty liability"
  )
  private val entityFailureCodeContent: Map[String, String] = Map(
    "IndividualFix.4.1" ->
      """
        |File your missing Self Assessment returns
        |Your Self Assessment returns
        |When we reviewed your application, we found that one or more returns had not been filed.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue returns must now be filed and any resulting liabilities paid.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out more about Self Assessment tax returns (opens in a new tab).
        |Have all overdue returns been filed?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.4.3" ->
      """
        |File your missing VAT returns
        |Your VAT returns
        |When we reviewed your application, we found that one or more returns had not been filed.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue returns must now be filed and any resulting liabilities paid.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out more about VAT returns (opens in a new tab).
        |Have all overdue returns been filed?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.4.4" ->
      """
        |File your missing PAYE reports
        |Your PAYE reports
        |When we reviewed your application, we found that one or more reports had not been filed.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue reports must now be filed and any resulting liabilities paid.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out more about PAYE for employers (opens in a new tab).
        |Have all overdue reports been filed?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.1" ->
      """
        |Pay your Self Assessment liability
        |You have an overdue Self Assessment liability
        |When we reviewed the application, we found that you owed an overdue amount relating to Self Assessment.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay Self Assessment (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.3" ->
      """
        |Pay your VAT liability
        |You have an overdue VAT liability
        |When we reviewed the application, we found that you owed an overdue amount relating to VAT.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay VAT (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.4" ->
      """
        |Pay your PAYE liability
        |You have an overdue PAYE liability
        |When we reviewed the application, we found that you owed an overdue amount relating to PAYE.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay PAYE (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.5" ->
      """
        |Pay your civil penalty liability
        |You have an overdue civil penalty liability
        |When we reviewed the application, we found that you owed an overdue amount relating to one or more civil penalties.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay taxes and penalties (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.6" ->
      """
        |Pay your Stamp Duty liability
        |You have an overdue Stamp Duty liability
        |When we reviewed the application, we found that you owed an overdue amount relating to Stamp Duty.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay Stamp Duty (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.5.7" ->
      """
        |Pay your Capital Gains Tax liability
        |You have an overdue Capital Gains Tax liability
        |When we reviewed the application, we found that you owed an overdue amount relating to Capital Gains Tax.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay Capital Gains Tax (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin,
    "IndividualFix.8.7" ->
      """
        |Pay your relevant anti-avoidance penalty liability
        |You have an overdue relevant anti-avoidance penalty liability
        |When we reviewed the application, we found that you owed an overdue amount relating to one or more relevant anti-avoidance penalties.
        |We cannot set up an agent services account for Test Company Name until this is resolved.
        |Take action on this issue
        |All overdue liabilities must now be paid in full or included in a payment plan.
        |You need to confirm by 17 August 2026 that this has been done.
        |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
        |Find out how to pay taxes and penalties (opens in a new tab).
        |Have all overdue liabilities been paid or included in a payment plan?
        |Yes
        |No
        |Save and continue
        |Is this page not working properly? (opens in new tab)
        |"""
        .stripMargin
  )

  object agentApplication:
    def riskingCompletedFailedFixableAllCodes: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableAllCodes

  entityFailureCodeContent.foreach:
    (
      individualFixCode: String,
      expectedContent: String
    ) =>
      val doc: Document = Jsoup.parse(
        viewTemplate(
          entityName = "Test Company Name",
          failureCode = individualFixCode,
          correctiveActionExpiryDate = "17 August 2026",
          form = ConfirmFixForm.form(individualFixCode),
          linkId = tdAll.linkId
        ).body
      )
      s"EntityFailureDetailsPage when entity has failure code $individualFixCode" should:
        "have expected content" in:
          doc.mainContent shouldContainContent expectedContent

        s"have the correct h1 for $individualFixCode" in:
          doc.h1 shouldBe individualFixFailureCodeHeadings.getOrElse(individualFixCode, "")

        s"have a form for confirming the fix for $individualFixCode" in:
          val form = doc.select("form")
          form.attr("action") shouldBe s"/agent-registration/provide-details/conditions-not-yet-met/failure-details/$individualFixCode/${tdAll.linkId.value}"
          form.attr("method") shouldBe ("POST")
          val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
            legend =
              if individualFixCode === "IndividualFix.4.4" then "Have all overdue reports been filed?"
              else checkLevelFormLegends(individualFixCode.split('.').take(2).mkString(".")),
            options = List(
              "Yes" -> YesNo.Yes.toString,
              "No" -> YesNo.No.toString
            ),
            hint = None
          )
          doc.extractRadioGroup() shouldBe expectedRadioGroup
          form.select("button[type=submit]").selectOnlyOneElementOrFail().text() shouldBe "Save and continue"

      s"render any given form error for $individualFixCode correctly" in:
        val field = ConfirmFixForm.key
        val errorMessage = "Select yes if all overdue returns have been filed"
        val formWithError = ConfirmFixForm.form(individualFixCode).withError(field, "Select yes if all overdue returns have been filed")
        val docWithError: Document = Jsoup.parse(
          viewTemplate(
            entityName = "Test Company Name",
            failureCode = individualFixCode,
            correctiveActionExpiryDate = "17 August 2026",
            form = formWithError,
            linkId = tdAll.linkId
          ).body
        )
        behavesLikePageWithErrorHandling(
          field = "isFixed",
          errorMessage = errorMessage,
          errorDoc = docWithError,
          heading = individualFixFailureCodeHeadings(individualFixCode)
        )
