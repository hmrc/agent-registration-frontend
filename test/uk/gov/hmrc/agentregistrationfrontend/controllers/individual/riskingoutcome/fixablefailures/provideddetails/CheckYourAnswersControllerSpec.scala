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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures.provideddetails

import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  val linkId = tdAll.linkId

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/check-your-answers/${linkId.value}"
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterRiskingCompletedApprovedWithFixableIndividuals

  "route should have correct path and method" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  private object individualProvideDetails:

    val missingSaUtr: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoProvided.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )
    val missingDateOfBirth: IndividualProvidedDetails = tdAll.providedDetails.afterEmailAddressVerified.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )
    val missingNino: IndividualProvidedDetails = tdAll.providedDetails.AfterDateOfBirth.afterDateOfBirthProvided.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )

  private final case class TestCaseForCya(
    providedDetails: IndividualProvidedDetails,
    name: String,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingSaUtr,
      name = "saUtr",
      expectedRedirect = Some(AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.show(linkId).url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingNino,
      name = "nino",
      expectedRedirect = Some(AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.show(linkId).url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingDateOfBirth,
      name = "date of birth",
      expectedRedirect = Some(AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualDateOfBirthController.show(linkId).url)
    )
  ).foreach: testCase =>
    testCase.expectedRedirect match
      case None =>
        s"GET $path with ${testCase.name} should return 200 and render page" in:
          ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
            agentApplication,
            testCase.providedDetails
          )
          val response: WSResponse = get(path)
          response.status shouldBe Status.OK
          val doc = response.parseBodyAsJsoupDocument
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
      case Some(expectedRedirect) =>
        s"GET $path with missing ${testCase.name} should redirect to the ${testCase.name} page" in:
          ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
            agentApplication,
            testCase.providedDetails
          )
          val response: WSResponse = get(path)
          response.status shouldBe Status.SEE_OTHER
          response.header("Location").value shouldBe expectedRedirect
