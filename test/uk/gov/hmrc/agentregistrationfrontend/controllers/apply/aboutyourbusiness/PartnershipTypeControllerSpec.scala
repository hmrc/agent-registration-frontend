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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.forms.PartnershipTypeForm
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class PartnershipTypeControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/partnership-type"

  "routes should have correct paths and methods" in:
    AppRoutes.apply.aboutyourbusiness.PartnershipTypeController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.PartnershipTypeController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.PartnershipTypeController.submit.url shouldBe AppRoutes.apply.aboutyourbusiness.PartnershipTypeController.show.url

  s"GET $path without BusinessType in session should return 303 and redirect to business type page" in:
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.BusinessTypeSessionController.show.url

  s"GET $path with something other than PartnershipType in session should return 303 and redirect to business type page" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.SoleTrader).extractCookies
    )

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.BusinessTypeSessionController.show.url

  s"GET $path with BusinessType as PartnershipType in session should return 200 and render page" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.PartnershipType).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "What type of partnership? - Apply for an agent services account - GOV.UK"

  BusinessType.Partnership.values.foreach: partnershipType =>
    s"POST $path selecting a ${partnershipType.toString} should redirect to the sign in filter page" in:
      val response: WSResponse =
        post(
          uri = path,
          cookies = addBusinessTypeToSession(BusinessTypeAnswer.PartnershipType).extractCookies
        )(Map(PartnershipTypeForm.key -> Seq(partnershipType.toString)))

      response.status shouldBe Status.SEE_OTHER
      response.body[String] shouldBe ""
      response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.UserRoleController.show.url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.PartnershipType).extractCookies
      )(Map(PartnershipTypeForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: What type of partnership? - Apply for an agent services account - GOV.UK"
