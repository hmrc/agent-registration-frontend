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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.providedbyapplicant

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdTestOnly
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class SelectIndividualControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/provide-details/select-individual"

  val testSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  private val agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.submit shouldBe Call(
      method = "POST",
      url = path
    )

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = List(
        tdAll.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.precreated
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Which relevant individual do you need to tell us about? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplicationId)

  s"GET $path should redirect to task list if user is sole trader owner" in:
    val soleTraderOwnerApplication = tdAll.agentApplicationSoleTrader.afterContactDetailsComplete
    ApplyStubHelper.stubsForAuthAction(soleTraderOwnerApplication)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(AppRoutes.apply.TaskListController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to the progress status page if there are no individuals yet to provide details" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = List(
        tdAll.providedDetails.afterFinished,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.afterFinished,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterFinished
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(AppRoutes.apply.listdetails.progress.CheckProgressController.show.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplicationId)

  s"POST $path should return 400 if form submitted with no selection" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = List(
        tdAll.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.precreated
      )
    )
    val response: WSResponse = post(path)(Map.empty[String, Seq[String]])

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Which relevant individual do you need to tell us about? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplicationId)

  s"POST $path should store a valid submission and redirect to the next page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = List(
        tdAll.providedDetails.afterAccessConfirmed,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.afterAccessConfirmed,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterAccessConfirmed
      )
    )
    val response: WSResponse =
      post(path)(
        Map("selectIndividual" -> Seq(tdAll.providedDetails.afterAccessConfirmed._id.value))
      )

    testSessionStore.find().futureValue shouldBe Some(
      ProvidedByApplicant(
        individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
        individualName = tdAll.providedDetails.afterAccessConfirmed.individualName
      )
    )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url
