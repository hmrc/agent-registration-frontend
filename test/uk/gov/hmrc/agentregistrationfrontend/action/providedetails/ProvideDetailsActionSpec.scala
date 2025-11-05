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

package uk.gov.hmrc.agentregistrationfrontend.action.providedetails

import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs

import scala.concurrent.Future

class ProvideDetailsActionSpec
extends ISpec:

  "when individual User is not logged in (request comes without authorisation in the session) action redirects to login url" in:
    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.requestNotLoggedIn

    individualAuthorisedAction(tdAll.linkId)
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .futureValue shouldBe Redirect(
      s"""http://localhost:22201/agent-registration/provide-details/start/${tdAll.linkId.value}"""
    )
    IndividualAuthStubs.verifyAuthorise(0)

//  "AffinityGroup must be Individual or else the action returns Unahtorised View" in:
//    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
//    IndividualAuthStubs.stubAuthorise(
//      responseBody =
//        // language=JSON
//        s"""
//           |{
//           |  "authorisedEnrolments": [],
//           |  "allEnrolments": [],
//           |  "agentInformation": {},
//           |  "internalId": "${tdAll.internalUserId.value}",
//           |  "optionalCredentials": {"providerId":"cred-id-12345","providerType":"GovernmentGateway"}
//           |}
//           |""".stripMargin
//    )
//
//    val resultF = individualAuthorisedAction(tdAll.linkId).invokeBlock(tdAll.requestLoggedIn, _ => fakeResultF)
//    contentAsString(resultF) should include("unauthorised.heading")
//    status(resultF) shouldBe Status.UNAUTHORIZED
//    IndividualAuthStubs.verifyAuthorise()

  "successfully authorise when user is logged in, credentialRole is User/Admin, and no active HMRC-AS-AGENT enrolment" in:
    val individualAuthorisedAction: IndividualAuthorisedAction = app.injector.instanceOf[IndividualAuthorisedAction]
    IndividualAuthStubs.stubAuthorise()
    val result: Result = Ok("AllGood")
    individualAuthorisedAction(tdAll.linkId)
      .invokeBlock(
        tdAll.requestLoggedIn,
        (r: IndividualAuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            result
          }
      )
      .futureValue shouldBe result
    IndividualAuthStubs.verifyAuthorise()

  def fakeResultF: Future[Result] = fail("this should not be executed if test works fine")
