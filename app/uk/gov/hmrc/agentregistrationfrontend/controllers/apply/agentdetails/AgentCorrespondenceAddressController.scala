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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails

import com.softwaremill.quicklens.*
import play.api.i18n.Lang
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.AgentCorrespondenceAddressForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.BusinessPartnerRecordService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.agentdetails.AgentCorrespondenceAddressPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class AgentCorrespondenceAddressController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: AgentCorrespondenceAddressPage,
  agentApplicationService: AgentApplicationService,
  businessPartnerRecordService: BusinessPartnerRecordService,
  addressLookUpConnector: AddressLookupFrontendConnector
)(using ec: ExecutionContext)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _
        .agentApplication
        .asLlpApplication
        .agentDetails.exists(
          _.agentEmailAddress.exists(_.isVerified)
        ),
      implicit request =>
        logger.warn("Because we don't have a verified email address we are redirecting to the email address page")
        Redirect(routes.AgentEmailAddressController.show)
    )

  def show: Action[AnyContent] = baseAction.async:
    implicit request =>
      businessPartnerRecordService
        .getBusinessPartnerRecord(
          request.agentApplication.utr
        ).map: bprOpt =>
          val agentApplication = request.agentApplication.asLlpApplication
          val existingAddress = agentApplication
            .agentDetails
            .flatMap(
              _.agentCorrespondenceAddress.map(_.toValueString)
            )
          val chroAddress = agentApplication
            .getBusinessDetails
            .companyProfile
            .unsanitisedCHROAddress.map(_.toValueString)
          val prefillValue: Option[String] =
            existingAddress match
              case Some(existingAddress) if existingAddress.contains(chroAddress.getOrElse("")) => Some(existingAddress)
              case Some(existingAddress) if existingAddress.contains(bprOpt.map(_.address.toValueString).getOrElse("")) => Some(existingAddress)
              case Some(existingAddress) => Some("other")
              case _ => None

          Ok(view(
            form = AgentCorrespondenceAddressForm.form.fill:
              prefillValue
            ,
            bprAddress = bprOpt.map(_.address)
          ))

  def submit: Action[AnyContent] =
    baseAction
      .async:
        implicit request: AgentApplicationRequest[AnyContent] =>
          AgentCorrespondenceAddressForm.form
            .bindFromRequest()
            .fold(
              formWithErrors =>
                businessPartnerRecordService
                  .getBusinessPartnerRecord(
                    request.agentApplication.utr
                  ).map: bprOpt =>
                    BadRequest(
                      view(
                        form = formWithErrors,
                        bprAddress = bprOpt.map(_.address)
                      )
                    ).pipe(SubmissionHelper.redirectIfSaveForLater(request, _)),
              addressOption =>
                if (addressOption.matches("other")) {
                  implicit val language: Lang = mcc.messagesApi.preferred(request).lang
                  addressLookUpConnector
                    .initJourney(routes.AgentCorrespondenceAddressController.returnFromAddressLookupFrontend())
                    .map(Redirect(_))
                }
                else
                  val updatedApplication: AgentApplication = request
                    .agentApplication
                    .asLlpApplication
                    .modify(_.agentDetails.each.agentCorrespondenceAddress)
                    .setTo(Some(AgentCorrespondenceAddress.fromString(addressOption)))
                  agentApplicationService
                    .upsert(updatedApplication)
                    .map: _ =>
                      Redirect(routes.CheckYourAnswersController.show.url)
            )
      .redirectIfSaveForLater

  def returnFromAddressLookupFrontend(id: String): Action[AnyContent] = baseAction
    .async:
      implicit request: AgentApplicationRequest[AnyContent] =>
        addressLookUpConnector.getAddressDetails(id).flatMap: address =>
          val updatedApplication: AgentApplication = request
            .agentApplication
            .asLlpApplication
            .modify(_.agentDetails.each.agentCorrespondenceAddress)
            .setTo(Some(AgentCorrespondenceAddress.fromAddressLookupAddress(address)))
          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(routes.CheckYourAnswersController.show.url)
