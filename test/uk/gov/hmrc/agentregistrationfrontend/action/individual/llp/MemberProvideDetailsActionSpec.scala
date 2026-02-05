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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentregistrationfrontend.action.individual.llp
//
//import com.softwaremill.quicklens.modify
//import play.api.mvc.Result
//import play.api.mvc.Results.*
//import uk.gov.hmrc.agentregistration.shared.AgentApplication
//import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsId
//import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
//import uk.gov.hmrc.agentregistration.shared.ApplicationState
//import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualProvideDetailsRefiner
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.agentApplicationId
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.individualProvidedDetails
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationIndividualProvidedDetailsStubs
//import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.*
//
//import scala.concurrent.Future
//

//TODO: this will be rewritten when we introduce IndividualProvideDetails with optional InternalUserId. Levaing commented out for now.
//class MemberProvideDetailsActionSpec
//extends ISpec:
//
//  val submittedAgentApplication: AgentApplication = tdAll
//    .agentApplicationLlp
//    .sectionContactDetails
//    .afterEmailAddressVerified
//    .modify(_.applicationState)
//    .setTo(ApplicationState.Submitted)
//
//  val inProgressAgentApplication: AgentApplication = tdAll
//    .agentApplicationLlp
//    .sectionContactDetails
//    .afterEmailAddressVerified
//    .modify(_.applicationState)
//    .setTo(ApplicationState.Started)
//
////TODO: explain how it can come that we have agentApplicationId in session?
//  "when agentApplicationId in session and individual provided details found than redirects to name page" in:
//    AgentRegistrationIndividualProvidedDetailsStubs
//      .stubFindIndividualProvidedDetails(individualProvidedDetails)
//
//    val refiner = app.injector.instanceOf[IndividualProvideDetailsRefiner]
//
//    refiner
//      .refineIntoRequestWithIndividualProvidedDetails(tdAll.requestLoggedIn)
//      .futureValue
//      .value
//      .data shouldBe (individualProvidedDetails)
//
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)
//
//  // TODO: explain how it can come that we have agentApplicationId in session?
//  "when agentApplicationId in session but no individual provided details found but application found - try to recover - find linkId and start journey" in:
//    AgentRegistrationIndividualProvidedDetailsStubs.stubFindIndividualProvidedDetailsNoContent(agentApplicationId)
//    AgentRegistrationStubs.stubFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId, submittedAgentApplication)
//
//    val refiner = app.injector.instanceOf[IndividualProvideDetailsRefiner]
//
//    refiner
//      .refineIntoRequestWithIndividualProvidedDetails(tdAll
//        .IndividualRequests
//        .rawRequestWithAgentApplicationId)
//      .futureValue
//      .left
//      .value shouldBe Redirect(
//      s"""/agent-registration/provide-details/internal/initiate-member-provide-details/${submittedAgentApplication.linkId.value}"""
//    )
//
//    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId)
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)
//
//  // TODO: explain how it can come that we have agentApplicationId in session?
//  "when agentApplicationId in session but no individual provided details found and no application found - try to recover will fail with error page" in:
//    AgentRegistrationIndividualProvidedDetailsStubs.stubFindIndividualProvidedDetailsNoContent(agentApplicationId)
//    AgentRegistrationStubs.stubFindApplicationByAgentApplicationIdNoContent(submittedAgentApplication.agentApplicationId)
//
//    val refiner: IndividualProvideDetailsRefiner = app.injector.instanceOf[IndividualProvideDetailsRefiner]
//
//    refiner.refineIntoRequestWithIndividualProvidedDetails(
//        tdAll.individualAuthorisedRequestLoggedInWithAgentApplicationId,
//        (r: IndividualProvideDetailsRequest[?]) => fakeResultF
//      ).futureValue shouldBe Redirect(
//      s"""/agent-registration/apply/exit"""
//    )
//
//    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId)
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)
//
//  "when agentApplicationId in session and no individual provided details found action recover - failed - redirect error page" in:
//    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetailsNoContent()
//
//    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]
//
//    provideDetailsAction
//      .invokeBlock(
//        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
//        (_: IndividualProvideDetailsRequest[?]) => fakeResultF
//      ).futureValue shouldBe Redirect(
//      """/agent-registration/provide-details/exit"""
//    )
//
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
//
//  "when agentApplicationId in session and one individual provided details found - try recover - success - redirect next page" in:
//    AgentRegistrationIndividualProvidedDetailsStubs.stubFindAllIndividualProvidedDetails(List(individualProvidedDetails))
//
//    val result: Result = Ok("AllGood")
//    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]
//
//    provideDetailsAction
//      .invokeBlock(
//        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
//        (r: IndividualProvideDetailsRequest[?]) =>
//          Future.successful {
//            r.internalUserId shouldBe tdAll.internalUserId
//            r.individualProvidedDetails shouldBe individualProvidedDetails
//            result
//          }
//      ).futureValue shouldBe result
//
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
//
//  "when agentApplicationId in session and more than one individual provided details found action recover - failed - redirect error page" in:
//    AgentRegistrationIndividualProvidedDetailsStubs
//      .stubFindAllIndividualProvidedDetails(
//        List(
//          individualProvidedDetails,
//          individualProvidedDetails.copy(
//            _id = IndividualProvidedDetailsId("member-provided-details-id-67890"),
//            agentApplicationId = AgentApplicationId(value = "another-agent-application-id")
//          )
//        )
//      )
//
//    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]
//
//    provideDetailsAction
//      .invokeBlock(
//        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
//        (_: IndividualProvideDetailsRequest[?]) => fakeResultF
//      ).futureValue shouldBe Redirect(
//      """/agent-registration/provide-details/multiple-provided-details"""
//    )
//
//    AgentRegistrationIndividualProvidedDetailsStubs.verifyFind()
