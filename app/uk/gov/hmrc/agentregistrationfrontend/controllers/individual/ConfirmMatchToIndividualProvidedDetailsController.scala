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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.AgentApplicationSoleTrader
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.forms.ConfirmMatchToIndividualProvidedDetailsForm
import uk.gov.hmrc.agentregistrationfrontend.forms.YesNo
import uk.gov.hmrc.agentregistrationfrontend.model.citizendetails.CitizenDetails
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.services.applicant.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.individual.ConfirmMatchToIndividualProvidedDetailsPage

import javax.inject.Inject
import scala.concurrent.Future

class ConfirmMatchToIndividualProvidedDetailsController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: ConfirmMatchToIndividualProvidedDetailsPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService,
  individualProvideDetailsService: IndividualProvideDetailsService,
  citizenDetailsConnector: CitizenDetailsConnector
)
extends FrontendController(mcc, actions):

  private type DataWithCitizenDetails = CitizenDetails *: List[IndividualProvidedDetails] *: AgentApplication *: DataWithAdditionalIdentifiers

  private type DataWithIndividualProvidedDetailsForMatching = IndividualProvidedDetails *: DataWithCitizenDetails

  private def baseAction(linkId: LinkId): ActionBuilderWithData[DataWithIndividualProvidedDetailsForMatching] = actions
    .authorisedWithAdditionalIdentifiers
    .refine(implicit request =>
      agentApplicationService
        .find(linkId)
        .map:
          case Some(agentApplication) => request.add(agentApplication)
          case None => Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
    )
    .refine(implicit request =>
      individualProvideDetailsService
        .findAllForMatchingWithApplication(request.get[AgentApplication].agentApplicationId)
        .map:
          case list: List[IndividualProvidedDetails] if list.exists(_.internalUserId.contains(request.get[InternalUserId])) =>
            Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
          case list: List[IndividualProvidedDetails] if !list.exists(_.internalUserId.isEmpty) =>
            logger.warn("No matching IndividualProvidedDetails record and there are no records left without an internalUserId, so we must exit the user")
            Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url)
          case list: List[IndividualProvidedDetails] => request.add[List[IndividualProvidedDetails]](list)
    )
    .refine(implicit request =>
      request.get[Option[Nino]] match
        case None =>
          logger.warn("No NINO found in session, cannot match to citizen details, redirecting to generic exit page")
          Future.successful(Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url))
        case Some(nino) =>
          citizenDetailsConnector
            .getCitizenDetails(nino)
            .map[RequestWithData[DataWithCitizenDetails]]: details =>
              request.add(details)
    )
    .refine(implicit request =>
      val list: List[IndividualProvidedDetails] = request.get
      val citizenDetails: CitizenDetails = request.get[CitizenDetails]
      val listOfUnclaimedIndividualProvidedDetails: List[IndividualProvidedDetails] = list.filter(_.internalUserId.isEmpty)
      listOfUnclaimedIndividualProvidedDetails.matchCitizenDetailsName(citizenDetails) match
        case Some(individualProvidedDetails) => request.add(individualProvidedDetails)
        case None =>
          logger.warn("No matching IndividualProvidedDetails record found for citizen details, redirecting to dedicated exit page")
          Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url) // TODO: redirect to new exit page
    )

  def show(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .async:
      implicit request =>
        val agentApplication: AgentApplication = request.get
        val businessTypeKey =
          agentApplication match
            case _: AgentApplicationLlp => "LimitedLiabilityPartnership"
            case _: AgentApplicationLimitedCompany => "LimitedCompany"
            case _: AgentApplicationSoleTrader => "SoleTrader"
            case _: AgentApplication.IsPartnership => "Partnership"
        businessPartnerRecordService
          .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
          .map: optBpr =>
            Ok(
              view(
                form = ConfirmMatchToIndividualProvidedDetailsForm.form,
                individualProvidedDetails = request.get[IndividualProvidedDetails],
                entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR is missing for application"), // TODO work out whether we call BPR or change to put it into application
                businessTypeKey = businessTypeKey,
                linkId = linkId
              )
            )

  def submit(linkId: LinkId): Action[AnyContent] = baseAction(linkId)
    .ensureValidForm[YesNo](
      form = ConfirmMatchToIndividualProvidedDetailsForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            val agentApplication: AgentApplication = request.get
            val businessTypeKey =
              agentApplication match
                case _: AgentApplicationLlp => "LimitedLiabilityPartnership"
                case _: AgentApplicationLimitedCompany => "LimitedCompany"
                case _: AgentApplicationSoleTrader => "SoleTrader"
                case _: AgentApplication.IsPartnership => "Partnership"
            businessPartnerRecordService
              .getApplicationBusinessPartnerRecord(agentApplication.getUtr)
              .map: optBpr =>
                BadRequest(
                  view(
                    form = formWithErrors,
                    individualProvidedDetails = request.get[IndividualProvidedDetails],
                    entityName = optBpr.map(_.getEntityName).getOrThrowExpectedDataMissing("BPR is missing for application"), // TODO work out whether we call BPR or change to put it into application
                    businessTypeKey = businessTypeKey,
                    linkId = linkId
                  )
                )
    )
    .async:
      implicit request =>
        val confirmMatchToIndividualProvidedDetails: YesNo = request.get
        if confirmMatchToIndividualProvidedDetails.toBoolean then
          individualProvideDetailsService
            .claimIndividualProvidedDetails(
              request.get[IndividualProvidedDetails],
              request.get[InternalUserId],
              request.get[Option[Nino]],
              request.get[CitizenDetails]
            )
            .map: _ =>
              Redirect(AppRoutes.providedetails.CheckYourAnswersController.show.url)
        else
          logger.warn(s"User does not agree with the match to IndividualProvidedDetails record ${request.get[IndividualProvidedDetails]._id} for citizen details ${request.get[CitizenDetails]} and user ${request.get[InternalUserId].value}, redirecting to generic exit page")
          Future.successful(Redirect(AppRoutes.providedetails.ExitController.genericExitPage.url))

  extension (list: List[IndividualProvidedDetails])
    private def matchCitizenDetailsName(
      citizenDetails: CitizenDetails
    )(using request: RequestHeader): Option[IndividualProvidedDetails] =
      val fullName: String = s"${citizenDetails.firstName.getOrElse("")} ${citizenDetails.lastName.getOrElse("")}"
      val maybeExactMatch = list.find(individualProvidedDetails =>
        individualProvidedDetails.individualName.value.toLowerCase === fullName.toLowerCase
      )
      maybeExactMatch match
        case Some(individualProvidedDetails) => Some(individualProvidedDetails)
        case None =>
          val surnameMatches = list.filter(individualProvidedDetails =>
            individualProvidedDetails.individualName.value.split(" ")
              .lastOption.exists(_.toLowerCase === citizenDetails.lastName.getOrElse("").toLowerCase)
          )
          surnameMatches match
            case individualProvidedDetails :: Nil => Some(individualProvidedDetails)
            case Nil => None
            case surnameList: List[IndividualProvidedDetails] =>
              surnameList.find(individualProvidedDetails =>
                individualProvidedDetails.individualName.value.split(" ")
                  .headOption.exists(_.toLowerCase === citizenDetails.firstName.getOrElse("").toLowerCase)
              )
