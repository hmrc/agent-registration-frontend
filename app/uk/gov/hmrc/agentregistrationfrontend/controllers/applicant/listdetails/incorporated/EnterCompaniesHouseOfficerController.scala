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
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer.normaliseOfficerName
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.getCompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseIndividuaNameForm
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
  individualProvideDetailsService: IndividualProvideDetailsService,
  companiesHouseService: CompaniesHouseService
)
extends FrontendController(mcc, actions):

  private type CompaniesHouseRequestData =
    SixOrMoreOfficers *: List[IndividualProvidedDetails] *: BusinessPartnerRecordResponse *: IsIncorporated *: DataWithAuth

  private val baseAction: ActionBuilderWithData[CompaniesHouseRequestData] = actions
    .getApplicationInProgress
    .getBusinessPartnerRecord
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
        individualProvideDetailsService
          .findAllKeyIndividualsByApplicationId(request.get[IsIncorporated].agentApplicationId)
          .map: individualProvidedDetailsList =>
            request.add[List[IndividualProvidedDetails]](individualProvidedDetailsList)
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

  def show: Action[AnyContent] = baseAction:
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
      .refine:
        implicit request: RequestWithData[IndividualName *: CompaniesHouseRequestData] =>
          val individualName: IndividualName = request.get
          val agentApplication: IsIncorporated = request.get[IsIncorporated]
          companiesHouseService
            .getActiveOfficers(
              companyRegistrationNumber = agentApplication.getCrn,
              lastName = individualName.value.split(" ").lastOption.getOrElse(""),
              expectedRole = getCompaniesHouseOfficerRole(agentApplication)
            ).map: results =>
              request.add[Seq[CompaniesHouseOfficer]](results)
      .async {
        implicit request: RequestWithData[Seq[CompaniesHouseOfficer] *: IndividualName *: CompaniesHouseRequestData] =>
          val individualNameFromForm: IndividualName = request.get
          val agentApplication: IsIncorporated = request.get
          val existingList: List[IndividualProvidedDetails] = request.get
          val companiesHouseOfficerList: Seq[CompaniesHouseOfficer] = request.get
          val availableNames: Seq[IndividualName] = reduceToAvailableNames(
            existingList = existingList,
            companiesHouseOfficerList = companiesHouseOfficerList
          )
          NameMatching.individualNameMatching(individualNameFromForm, availableNames) match
            case Some(matchedOfficerName) =>
              for
                individualProvidedDetails: IndividualProvidedDetails <- individualProvideDetailsService.create(
                  individualName = matchedOfficerName,
                  isPersonOfControl = true,
                  agentApplicationId = agentApplication.agentApplicationId
                )
                _ <- individualProvideDetailsService.upsertForApplication(individualProvidedDetails)
              yield Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show)

            case None =>
              // No match found — re-render the form with an error instead of an exit page, by design
              Future.successful(
                renderPage(
                  form = CompaniesHouseIndividuaNameForm.form
                    .fill(individualNameFromForm)
                    .withError(CompaniesHouseIndividuaNameForm.firstNameKey, "error.companiesHouseOfficer.nameNotMatched"),
                  resultStatus = BadRequest
                )(using request.delete[IndividualName].delete[Seq[CompaniesHouseOfficer]])
              )
      }
      .redirectIfSaveForLater

  /** We have abstracted the logic to determine what type of page to render, as it is used in multiple places
    */
  private def renderPage(
    form: Form[IndividualName],
    resultStatus: Status
  )(implicit request: RequestWithData[CompaniesHouseRequestData]): Result =
    val agentApplication: IsIncorporated = request.get
    val individuals: List[IndividualProvidedDetails] = request.get
    val sixOrMoreOfficers: SixOrMoreOfficers = request.get
    val entityName = request.get[BusinessPartnerRecordResponse].getEntityName
    individuals match
      case Nil =>
        resultStatus(
          enterCompaniesHouseFirstIndividualNamePage(
            form = form,
            entityName = entityName,
            sixOrMoreOfficers = request.get[SixOrMoreOfficers],
            agentApplication = agentApplication
          )
        )
      case x if x.size < sixOrMoreOfficers.totalListSize =>
        resultStatus(
          enterCompaniesHouseNextIndividualNamePage(
            form = form,
            entityName = entityName,
            agentApplication = agentApplication
          )
        )
      case _ => Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show)

  /** We need to support duplicate names existing on Companies House. This means that if there are 2 or more instances of the same name on Companies House and
    * the applicant has already provided a name that matches those duplicates, we want to allow them to provide as many as there are on Companies House. We
    * group the CH list by name and count the occurrences, then for each name in the CH list we check if it exists in the existing list and if so we reduce the
    * count. If the count is greater than 0, we know there are still unmatched officers with that name on the CH list, and we keep it in the available names
    * list. Worth noting we may end up using other CH officer values (such as DoB) and this is why we keep the CH officer list as a list of officers not names.
    */
  private def reduceToAvailableNames(
    existingList: List[IndividualProvidedDetails],
    companiesHouseOfficerList: Seq[CompaniesHouseOfficer]
  ): Seq[IndividualName] =
    val existingListGroupedByName: Map[String, Int] =
      existingList
        .groupMapReduce(_.individualName.value)(_ => 1)(_ + _)
    val (unMatchedCompaniesHouseOfficers, _) =
      companiesHouseOfficerList.foldLeft((Seq.empty[CompaniesHouseOfficer], existingListGroupedByName)):
        case ((acc, counts), officer) =>
          val key = normaliseOfficerName(officer.name)
          val existingForName = counts.getOrElse(key, 0)
          if existingForName > 0
          then (acc, counts.updated(key, existingForName - 1).filter(_._2 > 0))
          else
            // either not matched by existing list or all existing occurrences consumed therefore keep this officer in the list
            (acc :+ officer, counts)
    unMatchedCompaniesHouseOfficers
      .map(o => normaliseOfficerName(o.name))
      .map(IndividualName(_))
