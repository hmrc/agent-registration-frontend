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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.TelephoneNumberForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class TelephoneNumberControllerSpec
extends ControllerSpec:

  val providedByApplicantSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]
  override def afterEach(): Unit =
    super.afterEach()
    providedByApplicantSessionStore.delete().futureValue

  private val agentApplication: AgentApplicationLlp = tdAll.agentApplicationLlp.afterConfirmTwoChOfficers
  private val providedByApplicantBase = ProvidedByApplicant(
    individualProvidedDetailsId = IndividualProvidedDetailsId("test-id"),
    individualName = IndividualName("Steve Austin")
  )

  private object providedByApplicant:

    val beforeDateOfBirth: ProvidedByApplicant = providedByApplicantBase
      .copy(individualDateOfBirth = None)
    val beforeTelephoneUpdate: ProvidedByApplicant = providedByApplicantBase
      .copy(individualDateOfBirth = Some(tdAll.dateOfBirthProvided))
    val afterTelephoneNumberProvided: ProvidedByApplicant = beforeTelephoneUpdate
      .copy(telephoneNumber = Some(tdAll.telephoneNumber))

  private val path = s"/agent-registration/apply/list-details/provide-details/telephone-number"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.submit.url shouldBe AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show.url

  s"GET $path should return 200 and render page when session store is as expected" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeTelephoneUpdate).futureValue
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is Steve Austin’s telephone number? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to Select Individual page when no session store found" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with a valid number should save data and redirect to email address page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeTelephoneUpdate).futureValue
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq(tdAll.telephoneNumber.value)
      ))
    providedByApplicantSessionStore.find().futureValue shouldBe Some(providedByApplicant.afterTelephoneNumberProvided)
    response.status shouldBe Status.SEE_OTHER

    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeTelephoneUpdate).futureValue
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${TelephoneNumberForm.key}-error"
    ).text() shouldBe "Error: Enter the number we should call to speak to the individual about this application"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeTelephoneUpdate).futureValue
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${TelephoneNumberForm.key}-error").text() shouldBe "Error: Enter a phone number, like 01632 960 001 or 07700 900 982"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with more than 24 characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeTelephoneUpdate).futureValue
    val response: WSResponse =
      post(path)(Map(
        TelephoneNumberForm.key -> Seq("2".repeat(25))
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s telephone number? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${TelephoneNumberForm.key}-error").text() shouldBe "Error: The phone number must be 24 characters or fewer"
    ApplyStubHelper.verifyConnectorsForAuthAction()
