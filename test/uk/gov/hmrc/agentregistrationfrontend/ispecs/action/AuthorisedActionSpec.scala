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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.action

import org.scalatest.Assertions.fail
import org.scalatest.funsuite.AnyFunSuiteLike
import play.api.http.Status
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results
import play.api.mvc.Results.Ok
import play.api.mvc.Results.Redirect
import play.api.mvc.Results.Unauthorized
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedAction
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedRequest
import uk.gov.hmrc.agentregistrationfrontend.ispecs.ISpec
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.stubs.AuthStubs
import uk.gov.hmrc.auth.core.InternalError
import uk.gov.hmrc.auth.core.MissingBearerToken
import uk.gov.hmrc.auth.core.UnsupportedCredentialRole
import play.api.test.Helpers.*

import scala.concurrent.Future

class AuthorisedActionSpec
extends ISpec:

  "when User is not logged in (request comes without authorisation in the session) action redirects to login url" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.requestNotLoggedIn
    authorisedAction
      .invokeBlock(notLoggedInRequest, _ => fakeResultF)
      .futureValue shouldBe Redirect(
      """http://localhost:9099/bas-gateway/sign-in?continue_url=http://localhost:22201/&origin=agent-registration-frontend&accountType=agent"""
    )
    AuthStubs.verifyAuthorise(0)

  "Credential role must be User or Admin or else the action returns Unahtorised View" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.request
    val credentialRoleNotUserNorAdmin = "Assistant"
    AuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [],
           |  "credentialRole": "$credentialRoleNotUserNorAdmin",
           |  "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
           |  "agentInformation": {},
           |  "internalId": "${tdAll.internalUserId.value}"
           |}
           |""".stripMargin
    )

    val resultF = authorisedAction.invokeBlock(notLoggedInRequest, _ => fakeResultF)
    contentAsString(resultF) should include("unauthorised.heading")
    status(resultF) shouldBe Status.UNAUTHORIZED
    AuthStubs.verifyAuthorise()

  "active HMRC-AS-AGENT enrolment MUST NOT be assigned to user or else the action redirects to ASA Dashboard" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.request
    AuthStubs.stubAuthorise(
      responseBody =
        // language=JSON
        s"""
           |{
           |  "authorisedEnrolments": [],
           |  "allEnrolments": [
           |    {
           |      "key": "HMRC-AS-AGENT",
           |      "identifiers": [
           |        {
           |          "key": "AgentReferenceNumber",
           |          "value": "GARN6552483"
           |        }
           |      ],
           |      "state": "Activated"
           |    }
           |  ],
           |  "credentialRole": "User",
           |  "groupIdentifier": "3E7R-E0V0-5V4N-Q5S0",
           |  "agentInformation": {},
           |  "internalId": "${tdAll.internalUserId.value}"
           |}
           |""".stripMargin
    )

    val result = authorisedAction.invokeBlock(notLoggedInRequest, _ => fakeResultF).futureValue
    result shouldBe Redirect("http://localhost:9437/agent-services-account/home")
    AuthStubs.verifyAuthorise()

  "successfully authorise when user is logged in, credentialRole is User/Admin, and no active HMRC-AS-AGENT enrolment" in:
    val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
    val notLoggedInRequest: Request[?] = tdAll.request
    AuthStubs.stubAuthorise()
    val result: Result = Ok("AllGood")
    authorisedAction
      .invokeBlock(
        notLoggedInRequest,
        (r: AuthorisedRequest[?]) =>
          Future.successful {
            r.internalUserId shouldBe tdAll.internalUserId
            r.groupId shouldBe tdAll.groupId
            result
          }
      )
      .futureValue shouldBe result
    AuthStubs.verifyAuthorise()

  def fakeResultF: Future[Result] = fail("this should not be executed if test works fine")
