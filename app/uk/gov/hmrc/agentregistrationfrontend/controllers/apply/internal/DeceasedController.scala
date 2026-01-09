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
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.EntityCheckResult.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
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
      condition =
        _.agentApplication
          .refusalToDealWithCheck
          .isDefined,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Entity verification has not been done. Redirecting to entity check.")
          Redirect(AppRoutes.apply.internal.RefusalToDealWithController.check())
    )
    .ensure(
      condition = _.agentApplication.businessType === BusinessType.SoleTrader,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Deceased verification not required. Redirecting to company status check.")
          Redirect(nextPage)
    )
    .ensure(
      condition = _.agentApplication.asSoleTraderApplication.deceasedCheck.forall(_ === EntityCheckResult.Fail),
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Deceased verification already done. Redirecting to company status check.")
          Redirect(nextPage)
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
                  .isDeceased(nino)
                  .map(_.asEntityCheckResult)
              case None => Future.successful(EntityCheckResult.Pass) // TODO - confirm this is correct

          _ <- agentApplicationService
            .upsert(request.agentApplication
              .asSoleTraderApplication
              .modify(_.deceasedCheck)
              .setTo(Some(checkResult)))
        yield checkResult match
          case EntityCheckResult.Pass => Redirect(nextPage)
          case EntityCheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage = AppRoutes.apply.entitycheckfailed.CanNotConfirmIdentityController.show
  private def nextPage = AppRoutes.apply.internal.CompaniesHouseStatusController.check()
