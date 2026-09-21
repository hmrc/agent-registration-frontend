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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.aboutyourbusiness

import org.jsoup.nodes.Document
import play.api.libs.ws.WSCookie
import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

import scala.annotation.unused

class TypeOfSignInControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/agent-online-services-account"

  private val signInPath = "/agent-registration/apply/about-your-business/sign-in"

  private val expectedSignInLink = s"$thisFrontendBaseUrl/agent-registration/apply/internal/initiate-agent-application/uk-tax-agent/limited-company/director"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.submit.url shouldBe AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url
    AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.showSignInPage shouldBe Call(
      method = "GET",
      url = signInPath
    )

  s"GET $path with a valid business type and user role in session should return 200 and render the page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse = getUnauthenticated(
      uri = path,
      cookies = addUserRoleToSession(UserRole.Director).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you already use HMRC online services on behalf of your clients? - Apply for an agent services account - GOV.UK"

  s"POST $path with valid selection should return 303 and redirect to sign in start page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse =
      postUnauthenticated(
        uri = path,
        cookies = addUserRoleToSession(UserRole.Director).extractCookies
      )(Map(TypeOfSignInForm.key -> Seq(TypeOfSignIn.HmrcOnlineServices.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.showSignInPage.url

  s"POST $path without valid selection should return 400" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse =
      postUnauthenticated(
        uri = path,
        cookies = addUserRoleToSession(UserRole.Director).extractCookies
      )(Map(TypeOfSignInForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    val doc: Document = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Error: Do you already use HMRC online services on behalf of your clients? - Apply for an agent services account - GOV.UK"
    doc.mainContent.select(
      s"#${TypeOfSignInForm.key}-error"
    ).text() shouldBe "Error: Select yes if already use HMRC online services on behalf of your clients" // copy defect in conf/messages, raise with content team

  s"GET $signInPath when the applicant already uses HMRC online services should return 200 and render the sign in with agent details page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse = getUnauthenticated(
      uri = signInPath,
      cookies = sessionWith(TypeOfSignIn.HmrcOnlineServices).extractCookies
    )

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Sign in with your existing agent account details - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("a.govuk-button").attr("href") shouldBe expectedSignInLink

  s"GET $signInPath when the applicant has no agent sign in details should return 200 and render the create sign in details page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse = getUnauthenticated(
      uri = signInPath,
      cookies = sessionWith(TypeOfSignIn.CreateSignInDetails).extractCookies
    )

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Create your agent account sign in details - Apply for an agent services account - GOV.UK"
    doc.mainContent.select("a.govuk-button").attr("href") shouldBe expectedSignInLink

  s"GET $signInPath without the type of sign in in session should redirect to the type of sign in page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse = getUnauthenticated(
      uri = signInPath,
      cookies = addUserRoleToSession(UserRole.Director).extractCookies
    )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url

  s"GET $signInPath with an empty session should redirect to the agent type page" in:
    @unused implicit val request: Request[AnyContent] = FakeRequest()
    val response: WSResponse = getUnauthenticated(uri = signInPath)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.AgentTypeController.show.url

  s"GET $signInPath when the applicant is already signed in should redirect to start the application" in:
    val response: WSResponse = getUnauthenticated(
      uri = signInPath,
      cookies = Seq(signedInSessionCookieWith(TypeOfSignIn.HmrcOnlineServices))
    )

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe expectedSignInLink

  private def signedInSessionCookieWith(typeOfSignIn: TypeOfSignIn): WSCookie =
    given Request[AnyContent] = FakeRequest().withSession(sessionHeaders.toSeq*)
    val result: Result = Ok("")
      .addToSession(AgentType.UkTaxAgent)
      .addToSession(BusinessTypeAnswer.LimitedCompany)
      .addToSession(UserRole.Director)
      .addToSession(typeOfSignIn)
    sessionCookie(result.newSession.value)

  private def sessionWith(typeOfSignIn: TypeOfSignIn): WSResponse =
    postUnauthenticated(
      uri = path,
      cookies = addUserRoleToSession(UserRole.Director).extractCookies
    )(Map(TypeOfSignInForm.key -> Seq(typeOfSignIn.toString)))
