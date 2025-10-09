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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicantcontactdetails

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MemberNamePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberNameController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: MemberNamePage,
  applicationService: ApplicationService,
  memberNameMatchesView: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication.applicantContactDetails match {
        case Some(details) => details.applicantRoleInLlp === ApplicantRoleInLlp.Member
        case _ => false
      },
      implicit request =>
        logger.info("Redirecting to applicant role page due to missing or invalid applicantRoleInLlp value")
        Redirect(routes.ApplicantRoleInLlpController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        CompaniesHouseNameQueryForm.form.fill:
          request
            .agentApplication
            .applicantContactDetails
            .flatMap(_.memberNameQuery)
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(CompaniesHouseNameQueryForm.form, implicit r => view(_))
      .async:
        implicit request =>
          val validFormData: CompaniesHouseNameQuery = request.formValue
          applicationService.upsert(
            request.agentApplication
              .modify(_.applicantContactDetails.each.memberNameQuery)
              .setTo(Some(validFormData))
          ).map((_: Unit) =>
            Redirect(
              routes.MemberNameController.showMemberNameMatches.url
            )
          )
      .redirectIfSaveForLater

  def showMemberNameMatches: Action[AnyContent] =
    baseAction
      .ensure(
        _.agentApplication.applicantContactDetails.flatMap(_.memberNameQuery).isDefined,
        implicit request =>
          logger.info("Redirecting to member name page due to missing memberNameQuery value")
          Redirect(routes.MemberNameController.show)
      ):
        implicit request =>
          Ok(memberNameMatchesView(
            h1 = "Member name matches",
            bodyText = Some("placeholder for matches")
          ))
