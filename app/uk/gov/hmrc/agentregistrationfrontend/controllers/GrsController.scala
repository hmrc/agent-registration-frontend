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
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsLlp
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsPartnership
import uk.gov.hmrc.agentregistration.shared.BusinessDetailsSoleTrader
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyData
import uk.gov.hmrc.agentregistrationfrontend.model.grs.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus.GrsFailed
import uk.gov.hmrc.agentregistrationfrontend.model.grs.RegistrationStatus.GrsNotCalled
import uk.gov.hmrc.agentregistrationfrontend.services.AgentRegistrationService
import uk.gov.hmrc.agentregistrationfrontend.services.GrsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class GrsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  grsService: GrsService,
  applicationService: AgentRegistrationService,
  placeholder: SimplePage
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

  /** This endpoint is called by Grs when a User is navigated back from Grs to this Frontend Service
    */
  def journeyCallback(
    journeyId: JourneyId
  ): Action[AnyContent] = actions.getApplicationInProgress.async:
    implicit request =>

      request.agentApplication.businessType

      for journeyData <- grsService.getJourneyData(request.agentApplication.businessType, journeyId)
//        _ = request.agentApplication match
//          case aa: AgentApplicationSoleTrader
      yield Ok("")

//      grsService
//        .getJourneyData(request.agentApplication.businessType, journeyId)
//
//
//        .flatMap {
//          case grsResponse: JourneyData if grsResponse.identifiersMatch && grsResponse.registration.registeredBusinessPartnerId.nonEmpty =>
//            applicationService
//              .upsert(
//                request
//                  .agentApplication
//                  .modify(_.utr)
//                  .setTo(Some(grsResponse.getUtr))
//                  .modify(_.businessDetails)
//                  .setTo(Some(grsResponse.toBusinessDetails(businessType)))
//              ).map { _ =>
//                Redirect(routes.TaskListController.show.url)
//              }
//          case grsResponse if !grsResponse.identifiersMatch && grsResponse.registration.registrationStatus.equals(GrsNotCalled) =>
//            Future.successful(Ok(placeholder(
//              h1 = "Identifiers did not match...",
//              bodyText = Some(
//                "Placeholder for the Identifier match failure page..."
//              )
//            )))
//          case grsResponse if grsResponse.identifiersMatch && grsResponse.registration.registrationStatus.equals(GrsFailed) =>
//            Future.successful(Ok(placeholder(
//              h1 = "Registration call on GRS failed...",
//              bodyText = Some(
//                "Placeholder for the Business registration grs error page..."
//              )
//            )))
//          case _ => throw new Exception(s"[GrsController] Unexpected response from GRS for journey: $journeyId")
//        }

object GrsController:

  extension (journeyData: JourneyData)

    def asBusinessDetailsSoleTrader: BusinessDetailsSoleTrader = BusinessDetailsSoleTrader(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrElse(missingDataError("registration.registeredBusinessPartnerId")),
      saUtr = journeyData.sautr.getOrElse(missingDataError("sautr")),
      fullName = journeyData.fullName.getOrElse(missingDataError("fullName")),
      dateOfBirth = journeyData.dateOfBirth.getOrElse(missingDataError("dateOfBirth")),
      nino = journeyData.nino,
      trn = journeyData.trn
    )

    def asBusinessDetailsLlp: BusinessDetailsLlp = BusinessDetailsLlp(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrElse(missingDataError("registration.registeredBusinessPartnerId")),
      saUtr = journeyData.sautr.getOrElse(missingDataError("sautr")),
      companyProfile = journeyData.companyProfile.getOrElse(missingDataError("companyProfile"))
    )

    def asBusinessDetailsPartnership: BusinessDetailsPartnership = BusinessDetailsPartnership(
      safeId = journeyData.registration.registeredBusinessPartnerId.getOrElse(missingDataError("safeId")),
      saUtr = journeyData.sautr.getOrElse(missingDataError("sautr")),
      companyProfile = journeyData.companyProfile,
      postcode = journeyData.postcode.getOrElse(missingDataError("postcode"))
    )

    private inline def missingDataError(key: String): Nothing = throw new RuntimeException(s"Missing expected fields in Grs JourneyData: $key")
