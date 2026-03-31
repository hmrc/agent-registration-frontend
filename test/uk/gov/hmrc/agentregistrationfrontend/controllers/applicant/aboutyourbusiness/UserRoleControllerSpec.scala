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

import play.api.libs.ws.DefaultBodyReadables.*
import play.api.libs.ws.WSCookie
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.forms.PartnershipTypeForm
import uk.gov.hmrc.agentregistrationfrontend.forms.UserRoleForm
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class UserRoleControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/about-your-business/user-role"
  private val partnershipTypePath = "/agent-registration/apply/about-your-business/partnership-type"

  private def partnershipSessionCookies(partnershipType: BusinessType.Partnership): Seq[WSCookie] =
    val businessTypeCookies = addBusinessTypeToSession(BusinessTypeAnswer.PartnershipType).extractCookies
    post(
      uri = partnershipTypePath,
      cookies = businessTypeCookies
    )(Map(PartnershipTypeForm.key -> Seq(partnershipType.toString))).extractCookies

  "routes should have correct paths and methods" in:
    AppRoutes.apply.aboutyourbusiness.UserRoleController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.UserRoleController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.apply.aboutyourbusiness.UserRoleController.submit.url shouldBe AppRoutes.apply.aboutyourbusiness.UserRoleController.show.url

  s"GET $path with SoleTrader in session should return 200 and render page with Owner title" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.SoleTrader).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are you the owner of the business? - Apply for an agent services account - GOV.UK"

  s"GET $path with LimitedCompany in session should return 200 and render page with Director title" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are you a director of the limited company? - Apply for an agent services account - GOV.UK"

  s"GET $path with LimitedLiabilityPartnership in session should return 200 and render page with Member title" in:
    val response: WSResponse = get(
      uri = path,
      cookies = partnershipSessionCookies(BusinessType.Partnership.LimitedLiabilityPartnership)
    )

    response.status shouldBe Status.OK
    response.parseBodyAsJsoupDocument.title() shouldBe "Are you a member of the limited liability partnership? - Apply for an agent services account - GOV.UK"

  Seq(
    BusinessType.Partnership.GeneralPartnership,
    BusinessType.Partnership.LimitedPartnership,
    BusinessType.Partnership.ScottishPartnership,
    BusinessType.Partnership.ScottishLimitedPartnership
  ).foreach: partnershipType =>
    s"GET $path with ${partnershipType.toString} in session should return 200 and render page with Partner title" in:
      val response: WSResponse = get(
        uri = path,
        cookies = partnershipSessionCookies(partnershipType)
      )

      response.status shouldBe Status.OK
      response.parseBodyAsJsoupDocument.title() shouldBe "Are you a partner in the business? - Apply for an agent services account - GOV.UK"

  s"GET $path with LimitedCompany should show 'Yes, I’m a current officer in Companies House' for Director" in:
    val response: WSResponse = get(
      uri = path,
      cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
    )

    response.status shouldBe Status.OK
    val document = response.parseBodyAsJsoupDocument
    document.select("input[value=Director]").first() should not be null
    document.text() should include("Yes, I’m a current officer in Companies House")

  s"GET $path with LimitedLiabilityPartnership should show 'Yes, I'm a current officer in Companies House' for Member" in:
    val response: WSResponse = get(
      uri = path,
      cookies = partnershipSessionCookies(BusinessType.Partnership.LimitedLiabilityPartnership)
    )

    response.status shouldBe Status.OK
    val document = response.parseBodyAsJsoupDocument
    document.select("input[value=Member]").first() should not be null
    document.text() should include("Yes, I’m a current officer in Companies House")

  Seq(
    BusinessType.Partnership.GeneralPartnership,
    BusinessType.Partnership.LimitedPartnership,
    BusinessType.Partnership.ScottishPartnership,
    BusinessType.Partnership.ScottishLimitedPartnership
  ).foreach: partnershipType =>
    s"GET $path with ${partnershipType.toString} should show 'Yes, I'm a current officer in Companies House' for Partner" in:
      val response: WSResponse = get(
        uri = path,
        cookies = partnershipSessionCookies(partnershipType)
      )

      response.status shouldBe Status.OK
      val document = response.parseBodyAsJsoupDocument
      document.select("input[value=Partner]").first() should not be null
      document.text() should include("Yes, I’m a current officer in Companies House")

  s"POST $path with valid UserRole selection should redirect to TypeOfSignIn page" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(UserRoleForm.key -> Seq(UserRole.Director.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url

  s"POST $path with Authorised selection should redirect to TypeOfSignIn page" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(UserRoleForm.key -> Seq(UserRole.Authorised.toString)))

    response.status shouldBe Status.SEE_OTHER
    response.body[String] shouldBe ""
    response.header("Location").value shouldBe AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url

  s"POST $path without valid selection should return 400" in:
    val response: WSResponse =
      post(
        uri = path,
        cookies = addBusinessTypeToSession(BusinessTypeAnswer.LimitedCompany).extractCookies
      )(Map(UserRoleForm.key -> Seq("")))

    response.status shouldBe Status.BAD_REQUEST
    response.parseBodyAsJsoupDocument.title() shouldBe "Error: Are you a director of the limited company? - Apply for an agent services account - GOV.UK"
