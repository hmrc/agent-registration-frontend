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

import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsAgentApplicationForDeclaringNumberOfKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLess
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.NumberOfRequiredKeyIndividuals
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMore
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.IndividualNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.MessageKeys
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.nonincorporated.EnterIndividualNameComplexPage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.listdetails.nonincorporated.EnterIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class EnterKeyIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterIndividualNameSimplePage: EnterIndividualNamePage,
  enterIndividualNameComplexPage: EnterIndividualNameComplexPage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilderWithData[
    NumberOfRequiredKeyIndividuals *: IsAgentApplicationForDeclaringNumberOfKeyIndividuals *: DataWithAuth
  ] = actions
    .getApplicationInProgress
    .refine4:
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
          case aa: IsAgentApplicationForDeclaringNumberOfKeyIndividuals =>
            request.replace[AgentApplication, IsAgentApplicationForDeclaringNumberOfKeyIndividuals](aa)
    .refine4:
      implicit request =>
        request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].numberOfRequiredKeyIndividuals match
          case Some(n: NumberOfRequiredKeyIndividuals) => request.add(n)
          case None =>
            logger.warn(
              "Number of required key individuals not specified in application, redirecting to number of key individuals page"
            )
            Redirect(AppRoutes.apply.listdetails.nonincorporated.NumberOfKeyIndividualsController.show.url)

  // TODO: extract logic of choosing view and rendering it based on NumberOfRequiredKeyIndividuals, Form[] and businessPartnerRecord

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val formAction: Call = AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.submit
        individualProvideDetailsService.findAllByApplicationId(request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].agentApplicationId).flatMap:
          existingList =>
            request.get[NumberOfRequiredKeyIndividuals] match
              case n: SixOrMore if existingList.isEmpty && n.paddingRequired > 0 =>
                businessPartnerRecordService
                  .getBusinessPartnerRecord(request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].getUtr)
                  .map: (bprOpt: Option[BusinessPartnerRecordResponse]) =>
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
                        ),
                      formAction = formAction
                    ))
              case n: FiveOrLess =>
                Future.successful(Ok(enterIndividualNameSimplePage(
                  form = IndividualNameForm.form,
                  ordinalKey = MessageKeys.ordinalKey(
                    existingSize = existingList.size,
                    isOnlyOne = n.numberOfKeyIndividuals === 1
                  ),
                  formAction = formAction
                )))
              case n: SixOrMore =>
                // no padding required, so we can use the simple page
                Future.successful(Ok(enterIndividualNameSimplePage(
                  form = IndividualNameForm.form,
                  ordinalKey = MessageKeys.ordinalKey(
                    existingSize = existingList.size,
                    isOnlyOne = n.numberOfKeyIndividualsResponsibleForTaxMatters === 1
                  ),
                  formAction = formAction
                )))

  def submit: Action[AnyContent] = baseAction
    .ensureValidFormAndRedirectIfSaveForLater4[IndividualName](
      form = IndividualNameForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[IndividualName]) =>
            val formAction: Call = AppRoutes.apply.listdetails.nonincorporated.EnterKeyIndividualController.submit
            individualProvideDetailsService.findAllByApplicationId(
              request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].agentApplicationId
            ).flatMap: existingList =>
              request.get[NumberOfRequiredKeyIndividuals] match
                case n: SixOrMore if existingList.isEmpty && n.paddingRequired > 0 =>
                  businessPartnerRecordService
                    .getBusinessPartnerRecord(request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].getUtr)
                    .map: bprOpt =>
                      enterIndividualNameComplexPage(
                        form = formWithErrors,
                        ordinalKey = MessageKeys.ordinalKey(
                          existingSize = existingList.size,
                          isOnlyOne = false // list size here can never be 1
                        ),
                        numberOfRequiredKeyIndividuals = n,
                        entityName = bprOpt
                          .map(_.getEntityName)
                          .getOrThrowExpectedDataMissing(
                            "Business Partner Record is missing"
                          ),
                        formAction = formAction
                      )
                case n: FiveOrLess =>
                  Future.successful(enterIndividualNameSimplePage(
                    form = formWithErrors,
                    ordinalKey = MessageKeys.ordinalKey(
                      existingSize = existingList.size,
                      isOnlyOne = n.numberOfKeyIndividuals === 1
                    ),
                    formAction = formAction
                  ))
                case n: SixOrMore =>
                  // no padding required, so we can use the simple page
                  Future.successful(enterIndividualNameSimplePage(
                    form = formWithErrors,
                    ordinalKey = MessageKeys.ordinalKey(
                      existingSize = existingList.size,
                      isOnlyOne = n.numberOfKeyIndividualsResponsibleForTaxMatters === 1
                    ),
                    formAction = formAction
                  ))
    )
    .async:
      implicit request =>
        val individualName: IndividualName = request.get
        individualProvideDetailsService.upsertPreCreatedProvidedDetails(individualProvideDetailsService.preCreateIndividualProvidedDetails(
          individualName = individualName,
          isPersonOfControl = true, // from this page we are only adding partners, who are persons of control
          agentApplicationId = request.get[IsAgentApplicationForDeclaringNumberOfKeyIndividuals].agentApplicationId
        ))
          .map: _ =>
            Redirect(AppRoutes.apply.listdetails.nonincorporated.CheckYourAnswersController.show)
