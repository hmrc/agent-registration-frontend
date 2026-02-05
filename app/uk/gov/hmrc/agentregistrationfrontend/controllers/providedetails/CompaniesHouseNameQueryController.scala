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
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions

import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.llp.IndividualProvideDetailsService
import uk.gov.hmrc.agentregistrationfrontend.views.html.providedetails.CompaniesHouseNameQueryPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class CompaniesHouseNameQueryController @Inject() (
  actions: IndividualActions,
  mcc: MessagesControllerComponents,
  view: CompaniesHouseNameQueryPage,
  individualProvideDetailsService: IndividualProvideDetailsService,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  def show: Action[AnyContent] = actions
    .getProvideDetailsInProgress
    .async:
      implicit request =>
        agentApplicationService
          .find(request.individualProvidedDetails.agentApplicationId)
          .map:
            case Some(app) if app.hasFinished =>
              Ok(view(
                CompaniesHouseNameQueryForm.form
                  .fill:
                    request.individualProvidedDetails
                      .companiesHouseMatch
                      .map(_.memberNameQuery)
                ,
                app.dontCallMe_getCompanyProfile.companyName
              ))
            case Some(app) =>
              logger.warn(s"Attempt to access Companies House name query page for application that has status ${app.applicationState}")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

            case None =>
              logger.warn(s"Attempt to access Companies House name query page for application that does not exist")
              Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)

  def submit: Action[AnyContent] = actions
    .getProvideDetailsWithApplicationInProgress
    .ensureValidForm4[CompaniesHouseNameQuery](
      form = CompaniesHouseNameQueryForm.form,
      resultToServeWhenFormHasErrors =
        implicit request =>
          formWithErrors =>
            view(
              formWithErrors,
              request.agentApplication.dontCallMe_getCompanyProfile.companyName
            )
    )
    .async:
      implicit request =>
        val companiesHouseNameQuery: CompaniesHouseNameQuery = request.get
        val hasChanged: Boolean =
          request.individualProvidedDetails
            .companiesHouseMatch
            .map(_.memberNameQuery)
            .getOrElse("") =!= companiesHouseNameQuery
        val redirectRoute = AppRoutes.providedetails.CompaniesHouseMatchingController.show
        if hasChanged then
          individualProvideDetailsService
            .upsert(
              request.individualProvidedDetails
                .modify(_.companiesHouseMatch)
                .setTo(Some(CompaniesHouseMatch(
                  memberNameQuery = companiesHouseNameQuery,
                  companiesHouseOfficer = None
                )))
            )
            .map: _ =>
              Redirect(redirectRoute)
        else
          Future.successful(Redirect(redirectRoute))
