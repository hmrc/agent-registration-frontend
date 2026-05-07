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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingprogress

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.RiskingProgress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.FailedNonFixablePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.IndividualConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskingProgressController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  individualConfirmationPage: IndividualConfirmationPage,
  failedNonFixablePage: FailedNonFixablePage,
  businessPartnerRecordService: BusinessPartnerRecordService
)
extends FrontendController(mcc, actions):

  def show(linkId: LinkId): Action[AnyContent] = actions.authorisedWithRiskingProgress(linkId).async:
    implicit request =>
      val riskingProgress: RiskingProgress = request.get
      val agentApplication: AgentApplication = request.get[AgentApplication]
      riskingProgress match
        case RiskingProgress.ReadyForSubmission => renderConfirmationPage(agentApplication)
        case failedNonFixable: RiskingProgress.FailedNonFixable =>
          val riskedIndividual: RiskedIndividual = failedNonFixable.riskedIndividuals.find(
            _.personReference === request.get[IndividualProvidedDetails].personReference
          ).getOrThrowExpectedDataMissing("Risking response individual matching the provided details is missing")

          if riskedIndividual.failures.exists:
              case _: IndividualFailure.NonFixable => true
              case _ => false
          then
            businessPartnerRecordService
              .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
              .map: optBpr =>
                Ok(failedNonFixablePage(
                  riskedIndividual = riskedIndividual,
                  agentApplication = agentApplication,
                  entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR for application entity is missing")
                ))
          else renderConfirmationPage(agentApplication)
        case _ => renderConfirmationPage(agentApplication) // TODO: add handling for overall status of fixable failures

  private def renderConfirmationPage(agentApplication: AgentApplication)(using RequestHeader) =
    val applicantName = agentApplication.getApplicantContactDetails.applicantName
    businessPartnerRecordService
      .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
      .map: optBpr =>
        Ok(individualConfirmationPage(
          applicantName = applicantName.value,
          entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR for application entity is missing"),
          isSoleTraderOwner = agentApplication.isSoleTraderOwner
        ))

  extension (agentApplication: AgentApplication)
    def isSoleTraderOwner: Boolean =
      agentApplication match
        case a: AgentApplicationSoleTrader => a.isOwner
        case _ => false
