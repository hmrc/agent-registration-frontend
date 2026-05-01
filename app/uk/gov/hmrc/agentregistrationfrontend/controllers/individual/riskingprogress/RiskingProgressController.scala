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
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationForRiskingStatus
import uk.gov.hmrc.agentregistration.shared.risking.ApplicationRiskingResponse
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFailure
import uk.gov.hmrc.agentregistration.shared.risking.IndividualRiskingResponse
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.risking.RiskedIndividual
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.FailedNonFixablePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.riskingprogress.IndividualConfirmationPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

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
      val riskingProgress: ApplicationRiskingResponse = request.get
      val agentApplication: AgentApplication = request.get[AgentApplication]
      riskingProgress.status match
        case ApplicationForRiskingStatus.ReadyForSubmission => renderConfirmationPage(agentApplication)
        case ApplicationForRiskingStatus.FailedNonFixable =>
          val riskedIndividual: IndividualRiskingResponse = riskingProgress.individuals.find(
            _.personReference === request.get[IndividualProvidedDetails].personReference
          ).getOrThrowExpectedDataMissing("Risking response individual matching the provided details is missing")
          riskedIndividual.failures match
            case Some(failures) =>
              if failures.exists:
                  case _: IndividualFailure.NonFixable => true
                  case _ => false
              then
                businessPartnerRecordService
                  .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
                  .map: optBpr =>
                    Ok(failedNonFixablePage(
                      individualRiskingResponse = RiskedIndividual( // TODO: casting as a local copy of RiskedIndividual until the shared version is available
                        personReference = riskedIndividual.personReference,
                        individualName = riskedIndividual.providedName,
                        failures = failures
                      ),
                      agentApplication = agentApplication,
                      entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR for application entity is missing")
                    ))
              else renderConfirmationPage(agentApplication)
            case _ => renderConfirmationPage(agentApplication)
        case ApplicationForRiskingStatus.FailedFixable => Future.successful(Redirect(AppRoutes.providedetails.CheckYourAnswersController.show(linkId).url))
        case _ => renderConfirmationPage(agentApplication) // this covers

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
