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

package uk.gov.hmrc.agentregistrationfrontend.controllers
// scalafix:off DisableSyntax

import uk.gov.hmrc.agentregistrationfrontend.controllers.routes as rootRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.routes as applyRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.aboutyourbusiness.routes as aboutyourbusinessRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls.routes as amlsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.amls.api.routes as amlsApiRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.checkfailed.routes as entitycheckfailedRoutes

import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.applicantcontactdetails.routes as applicantcontactdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.agentdetails.routes as agentdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.listdetails.routes as listdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.apply.internal.routes as internalRoutes

import uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.routes as providedetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.providedetails.internal.routes as internalProviDedetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.routes as testOnlyRoutes

/** All application routes centralized in one place for convenience and clarity. It helps avoid naming conflicts and makes route management easier.
  *
  * Please add new controllers here.
  */
object AppRoutes:

  val SignOutController = rootRoutes.SignOutController

  object apply:

    val AgentApplicationController = applyRoutes.AgentApplicationController
    val TaskListController = applyRoutes.TaskListController
    val SaveForLaterController = applyRoutes.SaveForLaterController
    val HmrcStandardForAgentsController = applyRoutes.HmrcStandardForAgentsController
    val DeclarationController = applyRoutes.DeclarationController

    object aboutyourbusiness:

      val AgentTypeController = aboutyourbusinessRoutes.AgentTypeController
      val TypeOfSignInController = aboutyourbusinessRoutes.TypeOfSignInController
      val PartnershipTypeController = aboutyourbusinessRoutes.PartnershipTypeController
      val BusinessTypeSessionController = aboutyourbusinessRoutes.BusinessTypeSessionController
      val UserRoleController = aboutyourbusinessRoutes.UserRoleController
      val CheckYourAnswersController = aboutyourbusinessRoutes.CheckYourAnswersController

    object amls:

      object api:
        val NotificationFromUpscanController = amlsApiRoutes.NotificationFromUpscanController

      val AmlsExpiryDateController = amlsRoutes.AmlsExpiryDateController
      val CheckYourAnswersController = amlsRoutes.CheckYourAnswersController
      val AmlsSupervisorController = amlsRoutes.AmlsSupervisorController
      val AmlsEvidenceUploadController = amlsRoutes.AmlsEvidenceUploadController
      val AmlsRegistrationNumberController = amlsRoutes.AmlsRegistrationNumberController

    object applicantcontactdetails:

      val ApplicantNameController = applicantcontactdetailsRoutes.ApplicantNameController
      val CheckYourAnswersController = applicantcontactdetailsRoutes.CheckYourAnswersController
      val EmailAddressController = applicantcontactdetailsRoutes.EmailAddressController
      val TelephoneNumberController = applicantcontactdetailsRoutes.TelephoneNumberController

    object agentdetails:

      val AgentBusinessNameController = agentdetailsRoutes.AgentBusinessNameController
      val AgentTelephoneNumberController = agentdetailsRoutes.AgentTelephoneNumberController
      val AgentEmailAddressController = agentdetailsRoutes.AgentEmailAddressController
      val AgentCorrespondenceAddressController = agentdetailsRoutes.AgentCorrespondenceAddressController
      val CheckYourAnswersController = agentdetailsRoutes.CheckYourAnswersController

    object listdetails:

      val ListDetailsController = listdetailsRoutes.ListDetailsController

    object checkfailed:

      val CanNotRegisterCompanyOrPartnershipController = entitycheckfailedRoutes.CanNotRegisterCompanyOrPartnershipController
      val CanNotRegisterController = entitycheckfailedRoutes.CanNotRegisterController
      val CanNotConfirmIdentityController = entitycheckfailedRoutes.CanNotConfirmIdentityController

    object internal:

      val InitiateAgentApplicationController = internalRoutes.InitiateAgentApplicationController
      val GrsController = internalRoutes.GrsController
      val AddressLookupCallbackController = internalRoutes.AddressLookupCallbackController
      val RefusalToDealWithController = internalRoutes.RefusalToDealWithController
      val DeceasedController = internalRoutes.DeceasedController
      val CompaniesHouseStatusController = internalRoutes.CompaniesHouseStatusController

  object providedetails:

    val StartController = providedetailsRoutes.StartController
    val CompaniesHouseNameQueryController = providedetailsRoutes.CompaniesHouseNameQueryController
    val CompaniesHouseMatchingController = providedetailsRoutes.CompaniesHouseMatchingController
    val ExitController = providedetailsRoutes.ExitController
    val IndividualTelephoneNumberController = providedetailsRoutes.IndividualTelephoneNumberController
    val IndividualEmailAddressController = providedetailsRoutes.IndividualEmailAddressController
    val IndividualNinoController = providedetailsRoutes.IndividualNinoController
    val IndividualSaUtrController = providedetailsRoutes.IndividualSaUtrController
    val IndividualApproveApplicantController = providedetailsRoutes.IndividualApproveApplicantController
    val IndividualHmrcStandardForAgentsController = providedetailsRoutes.IndividualHmrcStandardForAgentsController
    val IndividualConfirmStopController = providedetailsRoutes.IndividualConfirmStopController
    val CheckYourAnswersController = providedetailsRoutes.CheckYourAnswersController
    val IndividualConfirmationController = providedetailsRoutes.IndividualConfirmationController

    object internal:
      val InitiateIndividualProvideDetailsController = internalProviDedetailsRoutes.InitiateIndividualProvideDetailsController

  object testOnly:

    val TestOnlyController = testOnlyRoutes.TestOnlyController
    val GrsStubController = testOnlyRoutes.GrsStubController
    val FastForwardController = testOnlyRoutes.FastForwardController
    val EmailVerificationPasscodesController = testOnlyRoutes.EmailVerificationPasscodesController
