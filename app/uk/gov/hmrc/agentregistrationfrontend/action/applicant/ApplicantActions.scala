/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.action.applicant

import play.api.mvc.*
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsUnincorporatedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.AgentApplicationGeneralPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishLimitedPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationScottishPartnership
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix._10.IndividualDetailsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeApplication
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistration.shared.util.Errors.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineFutureEither
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.PresentIn
import uk.gov.hmrc.agentregistrationfrontend.audit.AuditService
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.RequestSupport.getCurrentSessionId
import uk.gov.hmrc.auth.core.retrieve.Credentials

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

object ApplicantActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  /** Checkpoints of the applicant journey, one alias per stage a controller can be at. Each alias extends its parent in the tree below (rightmost element =
    * added earliest); controllers pick single elements with `request.get[T]`.
    *
    * {{{
    * DataWithAuth                        InternalUserId, GroupId, Credentials
    * ├ DataWithMaybeApplication          + Option[AgentApplication]
    * └ DataWithApplication               + AgentApplication
    *   └ DataWithApplicationAndBpr       + BusinessPartnerRecordResponse
    *     └ DataWithSoleTraderIdentityFix + IndividualProvidedDetails, RiskingOutcomeApplication.FailedFixable, IndividualDetailsFix
    * }}}
    */
  type DataWithAuth = (InternalUserId, GroupId, Credentials)
  type DataWithMaybeApplication = Option[AgentApplication] *: DataWithAuth
  type DataWithApplication = AgentApplication *: DataWithAuth
  type DataWithApplicationAndBpr = BusinessPartnerRecordResponse *: DataWithApplication
  type DataWithSoleTraderIdentityFix = IndividualDetailsFix *: RiskingOutcomeApplication.FailedFixable *: IndividualProvidedDetails *: DataWithApplicationAndBpr
  type DataWithUnincorporatedPartnershipKeyIndividuals =
    List[IndividualProvidedDetails] *: NumberOfRequiredKeyIndividuals *: IsUnincorporatedPartnership *: DataWithAuth

@Singleton
class ApplicantActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  authorisedActionRefiner: ApplicantAuthRefiner,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvidedDetailsService: IndividualProvideDetailsService,
  auditService: AuditService
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export ApplicantActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  val authorised: ActionBuilderWithData[DataWithAuth] = action
    .refineFutureEither(authorisedActionRefiner.refine)

  val getApplication: ActionBuilderWithData[DataWithApplication] = authorised
    .refine:
      implicit request: RequestWithData[DataWithAuth] =>
        agentApplicationService
          .find()
          .map[Result | RequestWithData[DataWithApplication]]:
            case Some(agentApplication) => request.add(agentApplication)
            case None =>
              val redirect = AppRoutes.apply.AgentApplicationController.startRegistration
              logger.error(s"[Unexpected State] No agent application found for authenticated user ${request.get[InternalUserId].value}. Redirecting to startRegistration page ($redirect)")
              Redirect(redirect)
    .refine:
      implicit request =>
        val aa: AgentApplication = request.agentApplication
        if aa.continueJourney()
        then
          val updatedApplication = aa.updateCachedSessionId()

          agentApplicationService.upsert(updatedApplication).map(_ =>
            auditService.auditContinueApplication(aa)
            request.update(updatedApplication)
          )
        else request

  val getMaybeApplicationForInitiation: ActionBuilderWithData[DataWithMaybeApplication] = authorised
    .refine:
      implicit request =>
        agentApplicationService.find().map: maybeApplication =>
          request.add(maybeApplication)

  def getApplicationInProgress: ActionBuilderWithData[DataWithApplication] = getApplication
    .ensure(
      condition = _.get[AgentApplication].isBeforeSentForRisking,
      resultWhenConditionNotMet =
        implicit request =>
          val call = AppRoutes.apply.AgentApplicationController.applicationStatus
          logger.warn(
            s"The application is not in progress" +
              s" (current application state: ${request.get[AgentApplication].applicationState.toString}), " +
              s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
          )
          Redirect(call.url)
    )

  val getApplicationAfterSentForRisking: ActionBuilderWithData[DataWithApplicationAndBpr] =
    getApplication
      .ensure(
        condition = _.agentApplication.isAfterSentForRisking,
        resultWhenConditionNotMet =
          implicit request =>
            val call: Call = AppRoutes.apply.AgentApplicationController.landing
            logger.warn(
              s"The application is not in the final state" +
                s" (current application state: ${request.agentApplication.applicationState.toString}), " +
                s"redirecting to [${call.url}]. User might have used back or history to get to ${request.path} from previous page."
            )
            Redirect(call.url)
      )
      .getBusinessPartnerRecord

  val getApplicationForFailedFixable: ActionBuilderWithData[List[IndividualProvidedDetails] *: DataWithApplicationAndBpr] = getApplicationAfterSentForRisking
    .refine:
      implicit request =>
        individualProvidedDetailsService.findAllByApplicationId(request.get[AgentApplication]._id).map: list =>
          request.add[List[IndividualProvidedDetails]](list)

  def getIncorporatedApplication: ActionBuilderWithData[IsIncorporated *: DataWithAuth] =
    getApplicationInProgress
      .narrowToIncorporated

  def getIncorporatedApplicationAndBpr: ActionBuilderWithData[BusinessPartnerRecordResponse *: IsIncorporated *: DataWithAuth] =
    getApplicationInProgress
      .getBusinessPartnerRecord
      .narrowToIncorporated

  def getKeyIndividualsForUnincorporatedPartnership: ActionBuilderWithData[DataWithUnincorporatedPartnershipKeyIndividuals] = getApplicationInProgress
    .refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: IsIncorporated =>
            logger.warn(
              "Incorporated businesses should be name matching key individuals against Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case _: AgentApplicationSoleTrader =>
            logger.warn("Sole traders do not add individuals to a list, redirecting to task list for the correct links")
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case unincorporatedPartnership: IsUnincorporatedPartnership =>
            request.replace[AgentApplication, IsUnincorporatedPartnership](unincorporatedPartnership)
    .refine:
      implicit request =>
        request.get[IsUnincorporatedPartnership].getNumberOfRequiredKeyIndividuals match
          case Some(numberOfRequiredKeyIndividuals: NumberOfRequiredKeyIndividuals) =>
            request.add[NumberOfRequiredKeyIndividuals](numberOfRequiredKeyIndividuals)
          case None =>
            logger.warn(
              "Number of required key individuals not specified in application, redirecting to number of key individuals page"
            )
            Redirect(AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url)
    .refine:
      implicit request =>
        individualProvidedDetailsService
          .findAllKeyIndividualsByApplicationId(request.get[IsUnincorporatedPartnership].agentApplicationId)
          .map(request.add[List[IndividualProvidedDetails]])

  val getSoleTraderIdentityFix: ActionBuilderWithData[DataWithSoleTraderIdentityFix] = getApplicationAfterSentForRisking
    .refine:
      implicit request =>
        individualProvidedDetailsService.findAllByApplicationId(request.get[AgentApplication]._id).map:
          case soleTrader :: Nil => request.add[IndividualProvidedDetails](soleTrader)
          case _ =>
            logger.warn(s"Unexpected variation on sole trader individuals for application ${request.get[AgentApplication]._id}, redirecting to where outcome can be handled.")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
    .refine:
      implicit request =>
        request.get[AgentApplication].riskingOutcomeApplication match
          case Some(outcome: RiskingOutcomeApplication.FailedFixable) => request.add[RiskingOutcomeApplication.FailedFixable](outcome)
          case outcome =>
            logger.warn(s"Risking outcome for application is not fixable (or missing). Redirecting to where outcome can be handled: $outcome")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)
    .refine:
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        individualProvidedDetails.getRiskingOutcomeIndividual match
          case outcome: RiskingOutcomeIndividual.FailedFixable =>
            outcome.fixes.collectFirst { case fix: IndividualDetailsFix => fix } match
              case Some(individualFix) => request.add[IndividualDetailsFix](individualFix)
              case None =>
                logger.info("Risking outcome for individual does not require individual details to be provided, redirecting to fixable task list.")
                Redirect(AppRoutes.fixablefailures.FixableTaskListController.show)
          case _ =>
            logger.info("Risking outcome for individual is not fixable, redirecting to application status to get latest status.")
            Redirect(AppRoutes.apply.AgentApplicationController.applicationStatus)

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def getBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      BusinessPartnerRecordResponse AbsentIn Data
    ): ActionBuilderWithData[BusinessPartnerRecordResponse *: Data] = ab.refine:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(_.getOrThrowExpectedDataMissing(s"Business Partner Record for UTR ${request.get[AgentApplication].getUtr.value}"))
          .map(request.add)

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def narrowToIncorporated(using
      AgentApplication PresentIn Data,
      IsIncorporated AbsentIn Data
    ): ActionBuilderWithData[UniqueTuple.Replace[
      AgentApplication,
      IsIncorporated,
      Data
    ]] = ab.refine:
      implicit request =>
        request.get[AgentApplication] match
          case _: AgentApplication.IsNotIncorporated =>
            logger.warn(
              "NotIncorporated businesses do not have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case incorporated: IsIncorporated => request.replace[AgentApplication, IsIncorporated](incorporated)

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    /** The key individuals named on the application so far, without the other relevant individuals. */
    inline def getCompaniesHouseKeyIndividuals(using
      IsIncorporated PresentIn Data,
      List[IndividualProvidedDetails] AbsentIn Data
    ): ActionBuilderWithData[List[IndividualProvidedDetails] *: Data] = ab.refine:
      implicit request =>
        individualProvidedDetailsService
          .findAllKeyIndividualsByApplicationId(request.get[IsIncorporated].agentApplicationId)
          .map(request.add[List[IndividualProvidedDetails]])

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    /** The declared number of officers, for the pages that only exist when the company has six or more of them. `redirectWhenFiveOrLess` is where an
      * application with five or fewer officers belongs instead.
      */
    inline def getSixOrMoreOfficers(redirectWhenFiveOrLess: Call)(using
      IsIncorporated PresentIn Data,
      SixOrMoreOfficers AbsentIn Data
    ): ActionBuilderWithData[SixOrMoreOfficers *: Data] = ab.refine:
      implicit request =>
        request.get[IsIncorporated].getNumberOfCompaniesHouseOfficers match
          case Some(sixOrMoreOfficers: SixOrMoreOfficers) => request.add[SixOrMoreOfficers](sixOrMoreOfficers)
          case Some(_: FiveOrLessOfficers) =>
            logger.debug("Number of required key individuals is five or less, redirecting away from the six or more pages")
            Redirect(redirectWhenFiveOrLess.url)
          case None =>
            logger.debug("Number of required key individuals not specified in application, redirecting to Companies House officers page")
            Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)

  extension [Data <: Tuple](ab: ActionBuilderWithData[Data])

    inline def getMaybeBusinessPartnerRecord(using
      AgentApplication PresentIn Data,
      Option[BusinessPartnerRecordResponse] AbsentIn Data
    ): ActionBuilderWithData[Option[BusinessPartnerRecordResponse] *: Data] = ab.refine:
      implicit request =>
        businessPartnerRecordService
          .getBusinessPartnerRecord(request.get[AgentApplication].getUtr)
          .map(request.add)

  extension (agentApplication: AgentApplication)

    private def continueJourney()(using request: RequestHeader): Boolean =
      val currentSessionId = getCurrentSessionId
      agentApplication.cachedSessionId =!= currentSessionId

  extension (agentApplication: AgentApplication)
    private def updateCachedSessionId()(using request: RequestHeader): AgentApplication =
      val currentSessionId = getCurrentSessionId

      agentApplication match
        case a: AgentApplicationLlp => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationSoleTrader => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationLimitedCompany => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationGeneralPartnership => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationLimitedPartnership => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationScottishLimitedPartnership => a.copy(cachedSessionId = currentSessionId)
        case a: AgentApplicationScottishPartnership => a.copy(cachedSessionId = currentSessionId)
