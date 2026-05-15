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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsNotSoleTrader
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.citizendetails.DesignatoryDetailsResponse
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class DuplicateAsaCheckController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  citizenDetailsConnector: CitizenDetailsConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def check(): Action[AnyContent] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
    .ensure(
      condition = _.agentApplication.isDuplicateAsaCheckRequired,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Duplicate ASA verification already done. Redirecting to next check.")
          Redirect(nextCheckEndpoint)
    )
    .async:
      implicit request =>
        val bpr =
          request
            .get[BusinessPartnerRecordResponse]
        val isAlreadyRegisteredBpr = bpr.isAnASAgent === true && bpr.agentReferenceNumber.isDefined    

        if isAlreadyRegisteredBpr
        then 
          ???
//        connect to enrolment-store and check agentReferenceNumber
//        if groups are returned
//        then show a page telling them they already have an ASA account
//        else allocate enrolments and update known facts and show user a success screen
//
//        after each branch make sure to set checkResult:
//            .upsert(request.agentApplication
//              .modify(_.isDuplicateAsaCheckRequired)
//              .setTo(if isAlreadyRegisteredBpr then CheckResult.Pass else CheckResult.Fail))
        else
          ???
//        continue to next check
//        yield checkResult match
//          case CheckResult.Pass => Redirect(nextCheckEndpoint)
//          case CheckResult.Fail => Redirect(failedCheckPage)

  private def failedCheckPage: Call = AppRoutes.apply.checkfailed.CanNotConfirmIdentityController.show
  private def nextCheckEndpoint: Call = AppRoutes.apply.internal.UnifiedCustomerRegistryController.populateApplicationIdentifiersFromUcr

  extension (agentApplication: AgentApplication)
    private def isDuplicateAsaCheckRequired: Boolean = agentApplication.duplicateAsaCheckResult =!= Some(CheckResult.Pass)
