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
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr.NotProvided
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.forms.applicant.providedbyapplicant.SaUtrForm
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class SaUtrControllerSpec
extends ControllerSpec:

  private val path = s"/agent-registration/apply/list-details/provide-details/self-assessment-unique-taxpayer-reference"

  val agentApplication: AgentApplication = tdAll.agentApplicationLimitedPartnership.afterStarted

  val individualName: String = tdAll.providedDetails.afterAccessConfirmed.individualName.value

  object providedByApplicant:

    val beforeSaUtr: ProvidedByApplicant = ProvidedByApplicant(
      individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
      individualName = tdAll.providedDetails.afterAccessConfirmed.individualName,
      individualDateOfBirth = Some(tdAll.dateOfBirthProvided),
      telephoneNumber = Some(tdAll.telephoneNumber),
      emailAddress = Some(tdAll.individualEmailAddress),
      individualNino = Some(tdAll.ninoProvided),
      individualSaUtr = None
    )
    val afterSaUtr: ProvidedByApplicant = beforeSaUtr.copy(individualSaUtr = Some(tdAll.saUtrProvided))
    val afterNoSaUtr: ProvidedByApplicant = beforeSaUtr.copy(individualSaUtr = Some(NotProvided))

  override def afterEach(): Unit =
    providedByApplicantSessionStore.delete().futureValue
    super.afterEach()

  val providedByApplicantSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]

  "routes should have correct paths and methods" in:
    AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.submit.url shouldBe AppRoutes
      .apply.listdetails.providedbyapplicant.SaUtrController.show.url

  s"GET $path should return 200 and render page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe s"Do you know $individualName’s Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path should return 303 and redirect to the select individual page when there is nothing in session" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.find().futureValue shouldBe None withClue "ProvidedByApplicant session store should be empty for this scenario"

    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should redirect to the CYA controller for navigation when 'yes' has been selected and a valid SaUtr has been provided" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue
    val response: WSResponse =
      post(path)(Map(
        SaUtrForm.hasSaUtrKey -> Seq("Yes"),
        SaUtrForm.saUtrKey -> Seq(tdAll.saUtrProvided.saUtr.value)
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url
    providedByApplicantSessionStore.find().futureValue.value shouldBe providedByApplicant.afterSaUtr
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path without selecting an option should return 400" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue
    val response: WSResponse =
      post(path)(Map(
        SaUtrForm.hasSaUtrKey -> Seq(Constants.EMPTY_STRING),
        SaUtrForm.saUtrKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument

    doc.title() shouldBe s"Error: Do you know $individualName’s Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(
      s"${SaUtrForm.hasSaUtrKey}-error"
    ).text() shouldBe "Error: Select yes if you know their Self Assessment Unique Taxpayer Reference"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should redirect to the CYA controller for navigation when 'No' has been selected" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue
    val response: WSResponse =
      post(path)(Map(
        SaUtrForm.hasSaUtrKey -> Seq("No"),
        SaUtrForm.saUtrKey -> Seq("//")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url
    providedByApplicantSessionStore.find().futureValue.value shouldBe providedByApplicant.afterNoSaUtr
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should return a 400 when 'yes' has been selected and an invalid SaUtr has been provided" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue

    val response: WSResponse =
      post(path)(Map(
        SaUtrForm.hasSaUtrKey -> Seq("Yes"),
        SaUtrForm.saUtrKey -> Seq("//")
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: Do you know $individualName’s Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(
      s"${SaUtrForm.saUtrKey}-error"
    ).text() shouldBe s"Error: Enter a Self Assessment Unique Taxpayer Reference in the correct format"
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"POST $path should return a 400 when 'Yes' has been selected and no SaUtr has been provided" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    providedByApplicantSessionStore.upsert(providedByApplicant.beforeSaUtr).futureValue

    val response: WSResponse =
      post(path)(Map(
        SaUtrForm.hasSaUtrKey -> Seq("Yes"),
        SaUtrForm.saUtrKey -> Seq(Constants.EMPTY_STRING)
      ))

    response.status shouldBe Status.BAD_REQUEST
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe s"Error: Do you know $individualName’s Self Assessment Unique Taxpayer Reference? - Apply for an agent services account - GOV.UK"
    doc.mainContent.getElementById(s"${SaUtrForm.saUtrKey}-error").text() shouldBe "Error: Enter the Self Assessment Unique Taxpayer Reference"
    ApplyStubHelper.verifyConnectorsForAuthAction()
