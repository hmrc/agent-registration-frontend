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

package uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails

import com.softwaremill.quicklens.modify
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseMatch
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.MemberProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.CompaniesHouseNameQueryPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class CompaniesHouseNameQueryController @Inject() (
  actions: Actions,
  mcc: MessagesControllerComponents,
  view: CompaniesHouseNameQueryPage,
  memberProvideDetailsService: MemberProvideDetailsService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .Member
    .getProvideDetailsInProgress
    .async:
      implicit request =>
        agentApplicationService
          .find(request.memberProvidedDetails.agentApplicationId)
          .map:
            case Some(app) if app.hasFinished =>
              Ok(view(
                CompaniesHouseNameQueryForm.form
                  .fill:
                    request.memberProvidedDetails
                      .companiesHouseMatch
                      .map(_.memberNameQuery)
                ,
                app.asLlpApplication.getBusinessDetails.companyProfile.companyName
              ))
            case Some(app) =>
              logger.warn(s"Attempt to access Companies House name query page for application that has status ${app.applicationState}")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

            case None =>
              logger.warn(s"Attempt to access Companies House name query page for application that does not exist")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  def submit: Action[AnyContent] = actions
    .Member
    .getProvideDetailsInProgress
    .async:
      implicit request =>
        CompaniesHouseNameQueryForm.form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              agentApplicationService
                .find(request.memberProvidedDetails.agentApplicationId)
                .map:
                  case Some(app) =>
                    BadRequest(view(
                      formWithErrors,
                      app.asLlpApplication.getBusinessDetails.companyProfile.companyName
                    ))
                  case _ => Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
            ,
            validFormData =>
              val hasChanged: Boolean =
                request.memberProvidedDetails
                  .companiesHouseMatch
                  .map(_.memberNameQuery)
                  .getOrElse("") != validFormData
              val redirectRoute = AppRoutes.providedetails.CompaniesHouseMatchingController.show
              if hasChanged then
                memberProvideDetailsService
                  .upsert(
                    request.memberProvidedDetails
                      .modify(_.companiesHouseMatch)
                      .setTo(Some(CompaniesHouseMatch(
                        memberNameQuery = validFormData,
                        companiesHouseOfficer = None
                      )))
                  )
                  .map: _ =>
                    Redirect(redirectRoute)
              else
                Future.successful(Redirect(redirectRoute))
          )
