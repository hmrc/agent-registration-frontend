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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.nonincorporated

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.RemoveKeyIndividualForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class RemoveKeyIndividualControllerSpec
extends ControllerSpec:

  private val individualProvidedDetailsId: IndividualProvidedDetailsId = tdAll.individualProvidedDetails.individualProvidedDetailsId

  private val path = s"/agent-registration/apply/list-details/remove-key-individual/${individualProvidedDetailsId.value}"

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.nonincorporated.RemoveKeyIndividualController.show(individualProvidedDetailsId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.RemoveKeyIndividualController.submit(individualProvidedDetailsId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.nonincorporated.RemoveKeyIndividualController.submit(individualProvidedDetailsId).url shouldBe
      AppRoutes.apply.listdetails.nonincorporated.RemoveKeyIndividualController.show(individualProvidedDetailsId).url

  s"GET $path should return 200 and render page for removing selected partner" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.individualProvidedDetails
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Confirm that you want to remove Test Name from the list of partners - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualForApplication(
      individual = tdAll.individualProvidedDetails
    )
    val response: WSResponse = post(path)(Map.empty)

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Confirm that you want to remove Test Name from the list of partners - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${RemoveKeyIndividualForm.key}-error"
    ).text() shouldBe "Error: Select yes if you want to remove Test Name from the list of partners"
    ApplyStubHelper.verifyConnectorsForAuthAction()
