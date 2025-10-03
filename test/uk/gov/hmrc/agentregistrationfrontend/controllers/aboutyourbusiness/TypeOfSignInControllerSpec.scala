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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as applicationRoutes
import uk.gov.hmrc.agentregistrationfrontend.forms.TypeOfSignInForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import sttp.model.Uri.UriContext

class TypeOfSignInControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/agent-online-services-account"

  "routes should have correct paths and methods" in:
    routes.TypeOfSignInController.show shouldBe Call(
      method = "GET",
      url = path
    )
    routes.TypeOfSignInController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    routes.TypeOfSignInController.submit.url shouldBe routes.TypeOfSignInController.show.url

  s"GET $path without BusinessType in session should return 303 and redirect to business type page" in:
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.BusinessTypeSessionController.show.url

  s"GET $path with BusinessType of PartnershipType but no partnership type selected should return 303 and redirect to partnership type page" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.PartnershipType).extractCookies
    )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe routes.PartnershipTypeController.show.url

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

    val signInLink = appConfig.signInUri(
      continueUri =
        uri"${appConfig.thisFrontendBaseUrl + applicationRoutes.GrsController.setUpGrsFromSignIn(
            agentType = AgentType.UkTaxAgent,
            businessType = BusinessType.LimitedCompany
          ).url}"
    )
    response.status shouldBe Status.SEE_OTHER
    response.header("Location").value shouldBe routes.TypeOfSignInController.redirectToChosenSignIn(signInLink.toString).url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(TypeOfSignInForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Do you have an HMRC online services for agents account? - Apply for an agent services account - GOV.UK"
