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

import play.api.libs.ws.WSResponse
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

import scala.annotation.unused

class TypeOfSignInControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/agent-online-services-account"

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
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Do you already use HMRC online services on behalf of your clients? - Apply for an agent services account - GOV.UK"

//TODO: missing showSignInPage test
