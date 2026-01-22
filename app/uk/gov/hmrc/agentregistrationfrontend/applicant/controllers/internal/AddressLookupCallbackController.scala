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

package uk.gov.hmrc.agentregistrationfrontend.applicant.controllers.internal

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistrationfrontend.applicant.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.addresslookup.GetConfirmedAddressResponse
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.addresslookup.JourneyId
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.agentdetails.AgentCorrespondenceAddressHelper
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.shared.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.util.Errors

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class AddressLookupCallbackController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  agentApplicationService: AgentApplicationService,
  addressLookUpConnector: AddressLookupFrontendConnector
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  def journeyCallback(id: Option[JourneyId]): Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _ => id.isDefined,
      implicit r =>
        logger.error("Missing JourneyId in the request from address-lookup-frontend.")
        Errors.throwBadRequestException("Missing JourneyId in the request from address-lookup-frontend.")
    )
    .async:
      implicit request: AgentApplicationRequest[AnyContent] =>
        addressLookUpConnector
          .getConfirmedAddress(
            id.getOrThrowExpectedDataMissing("addressLookupJourneyId")
          ).flatMap: (address: GetConfirmedAddressResponse) =>
            val updatedApplication: AgentApplication = request
              .agentApplication
              .modify(_.agentDetails.each.agentCorrespondenceAddress)
              .setTo(Some(AgentCorrespondenceAddressHelper.fromAddressLookupAddress(address)))
            agentApplicationService
              .upsert(updatedApplication)
              .map: _ =>
                Redirect(AppRoutes.apply.agentdetails.CheckYourAnswersController.show.url)
