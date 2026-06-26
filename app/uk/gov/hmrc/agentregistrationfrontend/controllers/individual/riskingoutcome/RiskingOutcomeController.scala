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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.RiskedIndividual
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.FailedNonFixablePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.IndividualConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class RiskingOutcomeController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  individualConfirmationPage: IndividualConfirmationPage,
  failedNonFixablePage: FailedNonFixablePage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  simplePage: SimplePage
)
extends FrontendController(mcc, actions):

  def show(linkId: LinkId): Action[AnyContent] = actions.authorisedWithRiskingOutcome(linkId).async:
    implicit request =>
      val riskingOutcomeApplication: RiskingOutcomeApplication = request.get
      val riskingOutcomeIndividual: RiskingOutcomeIndividual = request.get
      riskingOutcomeApplication.outcome match
        case RiskingOutcomeApplication.Outcome.FailedNonFixable =>
          riskingOutcomeIndividual match
            case failedNonFixable: RiskingOutcomeIndividual.FailedNonFixable =>
              val riskedIndividual: RiskedIndividual = RiskedIndividual(
                personReference = request.get[IndividualProvidedDetails].personReference,
                individualName = request.get[IndividualProvidedDetails].individualName,
                failures = failedNonFixable.failures
              )
              businessPartnerRecordService
                .getApplicationBusinessPartnerRecord(request.agentApplication.getUtr)
                .map: optBpr =>
                  Ok(failedNonFixablePage(
                    riskedIndividual = riskedIndividual,
                    agentApplication = request.agentApplication,
                    entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR for application entity is missing")
                  ))
            case _ => renderConfirmationPage(request.agentApplication) // this individual has not failed non-fixable, so render the confirmation page
        case RiskingOutcomeApplication.Outcome.FailedFixable =>
          riskingOutcomeIndividual match
            case _: RiskingOutcomeIndividual.FailedFixable =>
              // TODO: implement fixable failures outcome page which will then link to the individual fixable task list page
              Future.successful(Ok(simplePage(
                h1 = "Fixable failures outcome page",
                bodyText = Some("This page is a placeholder for the fixable failures outcome page.")
              )))
            case _ => renderConfirmationPage(request.agentApplication) // this individual has not failed fixable, so render the confirmation page
        case _ => renderConfirmationPage(request.agentApplication) // any other outcome renders the confirmation page

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
