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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import com.softwaremill.quicklens.modify
import play.api.libs.ws.WSBodyReadables.readableAsString
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.{AgentApplication, AgentApplicationLlp, ApplicationState}
import uk.gov.hmrc.agentregistrationfrontend.forms.individual.NameMatchingForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TestOnlyData.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class NameMatchingControllerSpec
  extends ControllerSpec:

  private val linkId = tdAll.linkId

  val completeAgentApplication: AgentApplication =
    tdAll
      .agentApplicationLlp
      .sectionContactDetails
      .afterEmailAddressVerified
      .modify(_.applicationState)
      .setTo(ApplicationState.Submitted)

  object providedDetails:
    val providedDetails: IndividualProvidedDetails =
      tdAll
        .providedDetails
        .afterStarted

  val listOfAgentProvidedDetails: List[IndividualProvidedDetails] =
    List(
      individualProvidedDetails,
      individualProvidedDetails2,
      individualProvidedDetails3
    )

  private val path = s"/agent-registration/provide-details/individual-name-search/${linkId.value}"

  private object individualProvideDetails:
    val afterNinoNotProvided: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoNotProvided

  "NameMatchingController should have the correct routes" in :
    AppRoutes.providedetails.NameMatchingController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.providedetails.NameMatchingController.submit(linkId) shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.providedetails.NameMatchingController.submit(linkId).url shouldBe
      AppRoutes.providedetails.NameMatchingController.show(linkId).url

  s"GET $path should return 200 and render page when Nino is not provided in HMRC systems" in :
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(
      listOfAgentProvidedDetails,
      agentApplicationId
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Enter the name you provided to your agent for your application - Apply for an agent services account - GOV.UK"

  s"GET $path should redirect to the exit page when there is no application for the linkId" in :
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkIdNoContent(
      linkId = linkId
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(
      listOfAgentProvidedDetails,
      agentApplicationId
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER

  s"POST $path with a agent provided name should send the user to the potential match confirmation page" in :
    val testAgentProvidedName = "Test Name"
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(
      listOfAgentProvidedDetails,
      agentApplicationId
    )

    val response: WSResponse =
      post(path)(Map(
        NameMatchingForm.nameSearchKey -> Seq(testAgentProvidedName)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.IndividualConfirmationController.show(linkId).url

  s"POST $path with an incorrectly formatted name should show the page with errors" in :
    val testAgentProvidedName = "Test///Name"
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(
      listOfAgentProvidedDetails,
      agentApplicationId
    )

    val response: WSResponse =
      post(path)(Map(
        NameMatchingForm.nameSearchKey -> Seq(testAgentProvidedName)
      ))

    response.status shouldBe Status.BAD_REQUEST

  s"POST $path with a name which has not been provided by the agent should redirect to the contact page" in :
    val NotPresentName = "Bob Boson"
    AuthStubs.stubAuthoriseIndividual()
    AgentRegistrationStubs.stubFindApplicationByLinkId(
      linkId = linkId,
      agentApplication = completeAgentApplication
    )
    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(
      listOfAgentProvidedDetails,
      agentApplicationId
    )

    val response: WSResponse =
      post(path)(Map(
        NameMatchingForm.nameSearchKey -> Seq(NotPresentName)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.providedetails.ExitController.genericExitPage.url
