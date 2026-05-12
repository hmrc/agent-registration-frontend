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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.providedbyapplicant

import com.google.inject.Inject
import com.google.inject.Singleton
import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualVerifiedEmailAddress
import uk.gov.hmrc.agentregistration.shared.individual.ProvidedDetailsState.Finished
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantSessionStore
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.providedbyapplicant.CheckYourAnswersPage

@Singleton
class CheckYourAnswersController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: CheckYourAnswersPage,
  providedByApplicantSessionStore: ProvidedByApplicantSessionStore,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private type AgentProvidedIndividualDetails = IndividualProvidedDetails *: ProvidedByApplicant *: AgentApplication *: DataWithAuth
  private type ViewProvidedDetails = ProvidedByApplicant *: IndividualProvidedDetails *: AgentApplication *: DataWithAuth

  private def viewAction(individualProvidedDetailsId: IndividualProvidedDetailsId): ActionBuilderWithData[ViewProvidedDetails] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        individualProvideDetailsService
          .findById(individualProvidedDetailsId)
          .map:
            case Some(individualProvidedDetails) => request.add[IndividualProvidedDetails](individualProvidedDetails)
            case None =>
              logger.warn("No IndividualProvidedDetails record found for viewing or updating by applicant")
              Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url)
    .refine:
      implicit request =>
        val individualProvidedDetails: IndividualProvidedDetails = request.get
        val newProvidedByApplicant = ProvidedByApplicant(
          individualProvidedDetailsId = individualProvidedDetailsId,
          individualName = individualProvidedDetails.individualName,
          individualDateOfBirth = individualProvidedDetails.individualDateOfBirth,
          telephoneNumber = individualProvidedDetails.telephoneNumber,
          emailAddress = individualProvidedDetails.emailAddress.map(_.emailAddress),
          individualNino = individualProvidedDetails.individualNino,
          individualSaUtr = individualProvidedDetails.individualSaUtr
        )
        providedByApplicantSessionStore
          .upsert(newProvidedByApplicant)
          .map: _ =>
            request.add[ProvidedByApplicant](newProvidedByApplicant)

  private def baseAction: ActionBuilderWithData[AgentProvidedIndividualDetails] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        providedByApplicantSessionStore
          .find()
          .map:
            case Some(providedByApplicant) => request.add[ProvidedByApplicant](providedByApplicant)
            case None =>
              logger.warn("No ProvidedByApplicant details found in session when trying to access SaUtr page, redirecting to select individual page")
              Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SelectIndividualController.show.url)
    .ensure(
      _.get[ProvidedByApplicant].individualDateOfBirth.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedDoBController.show)
    )
    .ensure(
      _.get[ProvidedByApplicant].telephoneNumber.isDefined,
      implicit request =>
        Redirect(AppRoutes.apply.listdetails.providedbyapplicant.TelephoneNumberController.show)
    )
    .ensure(
      _.get[ProvidedByApplicant].emailAddress.isDefined,
      implicit request =>
        Redirect(AppRoutes.apply.listdetails.providedbyapplicant.EmailAddressController.show)
    )
    .ensure(
      _.get[ProvidedByApplicant].individualNino.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.apply.listdetails.providedbyapplicant.ApplicantProvidedNinoController.show)
    )
    .ensure(
      _.get[ProvidedByApplicant].individualSaUtr.nonEmpty,
      implicit request =>
        Redirect(AppRoutes.apply.listdetails.providedbyapplicant.SaUtrController.show)
    )
    .refine(implicit request =>
      individualProvideDetailsService
        .findById(request.get[ProvidedByApplicant].individualProvidedDetailsId)
        .map:
          case Some(individualProvidedDetails) => request.add[IndividualProvidedDetails](individualProvidedDetails)
          case None =>
            logger.warn(
              "No IndividualProvidedDetails found when trying to access Check Your Answers page, redirecting to list of individuals who need to provide details"
            )
            Redirect(AppRoutes.apply.listdetails.progress.CheckProgressController.show.url)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        providedByApplicant = request.get[ProvidedByApplicant]
      ))

  def submit: Action[AnyContent] = baseAction.async:
    implicit request =>
      val providedByApplicant: ProvidedByApplicant = request.get
      val individualProvidedDetails: IndividualProvidedDetails = request.get
      individualProvideDetailsService
        .upsertForApplication(individualProvidedDetails
          .modify(_.individualDateOfBirth)
          .setTo(providedByApplicant.individualDateOfBirth)
          .modify(_.telephoneNumber)
          .setTo(providedByApplicant.telephoneNumber)
          .modify(_.emailAddress)
          .setTo(Some(IndividualVerifiedEmailAddress(
            emailAddress = providedByApplicant.getEmailAddress,
            isVerified = false
          )))
          .modify(_.individualNino)
          .setTo(providedByApplicant.individualNino)
          .modify(_.individualSaUtr)
          .setTo(providedByApplicant.individualSaUtr)
          .modify(_.passedIv)
          .setTo(Some(false))
          .modify(_.providedByApplicant)
          .setTo(Some(true))
          .modify(_.providedDetailsState)
          .setTo(Finished))
        .map: _ =>
          Redirect(AppRoutes.apply.listdetails.progress.CheckProgressController.show)

  def view(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] =
    viewAction(individualProvidedDetailsId):
      implicit request =>
        Ok(view(
          providedByApplicant = request.get[ProvidedByApplicant]
        ))

  extension (agentApplication: AgentApplication)
    def isSoleTraderOwner: Boolean =
      agentApplication match
        case a: AgentApplicationSoleTrader => a.isOwner
        case _ => false
