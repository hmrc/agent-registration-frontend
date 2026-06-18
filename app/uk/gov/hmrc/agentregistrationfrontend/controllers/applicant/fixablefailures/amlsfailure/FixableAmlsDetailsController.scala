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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.amlsfailure

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.risking.EntityFix._3.AmlsFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeEntity
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.views.html.applicant.fixablefailures.amls.FixableAmlsDetailsPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FixableAmlsDetailsController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  detailsPage: FixableAmlsDetailsPage,
  appConfig: AppConfig
)
extends FrontendController(mcc, actions):

  def show(failureCode: String): Action[AnyContent] =
    actions.getApplicationAfterSentForRisking
      .behindFeatureFlag(appConfig.Features.fixableFailures)
      .ensure(
        implicit request =>
          request.agentApplication.getRiskingOutcomeEntity match
            case RiskingOutcomeEntity.FailedFixable(fixes) =>
              fixes.exists:
                // we check the failure code in the url is valid for this application
                case a: AmlsFix => a.failure.toString === failureCode
                case _ =>
                  logger.info(s"The failure code in the url $failureCode cannot be found in the AmlsFix.")
                  false
            case _ =>
              logger.info(s"The entity outcome on the application with application reference ${request.agentApplication.applicationReference.value} is not FailedFixable. Cannot display the Fixable AMLS details page.")
              false
        ,
        implicit request =>
          NotFound
      ):
        implicit request =>
          Ok(detailsPage(
            entityName = request.get[BusinessPartnerRecordResponse].getEntityName,
            failureCode = failureCode,
            amlsDetails = request.get[AgentApplication].getAmlsDetails
          ))
