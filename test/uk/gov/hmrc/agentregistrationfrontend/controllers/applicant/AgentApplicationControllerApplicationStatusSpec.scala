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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

/** The application status endpoint answers with a different page for each state the application can be in, so it gets a spec of its own - one test case per
  * state.
  */
class AgentApplicationControllerApplicationStatusSpec
extends ControllerSpec:

  private val applicationStatusPath: String = "/agent-registration/application-status"

  enum ExpectedResponse:

    case RendersPageTitled(title: String)
    case RedirectsTo(url: String)

  import ExpectedResponse.*

  final case class TestCase(
    description: String,
    application: AgentApplicationLlp,
    individuals: List[IndividualProvidedDetails],
    expectedResponse: ExpectedResponse
  )

  Seq(
    TestCase(
      description = "render the confirmation page when the application has been sent for risking",
      application = tdAll.agentApplicationLlp.afterDeclarationSubmitted,
      individuals = List(tdAll.providedDetails.afterFinished),
      expectedResponse = RendersPageTitled("You’ve applied for an agent services account - Apply for an agent services account - GOV.UK")
    ),
    TestCase(
      description = "render the resubmission confirmation page when a resubmitted application has been sent for risking",
      application = tdAll.agentApplicationLlp.afterResubmitted,
      individuals = List(tdAll.providedDetails.afterFinished),
      expectedResponse = RendersPageTitled("You have resubmitted your application for an agent services account - Apply for an agent services account - GOV.UK")
    ),
    TestCase(
      description = "render the in-progress page when the application has been sent to Minerva",
      application = tdAll.agentApplicationLlp.afterSentToMinerva,
      individuals = List(tdAll.providedDetails.afterFinished),
      expectedResponse = RendersPageTitled(
        s"Application reference: ${tdAll.agentApplicationLlp.afterSentToMinerva.applicationReference.value} - Apply for an agent services account - GOV.UK"
      )
    ),
    TestCase(
      description = "render the resubmission confirmation page when a resubmitted application has been sent to Minerva",
      application = tdAll.agentApplicationLlp.afterResubmittedSentToMinerva,
      individuals = List(tdAll.providedDetails.afterFinished),
      expectedResponse = RendersPageTitled("You have resubmitted your application for an agent services account - Apply for an agent services account - GOV.UK")
    ),
    TestCase(
      description = "render the fixable failures start page when risking completed with a fixable failure",
      application = tdAll.agentApplicationLlp.afterRiskingCompletedFixable,
      individuals = List(tdAll.providedDetails.afterRiskedFixable),
      expectedResponse = RendersPageTitled("Test Company Name does not meet the registration conditions yet - Apply for an agent services account - GOV.UK")
    ),
    TestCase(
      description = "render the failed non-fixable page when risking completed with a non-fixable failure",
      application = tdAll.agentApplicationLlp.afterRiskingCompletedNonFixable,
      individuals = List(tdAll.providedDetails.afterRiskedNonFixable),
      expectedResponse = RendersPageTitled("Test Company Name does not meet the registration conditions - Apply for an agent services account - GOV.UK")
    ),
    TestCase(
      description = "redirect to the ASA dashboard when risking completed and the application was approved",
      application = tdAll.agentApplicationLlp.afterRiskingCompletedApproved,
      individuals = List(tdAll.providedDetails.afterFinished),
      expectedResponse = RedirectsTo("http://localhost:9401/agent-services-account/home")
    )
  ).foreach: testCase =>
    s"GET $applicationStatusPath should ${testCase.description}" in:
      ApplyStubHelper.stubsForApplicationBprAndIndividuals(
        application = testCase.application,
        individuals = testCase.individuals
      )

      val response: WSResponse = get(applicationStatusPath)

      testCase.expectedResponse match
        case RendersPageTitled(title: String) =>
          response.status shouldBe Status.OK
          response.parseBodyAsJsoupDocument.title() shouldBe title
        case RedirectsTo(url: String) =>
          response.status shouldBe Status.SEE_OTHER
          response.header("Location").value shouldBe url

      ApplyStubHelper.verifyConnectorsToSupplyBprToPage()

  s"GET $applicationStatusPath should show the risking failures of each individual on the failed non-fixable page" in:
    val individual: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedNonFixable
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = tdAll.agentApplicationLlp.afterRiskingCompletedNonFixable,
      individuals = List(individual)
    )

    val response: WSResponse = get(applicationStatusPath)

    response.status shouldBe Status.OK
    // only the controller can put these on the page - it builds the RiskedIndividual list from each
    // individual's own risking outcome
    response
      .parseBodyAsJsoupDocument
      .selectOrFail(s"#${individual.personReference.value}-reasons")
      .text() shouldBe "Records indicate that Test Name is actively disqualified on Companies house."
    ApplyStubHelper.verifyConnectorsToSupplyBprToPage()
