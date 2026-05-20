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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdTestOnly
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import play.api.libs.ws.WSBodyReadables.readableAsString

class SelectIndividualControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/provide-details/select-individual"

  val providedByApplicantSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]

  object agentApplication:

    val afterHowManyKeyIndividuals: AgentApplicationGeneralPartnership =
      tdAll
        .agentApplicationGeneralPartnership
        .afterHowManyKeyIndividuals

  private val agentApplicationId = agentApplication.afterHowManyKeyIndividuals.agentApplicationId

  val completeIndividualDetails: ProvidedByApplicant = ProvidedByApplicant(
    individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
    individualName = tdAll.providedDetails.afterAccessConfirmed.individualName,
    individualDateOfBirth = Some(tdAll.dateOfBirthProvided),
    telephoneNumber = Some(tdAll.telephoneNumber),
    emailAddress = Some(tdAll.individualEmailAddress),
    individualNino = Some(tdAll.ninoProvided),
    individualSaUtr = Some(tdAll.saUtrProvided)
  )

  val listOfIndividuals: List[IndividualProvidedDetails] = List(
    tdAll.providedDetails.afterAccessConfirmed,
    TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
    TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.precreated
  )

  override def afterEach(): Unit =
    providedByApplicantSessionStore.delete().futureValue
    super.afterEach()

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
        tdAll.providedDetails.afterAccessConfirmed,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.precreated
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Which relevant individual do you need to tell us about? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplicationId)

  s"GET $path should show the page correctly and pre-populate if there is data in session" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = List(
        tdAll.providedDetails.afterAccessConfirmed,
        TdTestOnly.additionalIndividuals.secondIndividual.providedDetails.precreated,
        TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.precreated
      )
    )
    providedByApplicantSessionStore.upsert(completeIndividualDetails).futureValue
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Which relevant individual do you need to tell us about? - Apply for an agent services account - GOV.UK"
    response.body should include("Test Name")
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
      individuals = listOfIndividuals
    )
    val response: WSResponse = post(path)(Map.empty[String, Seq[String]])

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Which relevant individual do you need to tell us about? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualsForApplication(agentApplicationId)

  s"POST $path should store the selected users details and redirect to the next page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = listOfIndividuals
    )
    val response: WSResponse =
      post(path)(
        Map("selectIndividual" -> Seq(tdAll.providedDetails.afterAccessConfirmed._id.value))
      )

    providedByApplicantSessionStore.find().futureValue shouldBe Some(
      ProvidedByApplicant(
        individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
        individualName = tdAll.providedDetails.afterAccessConfirmed.individualName
      )
    )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url

  s"POST $path should send users to the check your answers page when the selected individual has data in the cache" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = listOfIndividuals
    )
    providedByApplicantSessionStore.upsert(completeIndividualDetails).futureValue

    val response: WSResponse =
      post(path)(
        Map("selectIndividual" -> Seq(tdAll.providedDetails.afterAccessConfirmed._id.value))
      )

    providedByApplicantSessionStore.find().futureValue shouldBe Some(completeIndividualDetails)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url

  s"POST $path should send users to the applicant provided date of birth page when the cache data does not belong to the selected name" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.afterHowManyKeyIndividuals)
    AgentRegistrationStubs.stubFindIndividualsForApplication(
      agentApplicationId = agentApplicationId,
      individuals = listOfIndividuals
    )
    providedByApplicantSessionStore.upsert(completeIndividualDetails).futureValue

    val response: WSResponse =
      post(path)(
        Map("selectIndividual" -> Seq(
          TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterAccessConfirmed._id.value
        ))
      )

    providedByApplicantSessionStore.find().futureValue shouldBe Some(
      ProvidedByApplicant(
        individualProvidedDetailsId = TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterAccessConfirmed._id,
        individualName = TdTestOnly.additionalIndividuals.thirdIndividual.providedDetails.afterAccessConfirmed.individualName
      )
    )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url
