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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.http.Status
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.test.Helpers.*
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithAuth
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantAuthRefiner
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs

import scala.concurrent.Future

class ApplicantAuthRefinerSpec
extends ISpec:

  "when User is not logged in (request comes without authorisation in the session) action redirects to login url" in:
    val authRefiner: ApplicantAuthRefiner = app.injector.instanceOf[ApplicantAuthRefiner]
    val notLoggedInRequest: RequestWithData[EmptyTuple] = tdAll.requestNotLoggedIn
    authRefiner
      .refine(notLoggedInRequest)
      .futureValue
      .left
      .value shouldBe Redirect(
      s"""http://localhost:9099/bas-gateway/sign-in?continue_url=$thisFrontendBaseUrl/&origin=agent-registration-frontend&affinityGroup=agent"""
    )
    AuthStubs.verifyAuthorise(0)

  "Credential role must be User or Admin or else the action returns Unahtorised View" in:
    val authRefiner: ApplicantAuthRefiner = app.injector.instanceOf[ApplicantAuthRefiner]
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

    val result: Result =
      authRefiner
        .refine(tdAll.requestLoggedIn)
        .futureValue
        .left
        .value
    contentAsString(Future.successful(result)) should include("unauthorised.heading")
    result.header.status shouldBe Status.UNAUTHORIZED
    AuthStubs.verifyAuthorise()

  "active HMRC-AS-AGENT enrolment MUST NOT be assigned to user or else the action redirects to ASA Dashboard" in:
    val authRefiner: ApplicantAuthRefiner = app.injector.instanceOf[ApplicantAuthRefiner]
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

    val result: Result = authRefiner.refine(tdAll.requestLoggedIn).futureValue.left.value
    result shouldBe Redirect("http://localhost:9437/agent-services-account/home")
    AuthStubs.verifyAuthorise()

  "successfully authorise when user is logged in, credentialRole is User/Admin, and no active HMRC-AS-AGENT enrolment" in:
    val authRefiner: ApplicantAuthRefiner = app.injector.instanceOf[ApplicantAuthRefiner]
    AuthStubs.stubAuthorise()
    val requestWithAuth: RequestWithData[DataWithAuth] =
      authRefiner
        .refine(tdAll.requestLoggedIn)
        .futureValue
        .value
    requestWithAuth.data shouldBe tdAll.ApplicantRequests.requestWithAuthData.data
    AuthStubs.verifyAuthorise()
