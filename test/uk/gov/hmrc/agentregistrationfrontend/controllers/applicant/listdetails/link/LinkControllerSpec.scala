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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.link

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class LinkControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/key-individuals/share-link"

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.link.LinkController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.link.LinkController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.link.LinkController.submit.url shouldBe
      AppRoutes.apply.listdetails.link.LinkController.show.url

  s"GET $path should return 200 and render page for sharing the access link for individuals" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Share this link with everyone on the list - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHowManyKeyIndividuals.agentApplicationId)

  s"POST $path should mark all individual provided details records with the AccessConfirmed status and redirect to task list" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId,
      individuals = List(
        tdAll.individualProvidedDetails,
        tdAll.individualProvidedDetails2,
        tdAll.individualProvidedDetails3
      )
    )
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(tdAll.individualProvidedDetails.copy(providedDetailsState =
      ProvidedDetailsState.AccessConfirmed
    ))
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(tdAll.individualProvidedDetails2.copy(providedDetailsState =
      ProvidedDetailsState.AccessConfirmed
    ))
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(tdAll.individualProvidedDetails3.copy(providedDetailsState =
      ProvidedDetailsState.AccessConfirmed
    ))
    val response: WSResponse =
      post(path)(
        Map("submit" -> Seq("SaveAndContinue"))
      )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.TaskListController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplication.afterHowManyKeyIndividuals.agentApplicationId)
    AgentRegistrationStubs.verifyUpsertIndividualProvidedDetails(count = 3) // we should have updated 3 individual provided details records
