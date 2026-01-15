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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.llp.DesignatoryDetailsResponse
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class DeceasedController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  citizenDetailsConnector: CitizenDetailsConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      condition = _.agentApplication.businessType === BusinessType.SoleTrader,
      resultWhenConditionNotMet =
        implicit request =>
          logger.debug(s"Deceased verification is required only for SoleTrader, this business type is ${request.agentApplication.businessType}. Redirecting to company status check.")
          Redirect(nextCheckEndpoint)
    )
    .ensure(
      condition = _.agentApplication.asSoleTraderApplication.isDeceasedCheckRequired,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Deceased verification already done. Redirecting to company status check.")
          Redirect(nextCheckEndpoint)
    )
    .async:
      implicit request =>
        for
          checkResult <-
            request
              .agentApplication
              .asSoleTraderApplication
              .getBusinessDetails
              .nino match
              case Some(nino) =>
                citizenDetailsConnector
                  .getDesignatoryDetails(nino)
                  .map: (designatoryDetailsResponse: DesignatoryDetailsResponse) =>
                    if designatoryDetailsResponse.deceased
                    then EntityCheckResult.Fail
                    else EntityCheckResult.Pass
              case None => Future.successful(EntityCheckResult.Pass) // TODO - confirm this is correct

          _ <- agentApplicationService
            .upsert(request.agentApplication
              .asSoleTraderApplication
              .modify(_.deceasedCheckResult)
              .setTo(Some(checkResult)))
        yield checkResult match
          case EntityCheckResult.Pass => Redirect(nextCheckEndpoint)
          case EntityCheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage: Call = AppRoutes.apply.entitycheckfailed.CanNotConfirmIdentityController.show
  private def nextCheckEndpoint: Call = AppRoutes.apply.internal.CompaniesHouseStatusController.check()

  extension (agentApplication: AgentApplicationSoleTrader)
    private def isDeceasedCheckRequired: Boolean = agentApplication.deceasedCheckResult =!= Some(EntityCheckResult.Pass)
