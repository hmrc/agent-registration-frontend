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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.providedbyapplicant

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.ApplicantProvidedNinoForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

class ApplicantProvidedNinoControllerSpec
extends ControllerSpec:

  private val path = s"/agent-registration/apply/list-details/provide-details/national-insurance-number"

  val application: AgentApplication = tdAll.agentApplicationLimitedPartnership.afterStarted

  val name: String = tdAll.providedDetails.afterAccessConfirmed.individualName.value

  val providedByApplicant: ProvidedByApplicant = ProvidedByApplicant(
    individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
    individualName = tdAll.providedDetails.afterAccessConfirmed.individualName,
    individualNino = None
  )

  override def afterEach(): Unit =
    providedByApplicantSessionStore.delete().futureValue
    super.afterEach()

  val providedByApplicantSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.submit.url shouldBe AppRoutes
      .apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show.url

  s"GET $path should return 200 and render page" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"Do you know $name’s National Insurance Number? - Apply for an agent services account - GOV.UK"

  s"GET $path should return 303 and redirect to the select individual page when there is nothing in session" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.find().futureValue shouldBe None withClue "ProvidedByApplicant session store should be empty for this scenario"

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url

  s"POST $path should redirect to the enter your UTR page when 'yes' has been selected and a valid nino has been provided" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue
    val response: WSResponse =
      post(path)(Map(
        ApplicantProvidedNinoForm.hasNino -> Seq("Yes"),
        ApplicantProvidedNinoForm.ninoKey -> Seq(tdAll.ninoProvided.nino.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
    providedByApplicantSessionStore.find().futureValue.value shouldBe
      ProvidedByApplicant(
        individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
        individualName = tdAll.providedDetails.afterAccessConfirmed.individualName,
        individualNino = Some(tdAll.ninoProvided)
      )

  s"POST $path without selecting an option should return 400" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue
    val response: WSResponse =
      post(path)(Map(
        ApplicantProvidedNinoForm.hasNino -> Seq(Constants.EMPTY_STRING),
        ApplicantProvidedNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument

    doc.title() shouldBe s"Error: Do you know $name’s National Insurance Number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(
      s"${ApplicantProvidedNinoForm.hasNino}-error"
    ).text() shouldBe "Error: Select yes if you know their National Insurance number"

  s"POST $path should redirect to the enter your UTR page with no NINO in session when 'no' has been selected" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue
    val response: WSResponse =
      post(path)(Map(
        ApplicantProvidedNinoForm.hasNino -> Seq("No"),
        ApplicantProvidedNinoForm.ninoKey -> Seq("//")
      ))

    response.status shouldBe Status.SEE_OTHER // TODO replace with redirect to APB-11164 UTR Page
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
    providedByApplicantSessionStore.find().futureValue.value shouldBe
      ProvidedByApplicant(
        individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
        individualName = tdAll.providedDetails.afterAccessConfirmed.individualName,
        individualNino = None
      )

  s"POST $path should return a 400 when 'yes' has been selected and an invalid NINO has been provided" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue

    val response: WSResponse =
      post(path)(Map(
        ApplicantProvidedNinoForm.hasNino -> Seq("Yes"),
        ApplicantProvidedNinoForm.ninoKey -> Seq("//")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: Do you know $name’s National Insurance Number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(
      s"${ApplicantProvidedNinoForm.ninoKey}-error"
    ).text() shouldBe s"Error: Enter their National Insurance number in the correct format"

  s"POST $path should return a 400 when 'yes' has been selected and no NINO has been provided" in:
    AuthStubs.stubAuthorise()
    AgentRegistrationStubs.stubGetAgentApplication(application)
    providedByApplicantSessionStore.upsert(providedByApplicant).futureValue

    val response: WSResponse =
      post(path)(Map(
        ApplicantProvidedNinoForm.hasNino -> Seq("Yes"),
        ApplicantProvidedNinoForm.ninoKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: Do you know $name’s National Insurance Number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(s"${ApplicantProvidedNinoForm.ninoKey}-error").text() shouldBe "Error: Enter their National Insurance number"
