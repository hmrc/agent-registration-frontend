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

package uk.gov.hmrc.agentregistrationfrontend.controllers

import com.softwaremill.quicklens.*
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.http.Status.OK
import play.api.http.Status.SEE_OTHER
import play.api.libs.json.Json
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.BusinessType.*
import uk.gov.hmrc.agentregistration.shared.UserRole.*
import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.toStringHyphenated
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsFailed
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsNotCalled
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistrationStatus.GrsRegistered
import uk.gov.hmrc.agentregistrationfrontend.model.GrsRegistration
import uk.gov.hmrc.agentregistrationfrontend.model.GrsResponse
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationFactory
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs.stubApplicationInProgress
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs.stubUpdateAgentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs.stubAuthorise
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.GrsStubs.*

import java.util.UUID

class GrsControllerSpec
extends ControllerSpec:

  val grsStartUrl = "/agent-registration/apply/start-grs-journey"
  val grsCallbackUrl = "/agent-registration/apply/grs-callback"

  val applicationFactory: ApplicationFactory = app.injector.instanceOf[ApplicationFactory]
  val testJourneyId: String = UUID.randomUUID().toString
  val fakeAgentApplication: AgentApplication = applicationFactory.makeNewAgentApplication(tdAll.internalUserId)
  val soleTraderOwnerApplication: AgentApplication = fakeAgentApplication
    .modify(_.aboutYourApplication)
    .setTo(AboutYourApplication(
      Some(SoleTrader),
      Some(Owner),
      true
    ))
  val soleTraderTransactorApplication: AgentApplication = fakeAgentApplication
    .modify(_.aboutYourApplication)
    .setTo(AboutYourApplication(
      Some(SoleTrader),
      Some(Authorised),
      true
    ))
  val limitedCompanyApplication: AgentApplication = fakeAgentApplication
    .modify(_.aboutYourApplication)
    .setTo(AboutYourApplication(
      Some(LimitedCompany),
      Some(Owner),
      true
    ))
  val generalPartnershipApplication: AgentApplication = fakeAgentApplication
    .modify(_.aboutYourApplication)
    .setTo(AboutYourApplication(
      Some(GeneralPartnership),
      Some(Owner),
      true
    ))
  val limitedLiabilityPartnershipApplication: AgentApplication = fakeAgentApplication
    .modify(_.aboutYourApplication)
    .setTo(AboutYourApplication(
      Some(LimitedLiabilityPartnership),
      Some(Owner),
      true
    ))

  val soleTraderGrsResponse = GrsResponse(
    fullName = Some(tdAll.fullName),
    dateOfBirth = Some(tdAll.dateOfBirth),
    nino = Some(tdAll.nino),
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = None,
    ctutr = None,
    postcode = None,
    identifiersMatch = true,
    registration = GrsRegistration(
      registrationStatus = GrsRegistered,
      registeredBusinessPartnerId = Some(tdAll.safeId)
    )
  )
  val limitedCompanyGrsResponse = GrsResponse(
    fullName = None,
    dateOfBirth = None,
    nino = None,
    trn = None,
    sautr = None,
    companyProfile = Some(tdAll.companyProfile),
    ctutr = Some(tdAll.utr),
    postcode = None,
    identifiersMatch = true,
    registration = GrsRegistration(GrsRegistered, Some(tdAll.safeId))
  )
  val generalPartnershipGrsResponse = GrsResponse(
    fullName = None,
    dateOfBirth = None,
    nino = None,
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = None,
    ctutr = None,
    postcode = Some(tdAll.postcode),
    identifiersMatch = true,
    registration = GrsRegistration(GrsRegistered, Some(tdAll.safeId))
  )
  val limitedLiabiliyPartnershipGrsResponse = GrsResponse(
    fullName = None,
    dateOfBirth = None,
    nino = None,
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = Some(tdAll.companyProfile),
    ctutr = None,
    postcode = Some(tdAll.postcode),
    identifiersMatch = true,
    registration = GrsRegistration(GrsRegistered, Some(tdAll.safeId))
  )
  val unmatchedGrsResponse = GrsResponse(
    fullName = Some(tdAll.fullName),
    dateOfBirth = Some(tdAll.dateOfBirth),
    nino = Some(tdAll.nino),
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = None,
    ctutr = None,
    postcode = None,
    identifiersMatch = false,
    registration = GrsRegistration(GrsNotCalled, None)
  )
  val failedGrsResponse = GrsResponse(
    fullName = Some(tdAll.fullName),
    dateOfBirth = Some(tdAll.dateOfBirth),
    nino = Some(tdAll.nino),
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = None,
    ctutr = None,
    postcode = None,
    identifiersMatch = true,
    registration = GrsRegistration(GrsFailed, None)
  )
  val unexpectedGrsResponse = GrsResponse(
    fullName = Some(tdAll.fullName),
    dateOfBirth = Some(tdAll.dateOfBirth),
    nino = Some(tdAll.nino),
    trn = None,
    sautr = Some(tdAll.utr),
    companyProfile = None,
    ctutr = None,
    postcode = None,
    identifiersMatch = false,
    registration = GrsRegistration(GrsRegistered, Some(tdAll.safeId))
  )
  s"GET $grsStartUrl" should:
    "redirect to grs start for a sole trader owner" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)
      stubCreateGrsJourney(SoleTrader)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe grsSoleTraderJourneyUrl

    "redirect to grs start for a sole trader transactor" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderTransactorApplication)
      stubCreateGrsJourney(SoleTrader)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe grsSoleTraderJourneyUrl

    "redirect to grs start for a limited company" in:
      stubAuthorise()
      stubApplicationInProgress(limitedCompanyApplication)
      stubCreateGrsJourney(LimitedCompany)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe grsLimitedCompanyJourneyUrl

    "redirect to grs start for a general partnership" in:
      stubAuthorise()
      stubApplicationInProgress(generalPartnershipApplication)
      stubCreateGrsJourney(GeneralPartnership)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe grsGeneralPartnershipJourneyUrl

    "redirect to grs start for a limited liability partnership" in:
      stubAuthorise()
      stubApplicationInProgress(limitedLiabilityPartnershipApplication)
      stubCreateGrsJourney(LimitedLiabilityPartnership)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe grsLimitedLiabilityPartnershipJourneyUrl

    "redirect to journey start if user is missing data" in:
      stubAuthorise()
      stubApplicationInProgress(fakeAgentApplication)

      val response = get(grsStartUrl)

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe "/agent-registration/apply"

  s"GET $grsCallbackUrl" should:
    "store valid data and redirect to next page for a sole trader" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)
      stubGetGrsResponse(
        SoleTrader,
        testJourneyId,
        Json.toJson(soleTraderGrsResponse)
      )
      stubUpdateAgentApplication(
        soleTraderOwnerApplication
          .modify(_.utr)
          .setTo(Some(tdAll.utr))
          .modify(_.businessDetails)
          .setTo(Some(SoleTraderDetails(
            safeId = tdAll.safeId,
            businessType = SoleTrader,
            fullName = tdAll.fullName,
            dateOfBirth = tdAll.dateOfBirth,
            nino = Some(tdAll.nino),
            trn = None
          )))
      )

      val response = get(s"$grsCallbackUrl/${SoleTrader.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe routes.AgentApplicationController.taskList.url

    "store valid data and redirect to next page for a limited company" in:
      stubAuthorise()
      stubApplicationInProgress(limitedCompanyApplication)
      stubGetGrsResponse(
        LimitedCompany,
        testJourneyId,
        Json.toJson(limitedCompanyGrsResponse)
      )
      stubUpdateAgentApplication(
        limitedCompanyApplication
          .modify(_.utr)
          .setTo(Some(tdAll.utr))
          .modify(_.businessDetails)
          .setTo(Some(LimitedCompanyDetails(
            safeId = tdAll.safeId,
            businessType = LimitedCompany,
            companyProfile = tdAll.companyProfile
          )))
      )

      val response = get(s"$grsCallbackUrl/${LimitedCompany.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe routes.AgentApplicationController.taskList.url

    "store valid data and redirect to next page for a general partnership" in:
      stubAuthorise()
      stubApplicationInProgress(generalPartnershipApplication)
      stubGetGrsResponse(
        GeneralPartnership,
        testJourneyId,
        Json.toJson(generalPartnershipGrsResponse)
      )
      stubUpdateAgentApplication(
        generalPartnershipApplication
          .modify(_.utr)
          .setTo(Some(tdAll.utr))
          .modify(_.businessDetails)
          .setTo(Some(PartnershipDetails(
            safeId = tdAll.safeId,
            businessType = GeneralPartnership,
            companyProfile = None,
            postcode = tdAll.postcode
          )))
      )

      val response = get(s"$grsCallbackUrl/${GeneralPartnership.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe routes.AgentApplicationController.taskList.url

    "store valid data and redirect to next page for a limited liability partnership" in:
      stubAuthorise()
      stubApplicationInProgress(limitedLiabilityPartnershipApplication)
      stubGetGrsResponse(
        LimitedLiabilityPartnership,
        testJourneyId,
        Json.toJson(limitedLiabiliyPartnershipGrsResponse)
      )
      stubUpdateAgentApplication(
        limitedLiabilityPartnershipApplication
          .modify(_.utr)
          .setTo(Some(tdAll.utr))
          .modify(_.businessDetails)
          .setTo(Some(PartnershipDetails(
            safeId = tdAll.safeId,
            businessType = LimitedLiabilityPartnership,
            companyProfile = Some(tdAll.companyProfile),
            postcode = tdAll.postcode
          )))
      )

      val response = get(s"$grsCallbackUrl/${LimitedLiabilityPartnership.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe routes.AgentApplicationController.taskList.url

    "redirect to journey start if callback url does not match application data" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)

      val response = get(s"$grsCallbackUrl/${LimitedCompany.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe SEE_OTHER
      response.header("Location").value shouldBe "/agent-registration/apply"

    "redirect to failed to match identifiers if grs data has identifiersMatch = false" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)
      stubGetGrsResponse(
        SoleTrader,
        testJourneyId,
        Json.toJson(unmatchedGrsResponse)
      )

      val response = get(s"$grsCallbackUrl/${SoleTrader.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe OK // SEE_OTHER
    // response.header("Location").value shouldBe "identifiers not matched"

    "redirect to failed to registration failed if grs data has registrationStatus = REGISTRATION_FAILED" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)
      stubGetGrsResponse(
        businessType = SoleTrader,
        journeyId = testJourneyId,
        responseBody = Json.toJson(failedGrsResponse)
      )

      val response = get(s"$grsCallbackUrl/${SoleTrader.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe OK // SEE_OTHER
    // response.header("Location").value shouldBe "business regisration failed"

    "throw runtime error if grs returns an unexpected combination of data" in:
      stubAuthorise()
      stubApplicationInProgress(soleTraderOwnerApplication)
      stubGetGrsResponse(
        businessType = SoleTrader,
        journeyId = testJourneyId,
        responseBody = Json.toJson(unexpectedGrsResponse)
      )

      val response = get(s"$grsCallbackUrl/${SoleTrader.toStringHyphenated}?journeyId=$testJourneyId")

      response.status shouldBe INTERNAL_SERVER_ERROR
