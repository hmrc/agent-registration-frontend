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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails

import com.softwaremill.quicklens.*
import play.api.mvc.Action
import play.api.mvc.ActionBuilder
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.ApplicantRoleInLlp
import uk.gov.hmrc.agentregistration.shared.companieshouse.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.=!=
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.action.FormValue
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseNameQueryForm
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MemberNamePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemberNameController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  view: MemberNamePage,
  agentApplicationService: AgentApplicationService
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions
    .Applicant
    .getApplicationInProgress
    .ensure(
      _.agentApplication.asLlpApplication.applicantContactDetails.map(_.applicantName.role).contains(ApplicantRoleInLlp.Member),
      implicit request =>
        logger.warn("Member name page requires Member role. Redirecting to applicant role selection page")
        Redirect(routes.ApplicantRoleInLlpController.show)
    )

  def show: Action[AnyContent] = baseAction:
    implicit request =>
      Ok(view(
        CompaniesHouseNameQueryForm.form
          .fill:
            request.agentApplication.asLlpApplication.memberNameQuery
      ))

  def submit: Action[AnyContent] =
    baseAction
      .ensureValidFormAndRedirectIfSaveForLater(CompaniesHouseNameQueryForm.form, implicit r => view(_))
      .async:
        implicit request: (AgentApplicationRequest[AnyContent] & FormValue[CompaniesHouseNameQuery]) =>
          val submittedNameQuery: CompaniesHouseNameQuery = request.formValue
          val hasChanged = request.agentApplication.asLlpApplication.memberNameQuery =!= Some(submittedNameQuery)

          val updatedApplication =
            if hasChanged
            then
              request
                .agentApplication
                .asLlpApplication
                .modify(_.applicantContactDetails.each.applicantName)
                .setTo(ApplicantName.NameOfMember(
                  memberNameQuery = Some(submittedNameQuery)
                ))
            else request.agentApplication

          agentApplicationService
            .upsert(updatedApplication)
            .map: _ =>
              Redirect(
                routes.CompaniesHouseMatchingController.show.url
              )
      .redirectIfSaveForLater

  extension (agentApplication: AgentApplicationLlp)
    def memberNameQuery: Option[CompaniesHouseNameQuery] =
      for
        acd <- agentApplication.applicantContactDetails
        nameOfMember <- acd.applicantName.as[ApplicantName.NameOfMember]
        query <- nameOfMember.memberNameQuery
      yield query
