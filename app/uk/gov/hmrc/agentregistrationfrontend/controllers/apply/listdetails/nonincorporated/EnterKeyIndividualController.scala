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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.nonincorporated

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.MessageKeys
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.EnterIndividualNameComplexPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.EnterIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class EnterKeyIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  enterIndividualNameSimplePage: EnterIndividualNamePage,
  enterIndividualNameComplexPage: EnterIndividualNameComplexPage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication match
        case _: AgentApplication.IsNotSoleTrader => true
        case _: AgentApplication.IsSoleTrader => false
      ,
      implicit request =>
        logger.warn("Sole traders do not add individuals to a list, redirecting to task list for the correct links")
        Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .ensure(
      _.agentApplication match
        case _: AgentApplication.IsNotIncorporated => true
        case _: AgentApplication.IsIncorporated => false
      ,
      implicit request =>
        logger.warn(
          "Incorporated businesses should be name matching key individuals against Companies House results, redirecting to task list for the correct links"
        )
        Redirect(AppRoutes.apply.TaskListController.show.url)
    )
    .ensure(
      _.agentApplication.numberOfRequiredKeyIndividuals match
        case Some(_) => true
        case None => false
      ,
      implicit request =>
        logger.warn(
          "Number of required key individuals not specified in application, redirecting to number of key individuals page"
        )
        Redirect(AppRoutes.apply.listdetails.NumberOfKeyIndividualsController.show.url)
    )

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        individualProvideDetailsService.findByApplicationId.flatMap: existingList =>
          request.agentApplication.numberOfRequiredKeyIndividuals match
            case Some(n @ SixOrMore(_)) if n.paddingRequired > 0 =>
              businessPartnerRecordService
                .deleteMeGetBusinessPartnerRecord(request.agentApplication.getUtr)
                .map: bprOpt =>
                  Ok(enterIndividualNameComplexPage(
                    form = IndividualNameForm.form,
                    ordinalKey = MessageKeys.ordinalKey(
                      existingSize = existingList.size,
                      isOnlyOne = false // list size here can never be 1
                    ),
                    numberOfRequiredKeyIndividuals = n,
                    entityName = bprOpt
                      .map(_.getEntityName)
                      .getOrThrowExpectedDataMissing(
                        "Business Partner Record is missing"
                      )
                  ))
            case Some(n @ FiveOrLess(_)) =>
              Future.successful(Ok(enterIndividualNameSimplePage(
                form = IndividualNameForm.form,
                ordinalKey = MessageKeys.ordinalKey(
                  existingSize = existingList.size,
                  isOnlyOne = n.numberOfKeyIndividuals === 1
                )
              )))
            case Some(n @ SixOrMore(_)) =>
              // no padding required, so we can use the simple page
              Future.successful(Ok(enterIndividualNameSimplePage(
                form = IndividualNameForm.form,
                ordinalKey = MessageKeys.ordinalKey(
                  existingSize = existingList.size,
                  isOnlyOne = n.numberOfKeyIndividualsResponsibleForTaxMatters === 1
                )
              )))
            case None => throw IllegalStateException("Required key individuals not specified in application")

  def submit: Action[AnyContent] = baseAction
    .ensureValidFormAndRedirectIfSaveForLaterAsync[IndividualName](
      form = IndividualNameForm.form,
      viewToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            individualProvideDetailsService.findByApplicationId.flatMap: existingList =>
              request.agentApplication.numberOfRequiredKeyIndividuals match
                case Some(n @ SixOrMore(_)) if n.paddingRequired > 0 =>
                  businessPartnerRecordService
                    .deleteMeGetBusinessPartnerRecord(request.agentApplication.getUtr)
                    .map: bprOpt =>
                      enterIndividualNameComplexPage(
                        formWithErrors,
                        ordinalKey = MessageKeys.ordinalKey(
                          existingSize = existingList.size,
                          isOnlyOne = false // list size here can never be 1
                        ),
                        numberOfRequiredKeyIndividuals = n,
                        entityName = bprOpt
                          .map(_.getEntityName)
                          .getOrThrowExpectedDataMissing(
                            "Business Partner Record is missing"
                          )
                      )
                case Some(n @ FiveOrLess(_)) =>
                  Future.successful(enterIndividualNameSimplePage(
                    form = formWithErrors,
                    ordinalKey = MessageKeys.ordinalKey(
                      existingSize = existingList.size,
                      isOnlyOne = n.numberOfKeyIndividuals === 1
                    )
                  ))
                case Some(n @ SixOrMore(_)) =>
                  // no padding required, so we can use the simple page
                  Future.successful(enterIndividualNameSimplePage(
                    form = formWithErrors,
                    ordinalKey = MessageKeys.ordinalKey(
                      existingSize = existingList.size,
                      isOnlyOne = n.numberOfKeyIndividualsResponsibleForTaxMatters === 1
                    )
                  ))
                case None => throw IllegalStateException("Required key individuals not specified in application")
    )
    .async:
      implicit request: (AgentApplicationRequest[AnyContent] & FormValue[IndividualName]) =>
        val individualName = request.formValue
        // we need to create a new IndividualProvidedDetails record with a new Precreated status and new field for the name
        // we have a slight problem right now because these records require an internalUserId, we don't know the internalUserId of
        // the intended credentials user yet, using a placeholder could be confusing, maybe we need to make internalUserId
        // optional in IndividualProvidedDetails? Would also serve to know who has not yet signed in.
        Future.successful(Ok(s"Creation of person record not yet implemented, but received name: ${individualName.value}"))
