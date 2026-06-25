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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import com.softwaremill.quicklens.modify
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.AmlsRegistrationNumberForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class AmlsRegistrationNumberControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private val path = "/agent-registration/conditions-not-yet-met/anti-money-laundering/registration-number"

  private def updateFixableAmls(): Seq[EntityFix] => AmlsDetails => Seq[EntityFix] =
    fixes =>
      amlsDetails =>
        fixes.map:
          case a: AmlsFix => a.modify(_.amlsDetails).setTo(Some(amlsDetails))
          case other => other

  private def updatedRiskingOutcomeEntity(
    riskingOutcomeEntity: RiskingOutcomeEntity,
    amlsDetails: AmlsDetails
  ): RiskingOutcomeEntity.FailedFixable =
    riskingOutcomeEntity match
      case fixableEntity: RiskingOutcomeEntity.FailedFixable =>
        fixableEntity
          .modify(_.fixes)
          .setTo(updateFixableAmls()(fixableEntity.fixes)(amlsDetails))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private object agentApplication:

    val riskingCompletedFixableAmls: AgentApplicationLlp =
      tdAll
        .agentApplicationLlp
        .afterRiskingCompletedFixable

    // here we have just changed supervisor from HMRC to a new one so registration number will have been unset
    val newSupervisorInFix: AgentApplicationLlp = riskingCompletedFixableAmls
      .copy(
        riskingOutcomeEntity = Some(tdAll.riskingOutcomeEntityNewAmlsSupervisor)
      )

    def withUpdatedAmlsFix(amlsDetails: AmlsDetails): AgentApplicationLlp = riskingCompletedFixableAmls
      .modify(_.riskingOutcomeEntity)
      .setTo(Some(updatedRiskingOutcomeEntity(
        riskingOutcomeEntity = riskingCompletedFixableAmls.getRiskingOutcomeEntity,
        amlsDetails = amlsDetails
      )))

  private object ExpectedStrings:

    val heading = "What is the registration number?"
    val title = s"$heading - Apply for an agent services account - GOV.UK"
    val errorTitle = s"Error: $heading - Apply for an agent services account - GOV.UK"
    val hint = "This is the registration number given to you by your supervisory body."
    val requiredError = "Enter the registration number"
    val invalidFormatError = "Enter the registration number in the correct format"

  "routes should have correct paths and methods" in:
    AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show shouldBe Call(
      method = "GET",
      url = path // using path as that is what we are using in each test
    )
    AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.submit.url shouldBe AppRoutes.fixablefailures.amlsfailure.AmlsRegistrationNumberController.show.url

  private final case class TestCaseForAmlsRegistrationNumber(
    application: AgentApplication,
    updatedApplication: AgentApplication,
    amlsType: String,
    validInput: String,
    invalidInput: String,
    nextPage: String
  )

  List(
    TestCaseForAmlsRegistrationNumber(
      application = agentApplication.riskingCompletedFixableAmls,
      updatedApplication = agentApplication.riskingCompletedFixableAmls
        .modify(_.riskingOutcomeEntity)
        .setTo(Some(updatedRiskingOutcomeEntity(
          riskingOutcomeEntity = agentApplication.riskingCompletedFixableAmls.getRiskingOutcomeEntity,
          amlsDetails = AmlsDetails(
            supervisoryBody = AmlsSupervisoryBodyCode("HMRC"),
            amlsRegistrationNumber = Some(AmlsRegistrationNumber("XAML00000123457")),
            amlsEvidence = None // we are not testing anything to do with evidence here, so we can leave it as None
          )
        ))),
      amlsType = "HMRC",
      validInput = "XAML00000123457", // when the supervisory body is HMRC, the registration number has a different format to non-HMRC bodies
      invalidInput = "123",
      nextPage = AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url
    ),
    TestCaseForAmlsRegistrationNumber(
      application = agentApplication.newSupervisorInFix, // starting off with no registration number, as the supervisor has just been changed
      updatedApplication = agentApplication.newSupervisorInFix
        .modify(_.riskingOutcomeEntity)
        .setTo(Some(updatedRiskingOutcomeEntity(
          riskingOutcomeEntity = agentApplication.newSupervisorInFix.getRiskingOutcomeEntity,
          amlsDetails = AmlsDetails(
            supervisoryBody = AmlsSupervisoryBodyCode("ATT"),
            amlsRegistrationNumber = Some(AmlsRegistrationNumber("ATT AML-1234-123456")),
            amlsEvidence = None // we are not testing anything to do with evidence here, so we can leave it as None
          )
        ))),
      amlsType = "ATT",
      validInput = "ATT AML-1234-123456",
      invalidInput = ";</\\>",
      nextPage = AppRoutes.fixablefailures.amlsfailure.CheckYourAnswersController.show.url
    )
  ).foreach: testCase =>
    s"GET $path should return 200 for ${testCase.amlsType} and render page" in:
      ApplyStubHelper.stubsToSupplyBprToPage(testCase.application)
      val response: WSResponse = get(path)

      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      doc.title() shouldBe ExpectedStrings.title
      ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

    s"POST $path with valid input for ${testCase.amlsType} should redirect to the next page" in:
      ApplyStubHelper.stubsForSuccessfulUpdateWithBpr(
        application = testCase.application,
        updatedApplication = testCase.updatedApplication
      )
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.validInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe Constants.EMPTY_STRING
      response.header("Location").value shouldBe testCase.nextPage
      ApplyStubHelper.verifyConnectorsForSuccessfulUpdate()

    s"POST $path as blank form for ${testCase.amlsType} should return 400" in:
      ApplyStubHelper.stubsToSupplyBprToPage(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(Constants.EMPTY_STRING),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.BAD_REQUEST
      val doc = response.parseBodyAsJsoupDocument
      doc.title shouldBe ExpectedStrings.errorTitle
      doc.mainContent.select(
        s"#${AmlsRegistrationNumberForm.key}-error"
      ).text() shouldBe s"Error: ${ExpectedStrings.requiredError}"
      ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

    s"POST $path with an invalid value for ${testCase.amlsType} should return 400" in:
      ApplyStubHelper.stubsToSupplyBprToPage(testCase.application)
      val response: WSResponse =
        post(path)(Map(
          AmlsRegistrationNumberForm.key -> Seq(testCase.invalidInput),
          "submit" -> Seq("SaveAndContinue")
        ))

      response.status shouldBe Status.BAD_REQUEST
      val doc = response.parseBodyAsJsoupDocument
      doc.title shouldBe ExpectedStrings.errorTitle
      doc.mainContent.select(
        s"#${AmlsRegistrationNumberForm.key}-error"
      ).text() shouldBe s"Error: ${ExpectedStrings.invalidFormatError}"
      ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $path when registration number already stored should return 200 and render page with previous answer filled in" in:
    ApplyStubHelper.stubsToSupplyBprToPage(agentApplication.riskingCompletedFixableAmls)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe ExpectedStrings.title
    doc
      .select(s"input[name='${AmlsRegistrationNumberForm.key}']")
      .attr("value") shouldBe "XAML00000123456"
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
