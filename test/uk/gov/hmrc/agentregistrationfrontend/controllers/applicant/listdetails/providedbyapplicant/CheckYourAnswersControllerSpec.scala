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

import com.softwaremill.quicklens.modify
import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/list-details/provide-details/check-your-answers"

  "route should have correct paths and method" in:
    AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.submit.url shouldBe AppRoutes.apply.listdetails.providedbyapplicant.CheckYourAnswersController.show.url

  val agentApplication: AgentApplication = tdAll.agentApplicationLimitedPartnership.afterStarted
  val individualName: IndividualName = tdAll.providedDetails.afterAccessConfirmed.individualName
  val individualNameValue: String = individualName.value
  val providedByApplicantSessionStore: ProvidedByApplicantSessionStore = app.injector.instanceOf[ProvidedByApplicantSessionStore]
  override def afterEach(): Unit =
    providedByApplicantSessionStore.delete().futureValue
    super.afterEach()

  object providedByApplicant:

    val complete: ProvidedByApplicant = ProvidedByApplicant(
      individualProvidedDetailsId = tdAll.providedDetails.afterAccessConfirmed._id,
      individualName = individualName,
      individualDateOfBirth = Some(tdAll.dateOfBirthProvided),
      telephoneNumber = Some(tdAll.telephoneNumber),
      emailAddress = Some(tdAll.individualEmailAddress),
      individualNino = Some(tdAll.ninoProvided),
      individualSaUtr = Some(tdAll.saUtrProvided)
    )
    val missingDateOfBirth: ProvidedByApplicant = complete.copy(individualDateOfBirth = None)
    val missingTelephoneNumber: ProvidedByApplicant = complete.copy(telephoneNumber = None)
    val missingEmailAddress: ProvidedByApplicant = complete.copy(emailAddress = None)
    val missingNino: ProvidedByApplicant = complete.copy(individualNino = None)
    val missingSaUtr: ProvidedByApplicant = complete.copy(individualSaUtr = None)

  private final case class TestCaseForCya(
    providedByApplicant: ProvidedByApplicant,
    testCaseName: String,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      providedByApplicant = providedByApplicant.complete,
      testCaseName = "complete details"
    ),
    TestCaseForCya(
      providedByApplicant = providedByApplicant.missingDateOfBirth,
      testCaseName = "date of birth",
      expectedRedirect = Some(AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show.url)
    ),
    TestCaseForCya(
      providedByApplicant = providedByApplicant.missingTelephoneNumber,
      testCaseName = "telephone number",
      expectedRedirect = Some(AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show.url)
    ),
    TestCaseForCya(
      providedByApplicant = providedByApplicant.missingEmailAddress,
      testCaseName = "email address",
      expectedRedirect = Some(AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show.url)
    ),
    TestCaseForCya(
      providedByApplicant = providedByApplicant.missingNino,
      testCaseName = "National Insurance number",
      expectedRedirect = Some(AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show.url)
    ),
    TestCaseForCya(
      providedByApplicant = providedByApplicant.missingSaUtr,
      testCaseName = "SA UTR",
      expectedRedirect = Some(AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show.url)
    )
  ).foreach: testCase =>
    testCase.expectedRedirect match
      case None =>
        s"GET $path with complete details should return 200 and render page" in:
          ApplyStubHelper.stubsForAuthAction(agentApplication)
          AgentRegistrationStubs.stubFindIndividualForApplication(tdAll.providedDetails.afterAccessConfirmed)
          providedByApplicantSessionStore.upsert(testCase.providedByApplicant).futureValue
          val response: WSResponse = get(path)

          response.status shouldBe Status.OK
          val doc = response.parseBodyAsJsoupDocument
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
          doc.select(captionL).text() shouldBe "Relevant individual details"
          ApplyStubHelper.verifyConnectorsForAuthAction()
          AgentRegistrationStubs.verifyFindIndividualForApplication(tdAll.providedDetails.afterAccessConfirmed._id)

      case Some(expectedRedirect) =>
        s"GET $path with missing ${testCase.testCaseName} should redirect to the ${testCase.testCaseName} page" in:
          ApplyStubHelper.stubsForAuthAction(agentApplication)
          providedByApplicantSessionStore.upsert(testCase.providedByApplicant).futureValue
          val response: WSResponse = get(path)

          response.status shouldBe Status.SEE_OTHER
          response.body[String] shouldBe Constants.EMPTY_STRING
          response.header("Location").value shouldBe expectedRedirect
          ApplyStubHelper.verifyConnectorsForAuthAction()
          // ensure we don't call the find individual endpoint when we don't have all the details needed to show the CYA page
          AgentRegistrationStubs.verifyFindIndividualForApplication(tdAll.providedDetails.afterAccessConfirmed._id, 0)

  s"POST $path should update the individual record and redirect to the progress tracker page" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication)
    AgentRegistrationStubs.stubFindIndividualForApplication(tdAll.providedDetails.afterAccessConfirmed)
    providedByApplicantSessionStore.upsert(providedByApplicant.complete).futureValue
    AgentRegistrationStubs.stubUpsertIndividualProvidedDetails(tdAll.providedDetails.afterAccessConfirmed
      .modify(_.individualDateOfBirth)
      .setTo(providedByApplicant.complete.individualDateOfBirth)
      .modify(_.telephoneNumber)
      .setTo(providedByApplicant.complete.telephoneNumber)
      .modify(_.emailAddress)
      .setTo(Some(IndividualVerifiedEmailAddress(
        emailAddress = providedByApplicant.complete.getEmailAddress,
        isVerified = false
      )))
      .modify(_.individualNino)
      .setTo(providedByApplicant.complete.individualNino)
      .modify(_.individualSaUtr)
      .setTo(providedByApplicant.complete.individualSaUtr)
      .modify(_.passedIv)
      .setTo(Some(false))
      .modify(_.providedByApplicant)
      .setTo(Some(true))
      .modify(_.vrns)
      .setTo(Some(List.empty)) // no call can be made on individual's behalf so empty list
      .modify(_.payeRefs)
      .setTo(Some(List.empty))
      .modify(_.providedDetailsState)
      .setTo(Finished))
    val response: WSResponse =
      post(path)(Map(
        "submit" -> Seq("Confirm and continue")
      ))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe Constants.EMPTY_STRING
    response.header("Location").value shouldBe AppRoutes.apply.listdetails.progress.CheckProgressController.show.url
    ApplyStubHelper.verifyConnectorsForAuthAction()
    AgentRegistrationStubs.verifyFindIndividualForApplication(tdAll.providedDetails.afterAccessConfirmed._id)
