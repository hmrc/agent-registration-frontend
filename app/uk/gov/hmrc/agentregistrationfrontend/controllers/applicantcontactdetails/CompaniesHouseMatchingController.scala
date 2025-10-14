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
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseNameQuery
import uk.gov.hmrc.agentregistration.shared.contactdetails.CompaniesHouseOfficer
import uk.gov.hmrc.agentregistration.shared.util.RequiredDataExtensions.getOrThrowExpectedDataMissing
import uk.gov.hmrc.agentregistrationfrontend.action.Actions
import uk.gov.hmrc.agentregistrationfrontend.action.AgentApplicationRequest
import uk.gov.hmrc.agentregistrationfrontend.controllers.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.CompaniesHouseOfficerForm
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.services.ApplicationService
import uk.gov.hmrc.agentregistrationfrontend.services.CompaniesHouseService
import uk.gov.hmrc.agentregistrationfrontend.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.applicantcontactdetails.MembersMatchedPage

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

@Singleton
class CompaniesHouseMatchingController @Inject() (
  mcc: MessagesControllerComponents,
  actions: Actions,
  applicationService: ApplicationService,
  companiesHouseService: CompaniesHouseService,
  memberNameMatchesView: MembersMatchedPage,
  noMemberNameMatchesView: SimplePage
)
extends FrontendController(mcc, actions):

  private val baseAction: ActionBuilder[AgentApplicationRequest, AnyContent] = actions.getApplicationInProgress
    .ensure(
      _.agentApplication.memberNameQuery.isDefined,
      implicit request =>
        logger.info("Redirecting to member name page due to missing memberNameQuery value")
        Redirect(routes.MemberNameController.show)
    )

  def show: Action[AnyContent] = baseAction
    .async:
      implicit request =>
        companiesHouseService.getCompaniesHouseOfficers(
          companyRegistrationNumber = request.agentApplication.getCompanyRegistrationNumber,
          lastName = request.agentApplication.getLastNameFromQuery, // safe due to ensuring memberNameQuery is defined first
          isLlp = true // LLP members only
        ).map: officers =>
          if officers.isEmpty then
            logger.info("No Companies House officers found matching member name query")
            Ok(noMemberNameMatchesView(
              h1 = "No member name matches",
              bodyText = Some("placeholder for no matches")
            ))
          else
            logger.info(s"Found ${officers.size} Companies House officers matching member name query")
            Ok(memberNameMatchesView(
              form = CompaniesHouseOfficerForm.form(officers).fill:
                request.agentApplication.getCompanyOfficer
              ,
              officers = officers
            ))

  def submit: Action[AnyContent] = baseAction.async:
    implicit request =>
      companiesHouseService.getCompaniesHouseOfficers(
        companyRegistrationNumber = request.agentApplication.getCompanyRegistrationNumber,
        lastName = request.agentApplication.getLastNameFromQuery,
        isLlp = true // LLP members only
      ).flatMap { officers =>
        if (officers.isEmpty) {
          Future.successful(
            Ok(noMemberNameMatchesView(
              h1 = "No member name matches",
              bodyText = Some("placeholder for no matches")
            ))
          ).map(SubmissionHelper.redirectIfSaveForLater(request, _))
        }
        else if (officers.size == 1) {
          val formValue: Option[Seq[String]] = request
            .body
            .asFormUrlEncoded
            .getOrElse(Map.empty)
            .get(CompaniesHouseOfficerForm.key)
          if (formValue.exists(_.contains(officers.head.toString))) {
            // user has confirmed the single match is correct
            val updatedApplication = request.agentApplication
              .modify(_.applicantContactDetails.each.applicantName)
              .setTo(ApplicantName.NameOfMember(
                memberNameQuery = request.agentApplication.memberNameQuery,
                companiesHouseOfficer = Some(officers.head)
              ))
            applicationService.upsert(updatedApplication)
              .map(_ =>
                Redirect(routes.TelephoneNumberController.show.url)
              ).map(SubmissionHelper.redirectIfSaveForLater(request, _))
          }
          else if (formValue.contains(Seq("No"))) {
            // user has indicated the single match is not correct, so redirect to where user can
            // say they are not a member to provide a different name
            Future.successful(
              Redirect(routes.ApplicantRoleInLlpController.show.url)
            ).map(SubmissionHelper.redirectIfSaveForLater(request, _))
          }
          else {
            // user has not supplied any answer
            val formWithErrors = CompaniesHouseOfficerForm.form(officers)
              .withError(CompaniesHouseOfficerForm.key, s"${CompaniesHouseOfficerForm.key}.single.error.required")
            Future.successful(
              BadRequest(memberNameMatchesView(
                form = formWithErrors,
                officers = officers
              )).pipe(SubmissionHelper.redirectIfSaveForLater(request, _))
            )
          }
        }
        else {
          CompaniesHouseOfficerForm.form(officers)
            .bindFromRequest()
            .fold(
              formWithErrors =>
                Future.successful(
                  BadRequest(memberNameMatchesView(
                    form = formWithErrors,
                    officers = officers
                  ))
                ).map(SubmissionHelper.redirectIfSaveForLater(request, _)),
              validFormData => {
                val updatedApplication = request.agentApplication
                  .modify(_.applicantContactDetails.each.applicantName)
                  .setTo(ApplicantName.NameOfMember(
                    memberNameQuery = request.agentApplication.memberNameQuery,
                    companiesHouseOfficer = Some(validFormData)
                  ))
                applicationService.upsert(updatedApplication)
                  .map(_ =>
                    Redirect(routes.TelephoneNumberController.show.url)
                  ).map(SubmissionHelper.redirectIfSaveForLater(request, _))
              }
            )
        }
      }

  extension (agentApplication: AgentApplication)
    def memberNameQuery: Option[CompaniesHouseNameQuery] =
      for
        acd <- agentApplication.applicantContactDetails
        nameOfMember <- acd.applicantName.as[ApplicantName.NameOfMember]
        query <- nameOfMember.memberNameQuery
      yield query

  extension (agentApplication: AgentApplication)
    def getLastNameFromQuery: String =
      agentApplication.memberNameQuery.getOrThrowExpectedDataMissing("memberNameQuery is not defined")
        .lastName

  extension (agentApplication: AgentApplication)
    def getCompanyOfficer: Option[CompaniesHouseOfficer] =
      for
        acd <- agentApplication.applicantContactDetails
        nameOfMember <- acd.applicantName.as[ApplicantName.NameOfMember]
        companyOfficer <- nameOfMember.companiesHouseOfficer
      yield companyOfficer
