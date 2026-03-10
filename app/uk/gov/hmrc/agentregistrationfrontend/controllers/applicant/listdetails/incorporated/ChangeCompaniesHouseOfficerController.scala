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

import com.softwaremill.quicklens.modify
import play.api.data.Form
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.AgentApplication.IsIncorporated
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseOfficerRole.getCompaniesHouseOfficerRole
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistration.shared.lists.SixOrMoreOfficers
import uk.gov.hmrc.agentregistration.shared.lists.FiveOrLessOfficers
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData.*
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseIndividuaNameForm
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.util.NameMatching
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.listdetails.incorporated.EnterCompaniesHouseNextIndividualNamePage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class ChangeCompaniesHouseOfficerController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  enterCompaniesHouseNextIndividualNamePage: EnterCompaniesHouseNextIndividualNamePage,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  companiesHouseService: CompaniesHouseService
)
extends FrontendController(mcc, actions):

  private type DataWithList = Seq[IndividualName] *: List[IndividualProvidedDetails] *: SixOrMoreOfficers *: IsIncorporated *: DataWithAuth

  private val baseAction: ActionBuilderWithData[DataWithList] = actions
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
        request.get[IsIncorporated].getNumberOfCompaniesHouseOfficers match
          case Some(n: SixOrMoreOfficers) => request.add(n)
          case Some(_: FiveOrLessOfficers) =>
            logger.warn("Number of required key individuals is five or less, redirecting to Companies House officers page")
            Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)
          case None =>
            logger.warn(
              "Number of required key individuals not specified in application, redirecting to Companies House officers page"
            )
            Redirect(AppRoutes.apply.listdetails.incoporated.CompaniesHouseOfficersController.show.url)
    .refine:
      implicit request =>
        val agentApplication: IsIncorporated = request.get
        for
          individualsList <- individualProvideDetailsService
            .findAllKeyIndividualsByApplicationId(agentApplication.agentApplicationId)

          companiesHouseOfficers <- companiesHouseService
            .getActiveOfficers(agentApplication.getCrn, agentApplication.getCompaniesHouseOfficerRole)

          companiesHouseOfficersNames = companiesHouseOfficers
            .map(x => CompaniesHouseOfficer.normaliseOfficerName(x.name))
            .map(IndividualName(_))
            .filter(_.isValidName)
        yield request
          .add[List[IndividualProvidedDetails]](individualsList)
          .add[Seq[IndividualName]](companiesHouseOfficersNames)

  def show(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction
    .async:
      implicit request =>
        val existingList: List[IndividualProvidedDetails] = request.get
        val agentApplication: IsIncorporated = request.get
        val formAction: Call = AppRoutes.apply.listdetails.incoporated.ChangeCompaniesHouseOfficerController.submit(
          individualProvidedDetailsId
        )
        val nameToChange: IndividualName =
          existingList
            .find(_._id === individualProvidedDetailsId)
            .getOrThrowExpectedDataMissing(
              s"IndividualProvidedDetails with id $individualProvidedDetailsId not found"
            )
            .individualName

        getEntityName(agentApplication).map: entityName =>
          Ok(enterCompaniesHouseNextIndividualNamePage(
            form = CompaniesHouseIndividuaNameForm.form.fill(nameToChange),
            entityName = entityName,
            ordinalKey = "subsequent",
            formAction = formAction,
            agentApplication = agentApplication
          ))

  def submit(individualProvidedDetailsId: IndividualProvidedDetailsId): Action[AnyContent] = baseAction
    .ensureValidFormAndRedirectIfSaveForLater[IndividualName](
      form = CompaniesHouseIndividuaNameForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          (formWithErrors: Form[IndividualName]) =>
            val agentApplication: IsIncorporated = request.get
            val formAction: Call = AppRoutes.apply.listdetails.incoporated.ChangeCompaniesHouseOfficerController.submit(
              individualProvidedDetailsId
            )
            getEntityName(agentApplication).map: entityName =>
              enterCompaniesHouseNextIndividualNamePage(
                form = formWithErrors,
                entityName = entityName,
                ordinalKey = "subsequent",
                formAction = formAction,
                agentApplication = agentApplication
              )
    )
    .async:
      implicit request =>
        val individualNameFromForm: IndividualName = request.get
        val existingList: List[IndividualProvidedDetails] = request.get
        val companiesHouseOfficerList: Seq[IndividualName] = request.get
        val individualToChange: IndividualProvidedDetails = existingList
          .find(_._id === individualProvidedDetailsId)
          .getOrThrowExpectedDataMissing(
            s"IndividualProvidedDetails with id $individualProvidedDetailsId not found"
          )

        NameMatching.individualNameMatching(individualNameFromForm, companiesHouseOfficerList) match
          case Some(_) =>
            individualProvideDetailsService.upsertForApplication(
              individualToChange
                .modify(_.individualName)
                .setTo(individualNameFromForm)
            )
              .map: _ =>
                Redirect(AppRoutes.apply.listdetails.incoporated.CheckYourAnswersController.show)
          case None =>
            val agentApplication: IsIncorporated = request.get
            val formAction: Call = AppRoutes.apply.listdetails.incoporated.ChangeCompaniesHouseOfficerController.submit(
              individualProvidedDetailsId
            )
            getEntityName(agentApplication).map: entityName =>
              BadRequest(enterCompaniesHouseNextIndividualNamePage(
                form = CompaniesHouseIndividuaNameForm.form
                  .fill(individualNameFromForm)
                  .withError(CompaniesHouseIndividuaNameForm.firstNameKey, "error.companiesHouseOfficer.nameNotMatched"),
                entityName = entityName,
                ordinalKey = "subsequent",
                formAction = formAction,
                agentApplication = agentApplication
              ))

  private def getEntityName(agentApplication: IsIncorporated)(using RequestHeader): Future[String] = businessPartnerRecordService
    .getBusinessPartnerRecord(agentApplication.getUtr)
    .map(_.map(_.getEntityName).getOrThrowExpectedDataMissing("Business Partner Record is missing"))
