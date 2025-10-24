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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.services.GrsService
import uk.gov.hmrc.agentregistrationfrontend.util.Errors
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import GrsController.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

@Singleton
class GrsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  grsService: GrsService,
  agentRegistrationService: AgentRegistrationService,
  simplePage: SimplePage
)
extends FrontendController(mcc, actions):

  def setUpGrsFromSignIn(
    agentType: AgentType,
    businessType: BusinessType
  ): Action[AnyContent] = actions.authorised.async:
    implicit request =>
      grsService.createGrsJourney(
        businessType = businessType,
        false
      ).map(journeyStartUrl => Redirect(journeyStartUrl.value))

  /** This endpoint is called by GRS when a user is navigated back from GRS to this Frontend Service. This is where we get [[GrsJourneyData]], extract
    * [[BusinessDetails]] from it and store within [[AgentApplication]]
    */
  def journeyCallback(
    journeyId: JourneyId
  ): Action[AnyContent] = actions
    .getApplicationInProgress
    .async:
      implicit request: AgentApplicationRequest[AnyContent] =>
        for
          journeyData <- grsService.getJourneyData(request.agentApplication.businessType, journeyId)
          result <-
            journeyData.registration.registrationStatus match
              case RegistrationStatus.GrsRegistered =>
                if journeyData.identifiersMatch
                then onGrsRegisteredAndIdentifiersMatch(journeyData)
                else
                  Future.successful(Ok(simplePage(
                    h1 = "Identifiers did not match...",
                    bodyText = Some(
                      "Placeholder for the Identifier match failure page..."
                    )
                  )))
              case RegistrationStatus.GrsFailed =>
                Future.successful(Ok(simplePage(
                  h1 = "Registration call on GRS failed...",
                  bodyText = Some(
                    "Placeholder for the Business registration grs error page... (GrsFailed)"
                  )
                )))
              case RegistrationStatus.GrsNotCalled =>
                Future.successful(Ok(simplePage(
                  h1 = "GrsNotCalled...",
                  bodyText = Some(
                    "Placeholder for the GrsNotCalled page... (GrsNotCalled) "
                  )
                )))
        yield result

  private def onGrsRegisteredAndIdentifiersMatch(
    journeyData: JourneyData
  )(using request: AgentApplicationRequest[AnyContent]): Future[Result] =
    Errors.require(
      journeyData.registration.registrationStatus === RegistrationStatus.GrsRegistered,
      "this function is meant to be called for GrsRegistered case"
    )
    Errors.require(
      journeyData.identifiersMatch,
      "this function is meant to be called when identifiers match"
    )
    val updatedApplication =
      request.agentApplication match
        case aa: AgentApplicationSoleTrader =>
          aa
            .copy(
              applicationState = ApplicationState.GrsDataReceived,
              businessDetails = Some(journeyData.asBusinessDetailsSoleTrader)
            )
        case aa: AgentApplicationLlp =>
          aa.copy(
            applicationState = ApplicationState.GrsDataReceived,
            businessDetails = Some(journeyData.asBusinessDetailsLlp)
          )

    agentRegistrationService
      .upsert(updatedApplication)
      .map: _ =>
        Redirect(routes.TaskListController.show.url)

object GrsController:

  extension (journeyData: JourneyData)

    def asBusinessDetailsSoleTrader: BusinessDetailsSoleTrader = BusinessDetailsSoleTrader(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      fullName = journeyData.fullName.getOrThrowExpectedDataMissing("fullName"),
      dateOfBirth = journeyData.dateOfBirth.getOrThrowExpectedDataMissing("dateOfBirth"),
      nino = journeyData.nino,
      trn = journeyData.trn
    )

    def asBusinessDetailsLlp: BusinessDetailsLlp = BusinessDetailsLlp(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("registration.registeredBusinessPartnerId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      companyProfile = journeyData.companyProfile.getOrThrowExpectedDataMissing("companyProfile")
    )

    def asBusinessDetailsPartnership: BusinessDetailsPartnership = BusinessDetailsPartnership(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrThrowExpectedDataMissing("safeId"),
      saUtr = journeyData.sautr.getOrThrowExpectedDataMissing("sautr"),
      companyProfile = journeyData.companyProfile,
      postcode = journeyData.postcode.getOrThrowExpectedDataMissing("postcode")
    )
