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
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.routes as applyRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.aboutyourbusiness.routes as aboutyourbusinessRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls.routes as amlsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.amls.api.routes as amlsApiRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.checkfailed.routes as entitycheckfailedRoutes

import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.applicantcontactdetails.routes as applicantcontactdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.agentdetails.routes as agentdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.routes as listdetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.nonincorporated.routes as listdetailsNonIncorporatedRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.otherrelevantindividuals.routes as listdetailsOthersRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.link.routes as listdetailsLinkRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.listdetails.progress.routes as listdetailsProgressRoutes
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.internal.routes as internalRoutes

import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.routes as providedetailsRoutes
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.routes as testOnlyRoutes
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.applicant.routes as testOnlyApplicantRoutes
import uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.individual.routes as testOnlyIndividualRoutes

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

      val CheckYourAnswersController = listdetailsRoutes.CheckYourAnswersController

      object nonincorporated:

        val NumberOfKeyIndividualsController = listdetailsNonIncorporatedRoutes.NumberOfKeyIndividualsController
        val EnterKeyIndividualController = listdetailsNonIncorporatedRoutes.EnterKeyIndividualController
        val ChangeKeyIndividualController = listdetailsNonIncorporatedRoutes.ChangeKeyIndividualController
        val RemoveKeyIndividualController = listdetailsNonIncorporatedRoutes.RemoveKeyIndividualController
        val CheckYourAnswersController = listdetailsNonIncorporatedRoutes.CheckYourAnswersController

      object otherrelevantindividuals:

        val ConfirmOtherRelevantIndividualsController = listdetailsOthersRoutes.ConfirmOtherRelevantIndividualsController
        val EnterOtherRelevantIndividualController = listdetailsOthersRoutes.EnterOtherRelevantIndividualController
        val ChangeOtherRelevantIndividualController = listdetailsOthersRoutes.ChangeOtherRelevantIndividualController
        val RemoveOtherRelevantIndividualController = listdetailsOthersRoutes.RemoveOtherRelevantIndividualController
        val CheckYourAnswersController = listdetailsOthersRoutes.CheckYourAnswersController

      object link:

        val LinkController = listdetailsLinkRoutes.LinkController

      object progress:

        val CheckProgressController = listdetailsProgressRoutes.CheckProgressController

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
    val ExitController = providedetailsRoutes.ExitController
    val IndividualTelephoneNumberController = providedetailsRoutes.IndividualTelephoneNumberController
    val IndividualEmailAddressController = providedetailsRoutes.IndividualEmailAddressController
    val IndividualDateOfBirthController = providedetailsRoutes.IndividualDateOfBirthController
    val IndividualNinoController = providedetailsRoutes.IndividualNinoController
    val IndividualSaUtrController = providedetailsRoutes.IndividualSaUtrController
    val IndividualApproveApplicantController = providedetailsRoutes.IndividualApproveApplicantController
    val IndividualHmrcStandardForAgentsController = providedetailsRoutes.IndividualHmrcStandardForAgentsController
    val IndividualConfirmStopController = providedetailsRoutes.IndividualConfirmStopController
    val CheckYourAnswersController = providedetailsRoutes.CheckYourAnswersController
    val IndividualConfirmationController = providedetailsRoutes.IndividualConfirmationController
    val MatchIndividualProvidedDetailsController = providedetailsRoutes.MatchIndividualProvidedDetailsController
    val NameMatchingController = providedetailsRoutes.NameMatchingController
    val NameMatchConfrimationController = providedetailsRoutes.NameMatchConfirmationController
    val ContactApplicantController = providedetailsRoutes.ContactApplicantController

  object testOnly:

    val TestOnlyController = testOnlyRoutes.TestOnlyController
    val EmailVerificationPasscodesController = testOnlyRoutes.EmailVerificationPasscodesController

    object applicant:

      val TestOnlyController = testOnlyApplicantRoutes.TestOnlyController
      val GrsStubController = testOnlyApplicantRoutes.GrsStubController
      val FastForwardController = testOnlyApplicantRoutes.FastForwardController

    object individual:
      val TestOnlyController = testOnlyIndividualRoutes.TestOnlyController
