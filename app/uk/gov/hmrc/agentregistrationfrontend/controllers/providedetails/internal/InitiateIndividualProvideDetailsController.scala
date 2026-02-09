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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.internal

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.MessagesControllerComponents
import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualDateOfBirth
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.FrontendController

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class InitiateIndividualProvideDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentApplicationService: AgentApplicationService,
  citizenDetailsConnector: CitizenDetailsConnector
)
extends FrontendController(mcc, actions):

  /** This endpoint is called by resolve upon successful login.
    */
  def initiateIndividualProvideDetails(
    linkId: LinkId
  ): Action[AnyContent] = actions
    .authorisedWithAdditionalIdentifiers
    .async:
      implicit request =>

        val nextEndpoint: Call = AppRoutes.providedetails.CompaniesHouseNameQueryController.show
        val applicationGenericExitPageUrl: String = AppRoutes.apply.AgentApplicationController.genericExitPage.url

        agentApplicationService.find(linkId)
          .flatMap:
            case Some(agentApplication) =>
              individualProvideDetailsService
                .findByApplicationId(agentApplication.agentApplicationId)
                .flatMap:
                  case None =>
                    for {
                      individualProvidedDetails: IndividualProvidedDetailsToBeDeleted <- createIndividualProvidedDetailsFor(
                        applicationId = agentApplication.agentApplicationId,
                        internalUserId = request.internalUserId,
                        nino = request.get[Option[Nino]],
                        saUtr = request.get[Option[SaUtr]]
                      )
                      _ <- individualProvideDetailsService.upsert(individualProvidedDetails)
                    } yield Redirect(nextEndpoint).addToSession(agentApplication.agentApplicationId)

                  case Some(individualProvidedDetails) =>
                    logger.info("Individual provided details already exists, redirecting to individual name page")
                    Future.successful(Redirect(nextEndpoint).addToSession(individualProvidedDetails.agentApplicationId))

            case None =>
              logger.info(s"Application for linkId $linkId not found, redirecting to $applicationGenericExitPageUrl")
              Future.successful(Redirect(applicationGenericExitPageUrl))

  private def createIndividualProvidedDetailsFor(
    applicationId: AgentApplicationId,
    internalUserId: InternalUserId,
    nino: Option[Nino],
    saUtr: Option[SaUtr]
  )(using RequestHeader): Future[IndividualProvidedDetailsToBeDeleted] =
    (nino, saUtr) match

      case (Some(nino), None) =>
        logger.debug(s"Creating individual provided details with NINO only for applicationId: ${applicationId.value}")
        citizenDetailsConnector
          .getCitizenDetails(nino)
          .map { citizenDetails =>
            individualProvideDetailsService.createNewIndividualProvidedDetails(
              internalUserId = internalUserId,
              agentApplicationId = applicationId,
              maybeIndividualNino = Some(IndividualNino.FromAuth(nino)),
              maybeIndividualSaUtr = citizenDetails.saUtr.map(IndividualSaUtr.FromCitizenDetails.apply),
              maybeIndividualDateOfBirth = citizenDetails.dateOfBirth.map(IndividualDateOfBirth.FromCitizensDetails.apply)
            )
          }

      case (Some(nino), Some(saUtr)) =>
        logger.debug(s"Creating individual provided details with NINO AND SAUTR for applicationId: ${applicationId.value}")
        citizenDetailsConnector
          .getCitizenDetails(nino)
          .map { citizenDetails =>
            individualProvideDetailsService.createNewIndividualProvidedDetails(
              internalUserId = internalUserId,
              agentApplicationId = applicationId,
              maybeIndividualNino = Some(IndividualNino.FromAuth(nino)),
              maybeIndividualSaUtr = Some(IndividualSaUtr.FromAuth(saUtr)),
              maybeIndividualDateOfBirth = citizenDetails.dateOfBirth.map(IndividualDateOfBirth.FromCitizensDetails.apply)
            )
          }

      case (None, _) =>
        logger.debug(s"Creating individual provided details with no NINO or DoB for applicationId: ${applicationId.value}")
        Future.successful(
          individualProvideDetailsService.createNewIndividualProvidedDetails(
            internalUserId = internalUserId,
            agentApplicationId = applicationId,
            maybeIndividualNino = None,
            maybeIndividualSaUtr = saUtr.map(IndividualSaUtr.FromAuth.apply),
            maybeIndividualDateOfBirth = None // cannot get DOB without NINO to call citizen details
          )
        )
