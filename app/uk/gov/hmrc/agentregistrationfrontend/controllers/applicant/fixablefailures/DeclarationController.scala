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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.ApplicationState
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.amls.AmlsDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.fixableTaskListStatus
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentRegistrationRiskingService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.DeclarationPage

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeclarationController @Inject() (
  clock: Clock,
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  declarationPage: DeclarationPage,
  appConfig: AppConfig,
  agentRegistrationRiskingService: AgentRegistrationRiskingService,
  agentApplicationService: AgentApplicationService,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type RequestWithCompleteFixableFailures =
    RiskingOutcomeEntity *: RiskingOutcomeApplication.FailedFixable *: Map[PersonReference, RiskingOutcomeIndividual.FailedFixable] *: List[
      IndividualProvidedDetails
    ] *: DataWithApplicationAndBpr

  private val baseAction: ActionBuilderWithData[RequestWithCompleteFixableFailures] = actions.getApplicationForFailedFixable
    .behindFeatureFlag(appConfig.Features.fixableFailures)
    .refine(implicit request =>
      val agentApplication: AgentApplication = request.get
      agentApplication.riskingOutcomeApplication match
        case Some(overallOutcome: RiskingOutcomeApplication.FailedFixable) =>
          if overallOutcome.reSubmittedAt.isDefined
          then Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
          else
            val fixableIndividuals: Map[PersonReference, RiskingOutcomeIndividual.FailedFixable] =
              request.get[List[IndividualProvidedDetails]]
                .filterNot(_.providedByApplicant.contains(true))
                .flatMap: i =>
                  i.getRiskingOutcomeIndividual match
                    case fixable: RiskingOutcomeIndividual.FailedFixable => Some(i.personReference -> fixable)
                    case _ => None
                .toMap
            request
              .add[Map[PersonReference, RiskingOutcomeIndividual.FailedFixable]](fixableIndividuals)
              .add[RiskingOutcomeApplication.FailedFixable](overallOutcome)
        case _ =>
          logger.info(s"Attempted to access fixable failures declaration page but application ref ${request.get[AgentApplication].applicationReference.value} is not in failed fixable state, redirecting to where we handle what the real outcome is")
          Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
    )
    .refine:
      implicit request =>
        val riskingOutcomeEntity: RiskingOutcomeEntity = request.get[AgentApplication].getRiskingOutcomeEntity
        riskingOutcomeEntity match
          case _: RiskingOutcomeEntity.FailedNonFixable =>
            logger.info(s"Attempted to access fixable failures declaration page but application ref ${request.get[AgentApplication].applicationReference.value} has a non-fixable entity, redirecting to where we handle what the real outcome is")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
          case _ => request.add[RiskingOutcomeEntity](riskingOutcomeEntity)
    .ensure(
      condition =
        implicit request =>
          request.get[AgentApplication].fixableTaskListStatus(
            riskingOutcomeEntity = request.get[RiskingOutcomeEntity],
            fixableIndividuals = request.get[Map[PersonReference, RiskingOutcomeIndividual.FailedFixable]].values.toList
          ).declaration.canStart,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn(s"Attempted to access fixable failures declaration page but declaration cannot be started, redirecting to fixable task list")
          Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(declarationPage(
        entityName = request.get[BusinessPartnerRecordResponse].getEntityName
      ))

  def submit: Action[AnyContent] = baseAction.async:
    implicit request =>
      val riskingOutcomeEntity: RiskingOutcomeEntity = request.get[RiskingOutcomeEntity]
      val businessPartnerRecord: BusinessPartnerRecordResponse = request.get
      val agentApplication: AgentApplication =
        riskingOutcomeEntity match
          case outcome @ RiskingOutcomeEntity.FailedFixable(fixes: Seq[EntityFix]) if outcome.hasAmls =>
            val newAmlsDetails: Option[Option[AmlsDetails]] = fixes.collectFirst:
              case amlsFix: EntityFix._3.AmlsFix => amlsFix.amlsDetails
            request.get[AgentApplication]
              .modify(_.amlsDetails)
              .setTo(Some(newAmlsDetails.flatten.getOrElse(throw new IllegalStateException("AmlsFix was present but no AmlsDetails were provided"))))
          case _ => request.get[AgentApplication]

      /** we have a map of those individual fixes which is separate to the list of individual provided details - we need them for submitting */
      val fixesByPersonReference: Map[PersonReference, RiskingOutcomeIndividual.FailedFixable] = request.get
      val allIndividuals: List[IndividualProvidedDetails] = request.get
      val individualsForResubmission: List[IndividualProvidedDetails] = fixesByPersonReference
        .toList
        .map:
          case (personRef, individualFix) =>
            val fixedIndividual: IndividualProvidedDetails = allIndividuals.find(
              _.personReference === personRef
            ).getOrElse(throw new IllegalStateException(s"Failed to find IndividualProvidedDetails for $personRef"))
            individualFix.fixes.collectFirst:
              case d: IndividualFix._10.IndividualDetailsFix => d
            match
              case None => fixedIndividual
              case Some(details) =>
                val afterDob: IndividualProvidedDetails =
                  details.dateOfBirth.fold(fixedIndividual)(dob => fixedIndividual.modify(_.individualDateOfBirth).setTo(Some(dob)))
                val afterNino: IndividualProvidedDetails = details.nino.fold(afterDob)(nino => afterDob.modify(_.individualNino).setTo(Some(nino)))
                val afterSa: IndividualProvidedDetails = details.saUtr.fold(afterNino)(sa => afterNino.modify(_.individualSaUtr).setTo(Some(sa)))
                afterSa
      val arnToEnrolIfApproved: Option[Arn] =
        if
          businessPartnerRecord.isAlreadyRegisteredAsAgent
        then
          Some(businessPartnerRecord.getAgentReferenceNumber)
        else
          None

      val individualsToUpdate = individualsForResubmission.filter: ipd =>
        ipd.riskingOutcomeIndividual.exists:
          case RiskingOutcomeIndividual.FailedFixable(fixes: Seq[IndividualFix], _) =>
            fixes.exists:
              case _: IndividualFix._10.IndividualDetailsFix => true
              case _ => false
          case _ => false

      for
        _ <- agentRegistrationRiskingService.submitForRisking(
          agentApplication = agentApplication,
          individuals = individualsForResubmission,
          arn = arnToEnrolIfApproved,
          isResubmission = true
        )
        _ <- agentApplicationService.upsert(agentApplication
          .modify(_.applicationState)
          .setTo(ApplicationState.SentForRisking)
          .modify(_.riskingOutcomeApplication)
          .using: optionalOutcome =>
            optionalOutcome.collect:
              case failedFixable: RiskingOutcomeApplication.FailedFixable =>
                failedFixable
                  .modify(_.reSubmittedAt)
                  .setTo(Some(Instant.now(clock)))
            .orElse(optionalOutcome))
        _ <- individualProvideDetailsService.upsert(individualsToUpdate)
      yield Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
