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

package uk.gov.hmrc.agentregistrationfrontend.controllers.aboutyourbusiness

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class TypeOfSignInControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/agent-online-services-account"

  "routes should have correct paths and methods" in:
    arf.aboutyourbusiness.routes.TypeOfSignInController.show shouldBe Call(
      method = "GET",
      url = path
    )
    arf.aboutyourbusiness.routes.TypeOfSignInController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    arf.aboutyourbusiness.routes.TypeOfSignInController.submit.url shouldBe arf.aboutyourbusiness.routes.TypeOfSignInController.show.url

  s"GET $path with a valid business type in session should return 200 and render the page" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
    )
    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Do you have an HMRC online services for agents account? - Apply for an agent services account - GOV.UK"

  s"POST $path with valid selection should return 303 and redirect to sign in start page" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(TypeOfSignInForm.key -> Seq(TypeOfSignIn.HmrcOnlineServices.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe arf.aboutyourbusiness.routes.TypeOfSignInController.showSignInPage.url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(TypeOfSignInForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Do you have an HMRC online services for agents account? - Apply for an agent services account - GOV.UK"

//TODO: missing showSignInPage test
