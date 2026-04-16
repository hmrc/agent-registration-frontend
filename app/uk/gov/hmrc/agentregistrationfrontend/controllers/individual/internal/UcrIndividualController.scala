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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.internal

import com.softwaremill.quicklens.*
import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.Nino
import uk.gov.hmrc.agentregistration.shared.SaUtr
import uk.gov.hmrc.agentregistration.shared.UcrIdentifiers
import uk.gov.hmrc.agentregistration.shared.individual.IndividualNino
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualSaUtr
import uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions
import uk.gov.hmrc.agentregistrationfrontend.connectors.UnifiedCustomerRegistryConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.services.individual.IndividualProvideDetailsService

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class UcrIndividualController @Inject() (
  mcc: MessagesControllerComponents,
  actions: IndividualActions,
  unifiedCustomerRegistryConnector: UnifiedCustomerRegistryConnector,
  individualProvideDetailsService: IndividualProvideDetailsService
)
extends FrontendController(mcc, actions):

  def populateIndividualIdentifiersFromUcr(linkId: LinkId): Action[AnyContent] = actions
    .authorisedWithIndividualProvidedDetails(linkId)
    .ensure(
      condition = _.get[IndividualProvidedDetails].requiresUcrIdentifiers,
      resultWhenConditionNotMet =
        implicit request =>
          logger.warn("Ucr identifiers already populated. Redirecting to next page.")
          Redirect(nextPage(linkId))
    )
    .async:
      implicit request =>
        val individualProvidedDetails = request.get[IndividualProvidedDetails]
        for
          maybeUcrIdentifiers <- lookupIdentifiers(individualProvidedDetails)
          _ <- populateIndividualIdentifiers(maybeUcrIdentifiers, individualProvidedDetails)
        yield Redirect(nextPage(linkId))

  private def lookupIdentifiers(
    individualProvidedDetails: IndividualProvidedDetails
  )(using RequestHeader): Future[Option[UcrIdentifiers]] =
    individualProvidedDetails.getNinoValue match
      case Some(nino) => unifiedCustomerRegistryConnector.getIndividualIdentifiersByNino(nino)
      case None =>
        individualProvidedDetails.getSaUtrValue match
          case Some(saUtr) => unifiedCustomerRegistryConnector.getIndividualIdentifiersBySaUtr(saUtr)
          case None =>
            logger.warn("No NINO or SA-UTR available for individual UCR lookup. Skipping.")
            Future.successful(None)

  private def nextPage(linkId: LinkId): Call = AppRoutes.providedetails.CheckYourAnswersController.show(linkId)

  private def populateIndividualIdentifiers(
    maybeUcrIdentifiers: Option[UcrIdentifiers],
    individualProvidedDetails: IndividualProvidedDetails
  )(using RequestHeader): Future[Unit] =
    val vrns = maybeUcrIdentifiers.map(_.vrns).getOrElse(List.empty)
    val payeRefs = maybeUcrIdentifiers.map(_.payeRefs).getOrElse(List.empty)
    val updated = individualProvidedDetails
      .modify(_.vrns).setTo(Some(vrns))
      .modify(_.payeRefs).setTo(Some(payeRefs))
    individualProvideDetailsService.upsert(updated)

  extension (individualProvidedDetails: IndividualProvidedDetails)

    private def requiresUcrIdentifiers: Boolean = individualProvidedDetails.payeRefs.isEmpty || individualProvidedDetails.vrns.isEmpty

    private def getNinoValue: Option[Nino] = individualProvidedDetails.individualNino.collect:
      case IndividualNino.FromAuth(nino) => nino
      // IndividualNino.Provided is not used — unverified, not suitable for UCR lookup

    private def getSaUtrValue: Option[SaUtr] = individualProvidedDetails.individualSaUtr.collect:
      case IndividualSaUtr.FromAuth(saUtr) => saUtr
      case IndividualSaUtr.FromCitizenDetails(saUtr) => saUtr
      // IndividualSaUtr.Provided is not used — unverified, not suitable for UCR lookup
