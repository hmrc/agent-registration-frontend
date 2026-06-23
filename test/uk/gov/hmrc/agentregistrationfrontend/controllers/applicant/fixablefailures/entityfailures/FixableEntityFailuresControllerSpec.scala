/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.entityfailures

import com.softwaremill.quicklens.modify
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableEntityFailuresControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private def applyFix: Seq[EntityFix] => String => Seq[EntityFix] =
    fixes =>
      failureCode =>
        fixes.map:
          case a: EntityFix if a.toString === failureCode => a.modify(_.isConfirmed).setTo(Some(true))
          case other => other

  private def updatedRiskingOutcomeEntity(
    riskingOutcomeEntity: RiskingOutcomeEntity,
    failureCode: String
  ): RiskingOutcomeEntity.FailedFixable =
    riskingOutcomeEntity match
      case fixableEntity: RiskingOutcomeEntity.FailedFixable =>
        fixableEntity
          .modify(_.fixes)
          .setTo(applyFix(fixableEntity.fixes)(failureCode))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private val entityFixCheckLevelErrorMessages: Map[String, String] = Map(
    "EntityFix.4" -> "Select yes if all overdue returns have been filed",
    "EntityFix.5" -> "Select yes if all overdue liabilities have been paid or included in a payment plan",
    "EntityFix.8" -> "Select yes if all overdue liabilities have been paid or included in a payment plan"
  )

  private val soleTraderEntityFixFailureCodeHeadings: Map[String, String] = Map(
    "EntityFix.4.1" -> "Your Self Assessment returns",
    "EntityFix.4.3" -> "Your VAT returns",
    "EntityFix.4.4" -> "Your PAYE reports",
    "EntityFix.5.1" -> "You have an overdue Self Assessment liability",
    "EntityFix.5.3" -> "You have an overdue VAT liability",
    "EntityFix.5.4" -> "You have an overdue PAYE liability",
    "EntityFix.5.5" -> "You have an overdue civil penalty liability",
    "EntityFix.5.6" -> "You have an overdue Stamp Duty liability",
    "EntityFix.5.7" -> "You have an overdue Capital Gains Tax liability",
    "EntityFix.8.5" -> "You have an overdue penalty liability",
    "EntityFix.8.7" -> "You have an overdue penalty liability"
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

  private def pathForFailureCode(failureCode: String) = s"/agent-registration/conditions-not-yet-met/failure-details/$failureCode"
  object agentApplication:

    def riskingCompletedFailedFixableAllCodes: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableAllCodes

    def riskingCompletedFailedFixableSoleTraderAllCodes: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .riskingOutcomeEntityFailedFixableAllSoleTraderCodes

  final case class TestCase(
    name: String,
    application: AgentApplication,
    fixes: Seq[EntityFix],
    headings: Map[String, String]
  )
  List(
    TestCase(
      name = "Sole trader owner",
      application = agentApplication.riskingCompletedFailedFixableSoleTraderAllCodes,
      fixes = tdAll.riskingOutcomeEntityFailedFixableAllSoleTraderCodes.fixes,
      headings = soleTraderEntityFixFailureCodeHeadings
    ),
    TestCase(
      name = "Not sole trader owner",
      application = agentApplication.riskingCompletedFailedFixableAllCodes,
      fixes = tdAll.riskingOutcomeEntityFailedFixableAllCodes.fixes,
      headings = entityFixFailureCodeHeadings
    )
  ).foreach: testCase =>
    testCase.fixes.foreach: fix =>
      val (entityFixCode: String, entityFixHeading: String) = (fix.toString, testCase.headings(fix.toString))
      s"route for ${testCase.name} with entity fix code $entityFixCode should have correct path and method" in:
        AppRoutes.fixablefailures.entityfailures.FixableEntityFailuresController.show(entityFixCode) shouldBe Call(
          method = "GET",
          url = pathForFailureCode(entityFixCode)
        )
        AppRoutes.fixablefailures.entityfailures.FixableEntityFailuresController.submit(entityFixCode) shouldBe Call(
          method = "POST",
          url = pathForFailureCode(entityFixCode)
        )
        AppRoutes.fixablefailures.entityfailures.FixableEntityFailuresController.show(
          entityFixCode
        ).url shouldBe AppRoutes.fixablefailures.entityfailures.FixableEntityFailuresController.submit(entityFixCode).url

      s"GET ${pathForFailureCode(entityFixCode)} for ${testCase.name} should render correct content" in:
        ApplyStubHelper.stubsToSupplyBprToPage(application = testCase.application)
        val response: WSResponse = get(pathForFailureCode(entityFixCode))

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"$entityFixHeading - Apply for an agent services account - GOV.UK"
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

      s"POST ${pathForFailureCode(entityFixCode)} for ${testCase.name} with valid answer should redirect to the fixable task list" in:
        val application = testCase.application
        ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
          application = application,
          updatedApplication = application
            .modify(_.riskingOutcomeEntity)
            .setTo(
              Some(updatedRiskingOutcomeEntity(
                riskingOutcomeEntity = application.getRiskingOutcomeEntity,
                failureCode = entityFixCode
              ))
            )
        )
        val response: WSResponse = post(pathForFailureCode(entityFixCode))(Map(ConfirmFixForm.key -> Seq(YesNo.Yes.toString)))

        response.status shouldBe Status.SEE_OTHER
        response.header(HeaderNames.LOCATION) shouldBe Some(AppRoutes.fixablefailures.FixableTaskListController.show.url)
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

      s"POST ${pathForFailureCode(entityFixCode)} for ${testCase.name} without valid answer should re-render the form with the correct error" in:
        val application = testCase.application
        ApplyStubHelper.stubsToSupplyBprToPage(application = application)
        val response: WSResponse = post(pathForFailureCode(entityFixCode))(Map(ConfirmFixForm.key -> Seq("")))

        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"Error: $entityFixHeading - Apply for an agent services account - GOV.UK"
        doc.mainContent.select(
          s"#${ConfirmFixForm.key}-error"
        ).text() shouldBe s"Error: ${if entityFixCode === "EntityFix.4.4" then "Select yes if all overdue reports have been filed" else entityFixCheckLevelErrorMessages(entityFixCode.split('.').take(2).mkString("."))}"
        ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
