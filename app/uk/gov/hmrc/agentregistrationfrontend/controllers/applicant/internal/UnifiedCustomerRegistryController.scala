/*
 * Copyright 2026 HM Revenue & Customs
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

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.UcrIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.UnifiedCustomerRegistryConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class UnifiedCustomerRegistryController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  unifiedCustomerRegistryConnector: UnifiedCustomerRegistryConnector,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private def nextPage: Call = AppRoutes.apply.TaskListController.show

  def populateApplicationIdentifiersFromUcr: Action[AnyContent] = actions
    .getApplicationInProgress
    .async:
      implicit request =>
        val application = request.agentApplication
        for
          maybeUcrIdentifiers <- unifiedCustomerRegistryConnector.getOrganisationIdentifiers(application.getUtr)
          _ <-
            maybeUcrIdentifiers match
              case Some(ucrIdentifiers) => populateApplicationIdentifiers(ucrIdentifiers, application)
              case None => Future.unit
        yield Redirect(nextPage)

  private def populateApplicationIdentifiers(
    ucrIdentifiers: UcrIdentifiers,
    agentApplication: AgentApplication
  )(using request: RequestHeader): Future[Unit] =
    val updatedApplication: AgentApplication =
      agentApplication match
        case aa: AgentApplicationSoleTrader =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationLlp =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationLimitedCompany =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationGeneralPartnership =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationLimitedPartnership =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationScottishLimitedPartnership =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )
        case aa: AgentApplicationScottishPartnership =>
          aa.copy(
            vrns = Some(ucrIdentifiers.vrns),
            payeRefs = Some(ucrIdentifiers.payeRefs)
          )

    agentApplicationService
      .upsert(updatedApplication)
