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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class IndividualHmrcStandardForAgentsControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/agree-standard"

  "route should have correct path and method" in:
    AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.submit shouldBe Call(
      method = "POST",
      url = path
    )
  AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.submit.url shouldBe AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show.url

  object individualProvidedDetails:

    val beforeTermsAgreed: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.afterApproveAgentApplication

    val afterTermsAgreed: IndividualProvidedDetailsToBeDeleted = tdAll.providedDetailsLlp.afterHmrcStandardforAgentsAgreed

  s"GET $path before agreeing terms should return 200 and render page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeTermsAgreed))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Agree to meet the HMRC standard for agents - Apply for an agent services account - GOV.UK"
    doc.select("h2.govuk-caption-xl").text() shouldBe "HMRC standard for agents"

  s"GET $path after agreeing terms should return 200 and render page" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.afterTermsAgreed))
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Agree to meet the HMRC standard for agents - Apply for an agent services account - GOV.UK"
    doc.select("h2.govuk-caption-xl").text() shouldBe "HMRC standard for agents"

  s"POST $path with agree should update the application and redirect to the task list" in:
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails.beforeTermsAgreed))
    AgentRegistrationIndividualProvidedDetailsStubs.stubUpsertIndividualProvidedDetails(individualProvidedDetails.afterTermsAgreed)
    val response: WSResponse =
      post(path)(
        Map()
      )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.CheckYourAnswersController.show.url
