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

package uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness

import play.api.mvc.*
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.BusinessType.SoleTrader
import uk.gov.hmrc.agentregistration.shared.UserRole
import uk.gov.hmrc.agentregistrationfrontend.action.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.forms.UserRoleForm
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.views.html.apply.aboutyourbusiness.UserRolePage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRoleController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  view: UserRolePage
)
extends FrontendController(mcc, actions):

  def show: Action[?] = actions.action:
    implicit request =>
      Ok(view(
        form = UserRoleForm.form.fill(request.readUserRole),
        userRoleOption = userRoleOptionForBusinessType(request.getBusinessType)
      ))

  def submit: Action[AnyContent] =
    actions.action
      .ensureValidForm4(UserRoleForm.form, implicit request => view(_, userRoleOptionForBusinessType(request.getBusinessType))):
        implicit request =>
          val userRole: UserRole = request.get
          Redirect(
            AppRoutes.apply.aboutyourbusiness.TypeOfSignInController.show.url
          ).addToSession(userRole)

  private def userRoleOptionForBusinessType(
    businessType: BusinessType
  ): UserRole =
    businessType match
      case SoleTrader => UserRole.Owner
      case BusinessType.Partnership.LimitedLiabilityPartnership => UserRole.Member
      case BusinessType.LimitedCompany => UserRole.Director
      case BusinessType.Partnership.GeneralPartnership => UserRole.Partner
      case BusinessType.Partnership.LimitedPartnership => UserRole.Partner
      case BusinessType.Partnership.ScottishPartnership => UserRole.Partner
      case BusinessType.Partnership.ScottishLimitedPartnership => UserRole.Partner
