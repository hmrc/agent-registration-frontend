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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.incorporated

import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.getCompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseIndividuaNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.NameMatching
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseFirstIndividualNamePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseNextIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class EnterCompaniesHouseOfficerController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterCompaniesHouseFirstIndividualNamePage: EnterCompaniesHouseFirstIndividualNamePage,
  enterCompaniesHouseNextIndividualNamePage: EnterCompaniesHouseNextIndividualNamePage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  companiesHouseService: CompaniesHouseService
)
extends FrontendController(mcc, actions):

  private type CompaniesHouseRequestData = SixOrMoreOfficers *: Seq[IndividualName] *: List[IndividualProvidedDetails] *: IsIncorporated *: DataWithAuth

  private val baseAction: ActionBuilderWithData[CompaniesHouseRequestData] = actions
    .getApplicationInProgress
    .refine:
      implicit request =>
        request.agentApplication match
          case _: AgentApplication.IsNotIncorporated =>
            logger.warn(
              "NotIncorporated businesses do not have the number of key individuals determined by Companies House results, redirecting to task list for the correct links"
            )
            Redirect(AppRoutes.apply.TaskListController.show.url)
          case aa: IsIncorporated => request.replace[AgentApplication, IsIncorporated](aa)
    .refine:
      implicit request =>
        val agentApplication: IsIncorporated = request.get[IsIncorporated]
        for
          individualsList <- individualProvideDetailsService
            .findAllKeyIndividualsByApplicationId(agentApplication.agentApplicationId)

          companiesHouseOfficers <- companiesHouseService
            .getActiveOfficers(agentApplication.getCrn, agentApplication.getCompaniesHouseOfficerRole)

          allCompaniesHouseOfficersNames = companiesHouseOfficers
            .map(x => CompaniesHouseOfficer.normaliseOfficerName(x.name))
            .map(IndividualName(_))
            .filter(_.isValidName)

          notUsedCompaniesHouseOfficersNames = NameMatching.filterAlreadyUsedNames(
            allCompaniesHouseOfficersNames,
            individualsList.map(_.individualName)
          )
        yield request
          .add[List[IndividualProvidedDetails]](individualsList)
          .add[Seq[IndividualName]](notUsedCompaniesHouseOfficersNames)
    .refine:
      implicit request =>
        request.get[IsIncorporated].getNumberOfCompaniesHouseOfficers match
          case Some(n: SixOrMoreOfficers) => request.add(n)
          case Some(_: FiveOrLessOfficers) =>
            logger.warn("Number of required key individuals is five or less, redirecting to CYA page")
            Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show.url)
          case None =>
            logger.warn(
              "Number of required key individuals not specified in application, redirecting to number of key individuals page"
            )
            Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)

  private def renderPage(
    form: Form[IndividualName],
    resultStatus: Status
  )(implicit request: RequestWithData[CompaniesHouseRequestData]): Future[Result] =
    val agentApplication = request.get[IsIncorporated]
    val individuals = request.get[List[IndividualProvidedDetails]]
    val sixOrMoreOfficers = request.get[SixOrMoreOfficers]
    val formAction = AppRoutes.apply.listdetails.incoporated.EnterCompaniesHouseOfficerController.submit

    getEntityName(agentApplication).map: entityName =>
      individuals match
        case Nil =>
          resultStatus(
            enterCompaniesHouseFirstIndividualNamePage(
              form = form,
              entityName = entityName,
              sixOrMoreOfficers = request.get[SixOrMoreOfficers],
              ordinalKey = "first",
              formAction = formAction,
              agentApplication = agentApplication
            )
          )
        case x if x.size < sixOrMoreOfficers.totalListSize =>
          resultStatus(
            enterCompaniesHouseNextIndividualNamePage(
              form = form,
              entityName = entityName,
              ordinalKey = "subsequent",
              formAction = formAction,
              agentApplication = agentApplication
            )
          )
        case _ => Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show)

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      renderPage(CompaniesHouseIndividuaNameForm.form, Ok)

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
        form = CompaniesHouseIndividuaNameForm.form,
        resultToServeWhenFormHasErrors =
          implicit request =>
            (formWithErrors: Form[IndividualName]) =>
              renderPage(formWithErrors, BadRequest)
      )
      .async:
        implicit request =>
          val individualName: IndividualName = request.get
          val agentApplication = request.get[IsIncorporated]
          val companiesHouseOfficerList = request.get[Seq[IndividualName]]

          NameMatching.individualNameMatching(individualName, companiesHouseOfficerList) match
            case Some(matchedOfficerName) =>
              individualProvideDetailsService.generateNewPersonReference().map(personReference =>
                individualProvideDetailsService
                  .upsertForApplication(
                    individualProvideDetailsService.create(
                      individualName = matchedOfficerName,
                      isPersonOfControl = true,
                      agentApplicationId = agentApplication.agentApplicationId,
                      personReference = personReference
                    )
                  )
              )
                .map: _ =>
                  Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show)

            case None =>
              // No match found — re-render the form with an error
              renderPage(
                CompaniesHouseIndividuaNameForm.form
                  .fill(individualName)
                  .withError(CompaniesHouseIndividuaNameForm.firstNameKey, "error.companiesHouseOfficer.nameNotMatched"),
                BadRequest
              )(using request.delete[IndividualName])
      .redirectIfSaveForLater

  private def getEntityName(agentApplication: IsIncorporated)(using RequestHeader): Future[String] = businessPartnerRecordService
    .getBusinessPartnerRecord(agentApplication.getUtr)
    .map(_.map(_.getEntityName).getOrThrowExpectedDataMissing("Business Partner Record is missing"))
