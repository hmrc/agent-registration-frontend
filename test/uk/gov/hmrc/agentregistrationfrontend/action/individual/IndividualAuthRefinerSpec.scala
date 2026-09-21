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

package uk.gov.hmrc.agentregistrationfrontend.action.individual

import play.api.http.Status
import play.api.mvc.Result
import play.api.mvc.Results.*
import play.api.test.Helpers.contentAsString
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions.DataWithAuthAndCl
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.providedetails.IndividualAuthStubs
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.concurrent.Future

class IndividualAuthRefinerSpec
extends ISpec:

  "refineIntoRequestWithAdditionalIdentifiers" should:

    "when User is not logged in (request comes without authorisation in the session) action redirects to login url" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      val notLoggedInRequest: RequestWithData[EmptyTuple] = tdAll.requestNotLoggedIn
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(notLoggedInRequest)
        .futureValue
        .left
        .value shouldBe Redirect(
        s"""http://localhost:9099/bas-gateway/sign-in?continue_url=$thisFrontendBaseUrl/&origin=agent-registration-frontend&affinityGroup=individual"""
      )
      IndividualAuthStubs.verifyAuthorise(0)

    "successfully authorise and enrich request with InternalUserId and Credentials when no Nino or Utr are present" in:
      val individualAuthRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthorise()
      individualAuthRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe (
        None,
        None,
        tdAll.confidenceLevel250,
        tdAll.internalUserId,
        tdAll.credentials
      )
      IndividualAuthStubs.verifyAuthorise()

    "redirect to the not agent credential page when the user signs in with agent affinity" in:
      val individualAuthRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthoriseWithAgentAffinity()

      individualAuthRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .left
        .value shouldBe Redirect(
        AppRoutes.providedetails.NotAgentCredentialController.show(Some(RedirectUrl(s"$thisFrontendBaseUrl/")))
      )
      IndividualAuthStubs.verifyAuthorise()

    "successfully authorise and enrich request with Nino, InternalUserId and Credentials" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[
        IndividualAuthRefiner
      ]

      IndividualAuthStubs.stubAuthoriseWithNino()
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe (
        Some(tdAll.nino),
        None,
        tdAll.confidenceLevel250,
        tdAll.internalUserId,
        tdAll.credentials
      )
      IndividualAuthStubs.verifyAuthorise()

    "successfully authorise and enrich request with Utr, InternalUserId and Credentials" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]

      IndividualAuthStubs.stubAuthoriseWithSaUtr()
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe (
        None,
        Some(tdAll.saUtr),
        tdAll.confidenceLevel250,
        tdAll.internalUserId,
        tdAll.credentials
      )
      IndividualAuthStubs.verifyAuthorise()

    "successfully authorise and enrich request with both Nino and Utr" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]

      IndividualAuthStubs.stubAuthoriseWithNinoAndSaUtr()
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe (
        Some(tdAll.nino),
        Some(tdAll.saUtr),
        tdAll.confidenceLevel250,
        tdAll.internalUserId,
        tdAll.credentials
      )
      IndividualAuthStubs.verifyAuthorise()

    "take the Nino from the HMRC-NI enrolment when there is no HMRC-PT enrolment" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]

      IndividualAuthStubs.stubAuthoriseWithNino(IndividualAuthStubs.responseBodyAsIndividualWithNinoInHmrcNiEnrolment())
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .get[Option[Nino]] shouldBe Some(tdAll.nino)
      IndividualAuthStubs.verifyAuthorise()

    "prefer the HMRC-PT Nino when both enrolments have one" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]

      IndividualAuthStubs.stubAuthoriseWithNino(IndividualAuthStubs.responseBodyAsIndividualWithNinoInBothEnrolments())
      individualAuthorisedRefiner
        .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .get[Option[Nino]] shouldBe Some(tdAll.nino)
      IndividualAuthStubs.verifyAuthorise()

    "return the unauthorised page when the affinity group is missing" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]

      IndividualAuthStubs.stubAuthorise(IndividualAuthStubs.responseBodyWithoutAffinityGroup())
      val result: Result =
        individualAuthorisedRefiner
          .refineIntoRequestWithAdditionalIdentifiers(tdAll.requestLoggedIn)
          .futureValue
          .left
          .value
      result.header.status shouldBe Status.UNAUTHORIZED
      contentAsString(Future.successful(result)) should include("Unauthorised")
      IndividualAuthStubs.verifyAuthorise()

  "refineIntoRequestWithAuth" should:

    "when User is not logged in (request comes without authorisation in the session) action redirects to login url" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      individualAuthorisedRefiner
        .refineIntoRequestWithAuth(tdAll.requestNotLoggedIn)
        .futureValue
        .left
        .value shouldBe Redirect(
        s"""http://localhost:9099/bas-gateway/sign-in?continue_url=$thisFrontendBaseUrl/&origin=agent-registration-frontend&affinityGroup=individual"""
      )
      IndividualAuthStubs.verifyAuthorise(0)

    "successfully authorise and enrich request with InternalUserId and Credentials" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthorise()
      val request: RequestWithData[DataWithAuthAndCl] =
        individualAuthorisedRefiner
          .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
          .futureValue
          .value
      request.data.tuple shouldBe tdAll.IndividualRequests.requestWithAuthData.data.tuple

      IndividualAuthStubs.verifyAuthorise()

    "successfully authorise an Organisation" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthorise(IndividualAuthStubs.responseBodyAsOrganisation())
      individualAuthorisedRefiner
        .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe tdAll.IndividualRequests.requestWithAuthData.data.tuple
      IndividualAuthStubs.verifyAuthorise()

    "not enrich the request with the Nino and Utr held in the enrolments" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthoriseWithNinoAndSaUtr()
      individualAuthorisedRefiner
        .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
        .futureValue
        .value
        .data
        .tuple shouldBe tdAll.IndividualRequests.requestWithAuthData.data.tuple
      IndividualAuthStubs.verifyAuthorise()

    "redirect to the not agent credential page when the user signs in with agent affinity" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthoriseWithAgentAffinity()
      individualAuthorisedRefiner
        .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
        .futureValue
        .left
        .value shouldBe Redirect(
        AppRoutes.providedetails.NotAgentCredentialController.show(Some(RedirectUrl(s"$thisFrontendBaseUrl/")))
      )
      IndividualAuthStubs.verifyAuthorise()

    "redirect to the not agent credential page when auth rejects the affinity group" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubUnauthorised(reason = "UnsupportedAffinityGroup")
      individualAuthorisedRefiner
        .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
        .futureValue
        .left
        .value shouldBe Redirect(
        AppRoutes.providedetails.NotAgentCredentialController.show(Some(RedirectUrl(s"$thisFrontendBaseUrl/")))
      )
      IndividualAuthStubs.verifyAuthorise()

    "return the unauthorised page when auth rejects the auth provider" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubUnauthorised(reason = "UnsupportedAuthProvider")
      val result: Result =
        individualAuthorisedRefiner
          .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
          .futureValue
          .left
          .value
      result.header.status shouldBe Status.UNAUTHORIZED
      contentAsString(Future.successful(result)) should include("Unauthorised")
      IndividualAuthStubs.verifyAuthorise()

    "return the unauthorised page for any other authorisation failure" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubUnauthorised(reason = "InsufficientEnrolments")
      val result: Result =
        individualAuthorisedRefiner
          .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
          .futureValue
          .left
          .value
      result.header.status shouldBe Status.UNAUTHORIZED
      contentAsString(Future.successful(result)) should include("Unauthorised")
      IndividualAuthStubs.verifyAuthorise()

    "throw when auth returns no internal id" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthorise(IndividualAuthStubs.responseBodyWithoutInternalId())
      val exception: Throwable =
        individualAuthorisedRefiner
          .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
          .failed
          .futureValue
      exception.getMessage should include("Retrievals for internalId is missing")
      IndividualAuthStubs.verifyAuthorise()

    "throw when auth returns no credentials" in:
      val individualAuthorisedRefiner: IndividualAuthRefiner = app.injector.instanceOf[IndividualAuthRefiner]
      IndividualAuthStubs.stubAuthorise(IndividualAuthStubs.responseBodyWithoutCredentials())
      val exception: Throwable =
        individualAuthorisedRefiner
          .refineIntoRequestWithAuth(tdAll.requestLoggedIn)
          .failed
          .futureValue
      exception.getMessage should include("Retrievals for credentials is missing")
      IndividualAuthStubs.verifyAuthorise()
