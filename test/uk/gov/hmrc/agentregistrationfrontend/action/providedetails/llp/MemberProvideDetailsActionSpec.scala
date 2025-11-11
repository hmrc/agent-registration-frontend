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
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.agentApplicationId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll.tdAll.memberProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.IndividualAuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs

import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.llp.AgentRegistrationMemberProvidedDetailsStubs

import scala.concurrent.Future

class MemberProvideDetailsActionSpec
extends ISpec:

  def fakeResultF: Future[Result] = fail("this should not be executed if test works correctly")

  "when user is logged in with agentApplicationId in session but no member provided details found  action redirects to provided details start page no linkId" in:
    IndividualAuthStubs.stubAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.stubGetMemberProvidedDetailsNoContent(agentApplicationId)

    val individualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val result: Result = Ok("AllGood")

    individualAuthorisedAction
      .invokeBlock(
        tdAll.requestLoggedIn,
        (r: IndividualAuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            result
          }
      ).futureValue

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    provideDetailsAction
      .invokeBlock(
        tdAll.individualAuthorisedRequestLoggedInWithAgentApplicationId,
        (r: MemberProvideDetailsRequest[?]) => fakeResultF
      ).futureValue shouldBe Redirect(
      """/agent-registration/provide-details/start-no-linkId"""
    )

    IndividualAuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)

  "when user is logged in without agentApplicationId in session but no member provided details found  action redirects to provided details start page no linkId" in:
    IndividualAuthStubs.stubAuthorise()

    val individualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val result: Result = Ok("AllGood")

    individualAuthorisedAction
      .invokeBlock(
        tdAll.requestLoggedIn,
        (r: IndividualAuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            result
          }
      ).futureValue

    val provideDetailsAction = app.injector.instanceOf[ProvideDetailsAction]

    assertThrows[IllegalStateException] {
      provideDetailsAction
        .invokeBlock(
          tdAll.individualAuthorisedRequestLoggedInWithOutAgentApplicationId,
          (_: MemberProvideDetailsRequest[?]) => fakeResultF
        ).futureValue
    }

    IndividualAuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId, 0)

  "when user is logged with agentApplicationId in session and member provided details found than redirects to name page" in:
    IndividualAuthStubs.stubAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs
      .stubFindMemberProvidedDetailsByApplicationId(memberProvidedDetails)

    val individualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val result: Result = Ok("AllGood")

    individualAuthorisedAction
      .invokeBlock(
        tdAll.requestLoggedIn,
        (r: IndividualAuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            result
          }
      ).futureValue

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

    IndividualAuthStubs.verifyAuthorise()
    AgentRegistrationMemberProvidedDetailsStubs.verifyFindByAgentApplicationID(agentApplicationId)
