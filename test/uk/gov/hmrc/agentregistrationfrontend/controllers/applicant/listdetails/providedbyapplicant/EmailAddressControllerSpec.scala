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
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.EmailAddressForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class EmailAddressControllerSpec
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
    val beforeEmailAddress: ProvidedByApplicant = providedByApplicantBase
      .copy(
        individualDateOfBirth = Some(tdAll.dateOfBirthProvided),
        telephoneNumber = Some(tdAll.telephoneNumber)
      )
    val afterEmailAddress: ProvidedByApplicant = beforeEmailAddress
      .copy(emailAddress = Some(tdAll.individualEmailAddress))

  private val path = s"/agent-registration/apply/list-details/provide-details/email-address"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.submit.url shouldBe AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url

  s"GET $path should return 200 and render page when session store is as expected" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeEmailAddress).futureValue
    val response: WSResponse = get(path)
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What is Steve Austin’s email address? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should redirect to Select Individual page when no session store found" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    val response: WSResponse = get(path)
    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with a valid email address should save data and redirect to nino page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeEmailAddress).futureValue
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq(tdAll.individualEmailAddress.value)
      ))
    providedByApplicantSessionStore.find().futureValue shouldBe Some(providedByApplicant.afterEmailAddress)
    response.status shouldBe Status.SEE_OTHER

    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with blank inputs should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeEmailAddress).futureValue
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s email address? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${EmailAddressForm.key}-error"
    ).text() shouldBe "Error: Enter the email address"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with invalid characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.afterEmailAddress).futureValue
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("[[)(*%")
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s email address? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${EmailAddressForm.key}-error"
    ).text() shouldBe "Error: Enter the email address with a name, @ symbol and a domain name, like yourname@example.com"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path with more than 132 characters should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeEmailAddress).futureValue
    val response: WSResponse =
      post(path)(Map(
        EmailAddressForm.key -> Seq("a".repeat(133))
      ))
    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: What is Steve Austin’s email address? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(s"#${EmailAddressForm.key}-error").text() shouldBe "Error: The email address must be 132 characters or fewer"
    ApplyStubHelper.verifyConnectorsForAuthAction()
