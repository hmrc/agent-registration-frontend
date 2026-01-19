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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/provide-details/check-your-answers"

  "route should have correct path and method" in:
    AppRoutes.providedetails.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )

  private object individualProvideDetails:

    val complete = tdAll.providedDetailsLlp.afterHmrcStandardforAgentsAgreed
    val completeAndConfirmed = tdAll.providedDetailsLlp.afterProvidedDetailsConfirmed

    val missingAgreeStandarts = tdAll.providedDetailsLlp.afterApproveAgentApplication
    val missingApproveApplication = tdAll.providedDetailsLlp.AfterSaUtr.afterSaUtrProvided
    val missingSaUtr = tdAll.providedDetailsLlp.AfterNino.afterNinoProvided
    val missingNino = tdAll.providedDetailsLlp.afterEmailAddressVerified
    val missingEmail = tdAll.providedDetailsLlp.afterTelephoneNumberProvided
    val missingEmailValidation = tdAll.providedDetailsLlp.afterEmailAddressProvided
    val missingTelephone = tdAll.providedDetailsLlp.afterOfficerChosen
    val missingCompaniesHouseOfficerSelection = tdAll.providedDetailsLlp.afterNameQueryProvided
    val missingName = tdAll.providedDetailsLlp.afterStarted

  private final case class TestCaseForCya(
    providedDetails: IndividualProvidedDetails,
    name: String,
    expectedRedirect: Option[String] = None
  )

  List(
    TestCaseForCya(
      providedDetails = individualProvideDetails.completeAndConfirmed,
      name = "Confirmed member details",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualConfirmationController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.complete,
      name = "complete agent details"
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingAgreeStandarts,
      name = "agree to standards of agents",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualHmrcStandardForAgentsController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingApproveApplication,
      name = "approve applicant",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualApproveApplicantController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingSaUtr,
      name = "saUtr",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualSaUtrController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingNino,
      name = "nino",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualNinoController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingEmailValidation,
      name = "email address validation",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualEmailAddressController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingEmail,
      name = "email address",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualEmailAddressController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingTelephone,
      name = "telephone number",
      expectedRedirect = Some(AppRoutes.providedetails.IndividualTelephoneNumberController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingCompaniesHouseOfficerSelection,
      name = "name selection",
      expectedRedirect = Some(AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url)
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.missingName,
      name = "name",
      expectedRedirect = Some(AppRoutes.providedetails.CompaniesHouseNameQueryController.show.url)
    )
  ).foreach: testCase =>
    testCase.expectedRedirect match
      case None =>
        s"GET $path with ${testCase.name} should return 200 and render page" in:
          AuthStubs.stubAuthoriseIndividual()
          AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(testCase.providedDetails))

          val response: WSResponse = get(path)

          response.status shouldBe Status.OK
          val doc = response.parseBodyAsJsoupDocument
          doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"
          doc.select("h2.govuk-caption-l").text() shouldBe "LLP member confirmation"
          AuthStubs.verifyAuthorise()
          AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
      case Some(expectedRedirect) =>
        s"GET $path with missing ${testCase.name} should redirect to the ${testCase.name} page" in:
          AuthStubs.stubAuthoriseIndividual()
          AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(testCase.providedDetails))

          val response: WSResponse = get(path)

          response.status shouldBe Status.SEE_OTHER
          response.header("Location").value shouldBe expectedRedirect
          AuthStubs.verifyAuthorise()
          AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
