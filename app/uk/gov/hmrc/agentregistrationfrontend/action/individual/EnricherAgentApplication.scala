/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.action.individual

import com.google.inject.Inject
import com.google.inject.Singleton
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results
import uk.gov.hmrc.agentregistration.shared.*
import uk.gov.hmrc.agentregistration.shared.llp.IndividualProvidedDetailsToBeDeleted
import uk.gov.hmrc.agentregistrationfrontend.action.IndividualActions.*
import uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes
import uk.gov.hmrc.agentregistrationfrontend.services.AgentApplicationService
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class EnricherAgentApplication @Inject() (
  agentApplicationService: AgentApplicationService
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  inline def enrichRequest[Data <: Tuple](request: RequestWithData[Data])(using
    AgentApplication AbsentIn Data,
    IndividualProvidedDetailsToBeDeleted PresentIn Data
  ): Future[
    Result | RequestWithData[AgentApplication *: Data]
  ] =
    given RequestHeader = request
    agentApplicationService
      .find(request.individualProvidedDetails.agentApplicationId)
      .map:
        case Some(agentApplication) => request.add(agentApplication)
        case None =>
          // TODO this should be error page, not a generic exit page.
          Results.Redirect(AppRoutes.apply.AgentApplicationController.genericExitPage.url)
