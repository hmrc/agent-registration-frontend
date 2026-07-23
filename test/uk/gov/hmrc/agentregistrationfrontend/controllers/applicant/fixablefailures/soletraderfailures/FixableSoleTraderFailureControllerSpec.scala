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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.soletraderfailures

import com.softwaremill.quicklens.modify
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.fixablefailures.ConfirmFixForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class FixableSoleTraderFailureControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private def applyIndividualFix: Seq[IndividualFix] => String => Seq[IndividualFix] =
    fixes =>
      failureCode =>
        fixes.map:
          case a: IndividualFix if a.toString === failureCode => a.modify(_.isConfirmed).setTo(Some(true))
          case other => other

  private def updatedRiskingOutcomeIndividual(
    riskingOutcomeIndividual: RiskingOutcomeIndividual,
    failureCode: String
  ): RiskingOutcomeIndividual.FailedFixable =
    riskingOutcomeIndividual match
      case fixableIndividual: RiskingOutcomeIndividual.FailedFixable =>
        fixableIndividual
          .modify(_.fixes)
          .setTo(applyIndividualFix(fixableIndividual.fixes)(failureCode))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private def applyEntityFix: Seq[EntityFix] => String => Seq[EntityFix] =
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
          .setTo(applyEntityFix(fixableEntity.fixes)(failureCode))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private val individualFixFailureCodeHeadings: Map[String, String] = Map(
    "4.1" -> "Your Self Assessment returns",
    "4.3" -> "Your VAT returns",
    "4.4" -> "Your PAYE reports",
    "5.1" -> "You have an overdue Self Assessment liability",
    "5.3" -> "You have an overdue VAT liability",
    "5.4" -> "You have an overdue PAYE liability",
    "5.5" -> "You have an overdue civil penalty liability",
    "5.6" -> "You have an overdue Stamp Duty liability",
    "5.7" -> "You have an overdue Capital Gains Tax liability",
    "8.5" -> "You have an overdue penalty liability",
    "8.7" -> "You have an overdue relevant anti-avoidance penalty liability"
  )

  private def pathForFailureCode(failureCode: String) = s"/agent-registration/conditions-not-yet-met/sole-trader-failure-details/$failureCode"

  object agentApplication:

    def riskingCompletedFailedFixableAllCodes: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .riskingOutcomeEntityFailedFixableAllSoleTraderCodes
    def riskingCompletedDuplicateFixes: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .riskingOutcomeEntitySoleTraderDuplicateCodes
    def riskingCompletedFailedFixableNoEntityFailures: AgentApplicationSoleTrader =
      tdAll
        .agentApplicationSoleTrader
        .riskingOutcomeEntityFailedFixableNoEntityFailures

  object individualProvidedDetails:

    val failingFixableDuplicateCodes: IndividualProvidedDetails = tdAll
      .providedDetails
      .afterFinished
      .copy(riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualSoleTraderDuplicateFixes))

    val failingFixableAllCodes: IndividualProvidedDetails = tdAll
      .providedDetails
      .afterFinished
      .copy(riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualFailedFixableAllCodes))
    val noFailures: IndividualProvidedDetails = tdAll
      .providedDetails
      .afterFinished
      .copy(riskingOutcomeIndividual = Some(RiskingOutcomeIndividual.Approved))

  final case class TestCase(
    name: String,
    application: AgentApplication,
    individual: IndividualProvidedDetails,
    fixesToTest: Seq[IndividualFix] | Seq[EntityFix],
    expectedUpdates: ExpectedUpdates // whether the fix is applied to the sole trader individual, the sole trader entity, or both
  )

  enum ExpectedUpdates:
    case IndividualOnly, EntityOnly, BothFromIndividual, BothFromEntity

  List(
    TestCase(
      name = "Sole trader with duplicate entity and individual fixable failure codes",
      application = agentApplication.riskingCompletedDuplicateFixes,
      individual = individualProvidedDetails.failingFixableDuplicateCodes,
      fixesToTest = tdAll.riskingOutcomeIndividualSoleTraderDuplicateFixes.fixes,
      expectedUpdates = ExpectedUpdates.BothFromIndividual // the individual fixes are fed in but both entity and individual fixes are expected to be fixed
    ),
    TestCase(
      name = "Sole trader with duplicate entity and individual fixable failure codes",
      application = agentApplication.riskingCompletedDuplicateFixes,
      individual = individualProvidedDetails.failingFixableDuplicateCodes,
      fixesToTest = tdAll.riskingOutcomeEntitySoleTraderDuplicateFixes.fixes,
      expectedUpdates = ExpectedUpdates.BothFromEntity // the entity fixes are fed in but both entity and individual fixes are expected to be fixed
    ),
    TestCase(
      name = "Sole trader with only individual fixable failures",
      application = agentApplication.riskingCompletedFailedFixableNoEntityFailures,
      individual = individualProvidedDetails.failingFixableAllCodes,
      fixesToTest = tdAll.riskingOutcomeIndividualFailedFixableAllCodes.fixes,
      expectedUpdates = ExpectedUpdates.IndividualOnly
    ),
    TestCase(
      name = "Sole trader with only entity fixable failures",
      application = agentApplication.riskingCompletedFailedFixableAllCodes,
      individual = individualProvidedDetails.noFailures,
      fixesToTest = tdAll.riskingOutcomeEntityFailedFixableAllSoleTraderCodes.fixes,
      expectedUpdates = ExpectedUpdates.EntityOnly
    )
  ).foreach: testCase =>
    testCase.fixesToTest.foreach: fix =>
      val (fixCode: String, individualFixHeading: String) = (fix.toString, individualFixFailureCodeHeadings(fix.toString.split("\\.", 2)(1)))
      s"route for ${testCase.name} with fix code $fixCode should have correct path and method" in:
        AppRoutes.fixablefailures.soletraderfailures.FixableSoleTraderFailureController.show(fixCode) shouldBe Call(
          method = "GET",
          url = pathForFailureCode(fixCode)
        )
        AppRoutes.fixablefailures.soletraderfailures.FixableSoleTraderFailureController.submit(fixCode) shouldBe Call(
          method = "POST",
          url = pathForFailureCode(fixCode)
        )
        AppRoutes.fixablefailures.soletraderfailures.FixableSoleTraderFailureController.show(
          fixCode
        ).url shouldBe AppRoutes.fixablefailures.soletraderfailures.FixableSoleTraderFailureController.submit(fixCode).url

      s"GET ${pathForFailureCode(fixCode)} for ${testCase.name} should render correct content" in:
        ApplyStubHelper.stubsForApplicationBprAndIndividuals(
          application = testCase.application,
          individuals = List(testCase.individual)
        )
        val response: WSResponse = get(pathForFailureCode(fixCode))

        response.status shouldBe Status.OK
        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"$individualFixHeading - Apply for an agent services account - GOV.UK"

      s"POST ${pathForFailureCode(fixCode)} for ${testCase.name} with valid answer should update ${testCase.expectedUpdates.toString} and redirect to the fixable task list" in:
        testCase.expectedUpdates match
          case ExpectedUpdates.IndividualOnly =>
            ApplyStubHelper.stubFixableFailureUpdate(
              agentApplication = testCase.application,
              individualProvidedDetails = testCase.individual,
              updatedIndividualProvidedDetails = Some(testCase.individual.copy(
                riskingOutcomeIndividual = Some(updatedRiskingOutcomeIndividual(testCase.individual.getRiskingOutcomeIndividual, fixCode))
              )),
              updatedApplication = None
            )
          case ExpectedUpdates.EntityOnly =>
            ApplyStubHelper.stubFixableFailureUpdate(
              agentApplication = testCase.application,
              individualProvidedDetails = testCase.individual,
              updatedIndividualProvidedDetails = None,
              updatedApplication = Some(testCase.application.modify(_.riskingOutcomeEntity).setTo(
                Some(updatedRiskingOutcomeEntity(testCase.application.getRiskingOutcomeEntity, fixCode))
              ))
            )
          case ExpectedUpdates.BothFromIndividual =>
            ApplyStubHelper.stubFixableFailureUpdate(
              agentApplication = testCase.application,
              individualProvidedDetails = testCase.individual,
              updatedIndividualProvidedDetails = Some(testCase.individual.copy(
                riskingOutcomeIndividual = Some(updatedRiskingOutcomeIndividual(testCase.individual.getRiskingOutcomeIndividual, fixCode))
              )),
              updatedApplication = Some(testCase.application.modify(_.riskingOutcomeEntity).setTo(
                Some(updatedRiskingOutcomeEntity(testCase.application.getRiskingOutcomeEntity, fixCode.replace("IndividualFix", "EntityFix")))
              ))
            )
          case ExpectedUpdates.BothFromEntity =>
            ApplyStubHelper.stubFixableFailureUpdate(
              agentApplication = testCase.application,
              individualProvidedDetails = testCase.individual,
              updatedIndividualProvidedDetails = Some(testCase.individual.copy(
                riskingOutcomeIndividual = Some(updatedRiskingOutcomeIndividual(
                  testCase.individual.getRiskingOutcomeIndividual,
                  fixCode.replace("EntityFix", "IndividualFix")
                ))
              )),
              updatedApplication = Some(testCase.application.modify(_.riskingOutcomeEntity).setTo(
                Some(updatedRiskingOutcomeEntity(testCase.application.getRiskingOutcomeEntity, fixCode))
              ))
            )
        val response: WSResponse = post(pathForFailureCode(fixCode))(Map(ConfirmFixForm.key -> Seq(YesNo.Yes.toString)))

        response.status shouldBe Status.SEE_OTHER
        response.header(HeaderNames.LOCATION) shouldBe Some(
          AppRoutes.fixablefailures.FixableTaskListController.show.url
        )

      s"POST ${pathForFailureCode(fixCode)} for ${testCase.name} without valid answer should re-render the form with error" in:
        ApplyStubHelper.stubsForApplicationBprAndIndividuals(
          application = testCase.application,
          individuals = List(testCase.individual)
        )
        val response: WSResponse = post(pathForFailureCode(fixCode))(Map(ConfirmFixForm.key -> Seq("")))

        val doc = response.parseBodyAsJsoupDocument
        doc.title() shouldBe s"Error: $individualFixHeading - Apply for an agent services account - GOV.UK"
