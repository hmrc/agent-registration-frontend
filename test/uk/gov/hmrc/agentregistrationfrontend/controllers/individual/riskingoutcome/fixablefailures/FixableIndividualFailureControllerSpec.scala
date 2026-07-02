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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures

import com.softwaremill.quicklens.modify
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableIndividualFailureControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private def applyFix: Seq[IndividualFix] => String => Seq[IndividualFix] =
    fixes =>
      failureCode =>
        fixes.map:
          case a: IndividualFix if a.toString === failureCode => a.modify(_.isConfirmed).setTo(Some(true))
          case other => other

  private def applyIndividualFix(
    riskingOutcomeIndividual: RiskingOutcomeIndividual,
    failureCode: String
  ): RiskingOutcomeIndividual.FailedFixable =
    riskingOutcomeIndividual match
      case fixableIndividual: RiskingOutcomeIndividual.FailedFixable =>
        fixableIndividual
          .modify(_.fixes)
          .setTo(applyFix(fixableIndividual.fixes)(failureCode))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private val individualFixCheckLevelErrors: Map[String, String] = Map(
    "IndividualFix.4" -> "Select yes if all overdue returns have been filed",
    "IndividualFix.5" -> "Select yes if all overdue liabilities have been paid or included in a payment plan",
    "IndividualFix.8" -> "Select yes if all overdue liabilities have been paid or included in a payment plan"
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
    "IndividualFix.8.7" -> "You have an overdue relevant anti-avoidance penalty liability"
  )

  private def pathForFailureCode(failureCode: String) =
    s"/agent-registration/provide-details/conditions-not-yet-met/failure-details/$failureCode/${tdAll.linkId.value}"
  object riskingOutcomeIndividual:
    val failingFixableAllCodes: RiskingOutcomeIndividual.FailedFixable =
      tdAll
        .riskingOutcomeIndividualFailedFixableAllCodes

  object agentApplication:

    def riskingCompletedFailedFixableAllCodes: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixableAllCodes

  final case class TestCase(
    name: String,
    application: AgentApplication,
    fixes: Seq[IndividualFix],
    headings: Map[String, String]
  )
  List(
    TestCase(
      name = "Individual with all fixable failure codes",
      application = agentApplication.riskingCompletedFailedFixableAllCodes,
      fixes = tdAll.riskingOutcomeIndividualFailedFixableAllCodes.fixes,
      headings = individualFixFailureCodeHeadings
    )
  ).foreach: testCase =>
    testCase.fixes.foreach: fix =>
      val (individualFixCode: String, individualFixHeading: String) = (fix.toString, testCase.headings(fix.toString))
      s"route for ${testCase.name} with individual fix code $individualFixCode should have correct path and method" in:
        AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableIndividualFailureController.show(individualFixCode, tdAll.linkId) shouldBe Call(
          method = "GET",
          url = pathForFailureCode(individualFixCode)
        )
        AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableIndividualFailureController.submit(individualFixCode, tdAll.linkId) shouldBe Call(
          method = "POST",
          url = pathForFailureCode(individualFixCode)
        )
        AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableIndividualFailureController.show(
          fixCode = individualFixCode,
          linkId = tdAll.linkId
        ).url shouldBe AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableIndividualFailureController.submit(individualFixCode, tdAll.linkId).url

      s"GET ${pathForFailureCode(individualFixCode)} for ${testCase.name} should render correct content" in:
        ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
          agentApplication = testCase.application,
          individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
            riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualFailedFixableAllCodes)
          ),
          isScr = false
        )
        val response: WSResponse = get(pathForFailureCode(individualFixCode))

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"$individualFixHeading - Apply for an agent services account - GOV.UK"

      s"POST ${pathForFailureCode(individualFixCode)} for ${testCase.name} with valid answer should redirect to the fixable task list" in:
        ProvideDetailsStubHelper.stubFixableFailureUpdate(
          agentApplication = testCase.application,
          individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
            riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualFailedFixableAllCodes)
          ),
          updatedIndividualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
            riskingOutcomeIndividual = Some(applyIndividualFix(tdAll.riskingOutcomeIndividualFailedFixableAllCodes, individualFixCode))
          )
        )
        val response: WSResponse = post(pathForFailureCode(individualFixCode))(Map(ConfirmFixForm.key -> Seq(YesNo.Yes.toString)))

        response.status shouldBe Status.SEE_OTHER
        response.header(HeaderNames.LOCATION) shouldBe Some(
          AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableTaskListController.show(tdAll.linkId).url
        )

      s"POST ${pathForFailureCode(individualFixCode)} for ${testCase.name} without valid answer should re-render the form with the correct error" in:
        ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
          agentApplication = testCase.application,
          individualProvidedDetails = tdAll.providedDetails.afterFinished.copy(
            riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualFailedFixableAllCodes)
          ),
          isScr = false
        )
        val response: WSResponse = post(pathForFailureCode(individualFixCode))(Map(ConfirmFixForm.key -> Seq("")))

        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"Error: $individualFixHeading - Apply for an agent services account - GOV.UK"
        doc.mainContent.select(
          s"#${ConfirmFixForm.key}-error"
        ).text() shouldBe s"Error: ${if individualFixCode === "IndividualFix.4.4" then "Select yes if all overdue reports have been filed" else individualFixCheckLevelErrors(individualFixCode.split('.').take(2).mkString("."))}"
