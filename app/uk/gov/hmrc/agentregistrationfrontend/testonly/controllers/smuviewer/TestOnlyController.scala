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

package uk.gov.hmrc.agentregistrationfrontend.testonly.controllers.smuviewer

import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.FrontendController
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.TestSmuViewerIndividualPage

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestOnlyController @Inject() (
  mcc: MessagesControllerComponents,
  actions: ApplicantActions,
  testApplicationService: TestApplicationService,
  testSmuViewerIndividualPage: TestSmuViewerIndividualPage
)
extends FrontendController(mcc, actions):

  def makeTestSmuViewerIndividual: Action[AnyContent] = Action
    .async:
      implicit request =>
        testApplicationService
          .makeTestSmuIndividual()
          .map((individualProvidedDetailsId: IndividualProvidedDetailsId) =>
            Ok(testSmuViewerIndividualPage(individualProvidedDetailsId))
          )
