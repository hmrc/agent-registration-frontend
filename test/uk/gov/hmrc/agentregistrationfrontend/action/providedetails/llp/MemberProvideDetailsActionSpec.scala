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

package uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp

import org.scalatest.matchers.should.Matchers.*
import com.softwaremill.quicklens.modify
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.llp.MemberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.agentApplicationId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.memberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

import scala.concurrent.Future

class MemberProvideDetailsActionSpec
extends ISpec:

  def fakeResultF: Future[Result] = fail("this should not be executed if test works correctly")

  val submittedAgentApplication: AgentApplicationLlp = tdAll
    .agentApplicationLlp
    .sectionContactDetails
    .afterEmailAddressVerified
    .modify(_.applicationState)
    .setTo(ApplicationState.Submitted)

  val inProgressAgentApplication: AgentApplicationLlp = tdAll
    .agentApplicationLlp
    .sectionContactDetails
    .afterEmailAddressVerified
    .modify(_.applicationState)
    .setTo(ApplicationState.Started)

  "when agentApplicationId in session and member provided details found than redirects to name page" in:
    AgentRegistrationMemberProvidedDetailsStubs
      .stubFindMemberProvidedDetails(memberProvidedDetails)

    val result: Result = Ok("AllGood")
    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithAgentApplicationId,
        (r: MemberProvideDetailsRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            r.memberProvidedDetails shouldBe memberProvidedDetails
            result
          }
      ).futureValue shouldBe result

    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)

  "when agentApplicationId in session but no member provided details found but application found - try to recover - find linkId and start journey" in:
    AgentRegistrationMemberProvidedDetailsStubs.stubFindMemberProvidedDetailsNoContent(agentApplicationId)
    AgentRegistrationStubs.stubFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId, submittedAgentApplication)

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithAgentApplicationId,
        (r: MemberProvideDetailsRequest[?]) => fakeResultF
      ).futureValue shouldBe Redirect(
      s"""/agent-registration/provide-details/internal/initiate-member-provide-details/${submittedAgentApplication.linkId.value}"""
    )

    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)

  "when agentApplicationId in session but no member provided details found and no application found - try to recover will fail with error page" in:
    AgentRegistrationMemberProvidedDetailsStubs.stubFindMemberProvidedDetailsNoContent(agentApplicationId)
    AgentRegistrationStubs.stubFindApplicationByAgentApplicationIdNoContent(submittedAgentApplication.agentApplicationId)

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithAgentApplicationId,
        (r: MemberProvideDetailsRequest[?]) => fakeResultF
      ).futureValue shouldBe Redirect(
      s"""/agent-registration/apply/exit"""
    )

    AgentRegistrationStubs.verifyFindApplicationByAgentApplicationId(submittedAgentApplication.agentApplicationId)
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)

  "when agentApplicationId in session and no member provided details found action recover - failed - redirect error page" in:
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetailsNoContent()

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
        (_: MemberProvideDetailsRequest[?]) => fakeResultF
      ).futureValue shouldBe Redirect(
      """/agent-registration/provide-details/exit"""
    )

    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()

  "when agentApplicationId in session and one member provided details found - try recover - success - redirect next page" in:
    AgentRegistrationMemberProvidedDetailsStubs.stubFindAllMemberProvidedDetails(List(memberProvidedDetails))

    val result: Result = Ok("AllGood")
    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
        (r: MemberProvideDetailsRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            r.memberProvidedDetails shouldBe memberProvidedDetails
            result
          }
      ).futureValue shouldBe result

    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()

  "when agentApplicationId in session and more than one member provided details found action recover - failed - redirect error page" in:
    AgentRegistrationMemberProvidedDetailsStubs
      .stubFindAllMemberProvidedDetails(
        List(
          memberProvidedDetails,
          memberProvidedDetails.copy(
            _id = MemberProvidedDetailsId("member-provided-details-id-67890"),
            agentApplicationId = AgentApplicationId(value = "another-agent-application-id")
          )
        )
      )

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
        (_: MemberProvideDetailsRequest[?]) => fakeResultF
      ).futureValue shouldBe Redirect(
      """/agent-registration/provide-details/multiple-provided-details"""
    )

    AgentRegistrationMemberProvidedDetailsStubs.verifyFind()
