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

package uk.gov.hmrc.agentregistrationfrontend.views.applicant.fixablefailures.entityfailures

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ViewSpec
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.entityfailures.EntityFailureDetailsPage

class EntityFailureDetailsPageSpec
extends ViewSpec:

  val viewTemplate: EntityFailureDetailsPage = app.injector.instanceOf[EntityFailureDetailsPage]
  private val checkLevelFormLegends: Map[String, String] = Map(
    "EntityFix.4" -> "Have all overdue returns been filed?",
    "EntityFix.5" -> "Have all overdue liabilities been paid or included in a payment plan?",
    "EntityFix.8" -> "Have all overdue liabilities been paid or included in a payment plan?"
  )
  private val entityFixFailureCodeHeadings: Map[String, String] = Map(
    "EntityFix.4.1" -> "Self Assessment returns for Test Company Name",
    "EntityFix.4.2" -> "Company Tax returns for Test Company Name",
    "EntityFix.4.3" -> "VAT returns for Test Company Name",
    "EntityFix.4.4" -> "PAYE reports for Test Company Name",
    "EntityFix.5.1" -> "Test Company Name has an overdue Self Assessment liability",
    "EntityFix.5.2" -> "Test Company Name has an overdue Corporation Tax liability",
    "EntityFix.5.3" -> "Test Company Name has an overdue VAT liability",
    "EntityFix.5.4" -> "Test Company Name has an overdue PAYE liability",
    "EntityFix.5.5" -> "Test Company Name has an overdue civil penalty liability",
    "EntityFix.5.6" -> "Test Company Name has an overdue Stamp Duty liability",
    "EntityFix.5.7" -> "Test Company Name has an overdue Capital Gains Tax liability",
    "EntityFix.8.5" -> "Test Company Name has an overdue penalty liability",
    "EntityFix.8.7" -> "Test Company Name has an overdue penalty liability"
  )
  private val entityFailureCodeContent: Map[String, String] = Map(
    "EntityFix.4.1" -> """
                         |Issues with the business
                         |Self Assessment returns for Test Company Name
                         |When we reviewed the application for Test Company Name, we found that one or more returns had not been filed.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue returns must now be filed and any resulting liabilities paid.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out more about Self Assessment tax returns (opens in a new tab).
                         |Have all overdue returns been filed?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.4.2" -> """
                         |Issues with the business
                         |Company Tax returns for Test Company Name
                         |When we reviewed the application for Test Company Name, we found that one or more returns had not been filed.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue returns must now be filed and any resulting liabilities paid.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out more about Company Tax returns (opens in a new tab).
                         |Have all overdue returns been filed?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.4.3" -> """
                         |Issues with the business
                         |VAT returns for Test Company Name
                         |When we reviewed the application for Test Company Name, we found that one or more returns had not been filed.
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
                         |"""
      .stripMargin,
    "EntityFix.4.4" -> """
                         |Issues with the business
                         |PAYE reports for Test Company Name
                         |When we reviewed the application for Test Company Name, we found that one or more reports had not been filed.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue reports must now be filed and any resulting liabilities paid.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out more about PAYE for employers (opens in a new tab).
                         |Have all overdue reports been filed?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.1" -> """
                         |Issues with the business
                         |Test Company Name has an overdue Self Assessment liability
                         |When we reviewed the application, we found that Test Company Name has an overdue Self Assessment liability.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay Self Assessment (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.2" -> """
                         |Issues with the business
                         |Test Company Name has an overdue Corporation Tax liability
                         |When we reviewed the application, we found that Test Company Name has an overdue Corporation Tax liability.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay Corporation Tax (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.3" -> """
                         |Issues with the business
                         |Test Company Name has an overdue VAT liability
                         |When we reviewed the application, we found that Test Company Name has an overdue VAT liability.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay VAT (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.4" -> """
                         |Issues with the business
                         |Test Company Name has an overdue PAYE liability
                         |When we reviewed the application, we found that Test Company Name has an overdue PAYE liability.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay PAYE (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.5" -> """
                         |Issues with the business
                         |Test Company Name has an overdue civil penalty liability
                         |When we reviewed the application, we found that Test Company Name owed an overdue amount relating to one or more civil penalties.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay taxes and penalties (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.6" -> """
                         |Issues with the business
                         |Test Company Name has an overdue Stamp Duty liability
                         |When we reviewed the application, we found that Test Company Name owed an overdue amount relating to Stamp Duty.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay Stamp Duty (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.5.7" -> """
                         |Issues with the business
                         |Test Company Name has an overdue Capital Gains Tax liability
                         |When we reviewed the application, we found that Test Company Name owed an overdue amount relating to Capital Gains Tax.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay Capital Gains Tax (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.8.5" -> """
                         |Issues with the business
                         |Test Company Name has an overdue penalty liability
                         |When we reviewed the application, we found that Test Company Name owed an overdue amount relating to one or more relevant anti-avoidance penalties.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay taxes and penalties (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
                         |"""
      .stripMargin,
    "EntityFix.8.7" -> """
                         |Issues with the business
                         |Test Company Name has an overdue penalty liability
                         |When we reviewed the application, we found that Test Company Name owed an overdue amount relating to one or more relevant anti-avoidance penalties.
                         |We cannot set up an agent services account for Test Company Name until this has been resolved.
                         |Take action on this issue
                         |All overdue liabilities must now be paid in full or included in a payment plan.
                         |You need to confirm by 17 August 2026 that this has been done.
                         |If you do not confirm this, we will delete the application for an agent services account. This is to comply with our data retention policy.
                         |Find out how to pay taxes and penalties (opens in a new tab).
                         |Have all overdue liabilities been paid or included in a payment plan?
                         |Yes
                         |No
                         |Save and continue
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
      entityFixCode: String,
      expectedContent: String
    ) =>
      val doc: Document = Jsoup.parse(
        viewTemplate(
          entityName = "Test Company Name",
          failureCode = entityFixCode,
          correctiveActionExpiryDate = "17 August 2026",
          form = ConfirmFixForm.form(entityFixCode)
        ).body
      )
      s"EntityFailureDetailsPage when entity has failure code $entityFixCode" should:
        "have expected content" in:
          doc.mainContent shouldContainContent expectedContent

        s"have the correct h1 for $entityFixCode" in:
          doc.h1 shouldBe entityFixFailureCodeHeadings.getOrElse(entityFixCode, "")

        s"have a form for confirming the fix for $entityFixCode" in:
          val form = doc.select("form")
          form.attr("action") shouldBe s"/agent-registration/conditions-not-yet-met/failure-details/$entityFixCode"
          form.attr("method") shouldBe ("POST")
          val expectedRadioGroup: TestRadioGroup = TestRadioGroup(
            legend =
              if entityFixCode === "EntityFix.4.4" then "Have all overdue reports been filed?"
              else checkLevelFormLegends(entityFixCode.split('.').take(2).mkString(".")),
            options = List(
              "Yes" -> YesNo.Yes.toString,
              "No" -> YesNo.No.toString
            ),
            hint = None
          )
          doc.extractRadioGroup() shouldBe expectedRadioGroup
          form.select("button[type=submit]").selectOnlyOneElementOrFail().text() shouldBe "Save and continue"

      s"render any given form error for $entityFixCode correctly" in:
        val field = ConfirmFixForm.key
        val errorMessage = "Select yes if all overdue returns have been filed"
        val formWithError = ConfirmFixForm.form(entityFixCode).withError(field, "Select yes if all overdue returns have been filed")
        val docWithError: Document = Jsoup.parse(
          viewTemplate(
            entityName = "Test Company Name",
            failureCode = entityFixCode,
            correctiveActionExpiryDate = "17 August 2026",
            form = formWithError
          ).body
        )
        behavesLikePageWithErrorHandling(
          field = "isFixed",
          errorMessage = errorMessage,
          errorDoc = docWithError,
          heading = entityFixFailureCodeHeadings(entityFixCode)
        )
