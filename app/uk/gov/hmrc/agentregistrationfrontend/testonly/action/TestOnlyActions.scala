/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.action

import play.api.i18n.Messages
import play.api.i18n.MessagesApi
import play.api.mvc.*
import play.api.mvc.Results.NotFound
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetailsId
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuilders.refineUnion
import uk.gov.hmrc.agentregistrationfrontend.action.ActionBuildersWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.testonly.services.TestApplicationService
import uk.gov.hmrc.agentregistrationfrontend.testonly.views.html.SimplePage
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

/** Actions for test-only controllers that operate on an arbitrary application/individual looked up by an id from the URL (an unauthenticated "god view"), as
  * opposed to
  * [[uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions]]/[[uk.gov.hmrc.agentregistrationfrontend.action.individual.IndividualActions]]
  * which resolve "the current signed-in user's own application". Kept separate from those two rather than reusing their `action` bootstrap, since test-only
  * tooling has no auth step and shouldn't be coupled to either production journey.
  *
  * Note: `messagesApi`/`simplePage` are plain (non-`val`) constructor params rather than an `I18nSupport` mixin — a `val messagesApi` member here would get
  * pulled into any controller that does `export testOnlyActions.*` and collide with `FrontendControllerBase`'s own `messagesApi`.
  */
object TestOnlyActions:

  export uk.gov.hmrc.agentregistrationfrontend.action.Actions.*

  type DataWithApplication = AgentApplication *: EmptyData
  type RequestWithApplication = RequestWithData[DataWithApplication]

  type DataWithIndividual = IndividualProvidedDetails *: EmptyData
  type RequestWithIndividual = RequestWithData[DataWithIndividual]

@Singleton
class TestOnlyActions @Inject() (
  defaultActionBuilder: DefaultActionBuilder,
  testApplicationService: TestApplicationService,
  simplePage: SimplePage,
  messagesApi: MessagesApi
)(using ExecutionContext)
extends RequestAwareLogging:

  export ActionBuildersWithData.*
  export TestOnlyActions.*

  val action: ActionBuilderWithData[EmptyTuple] = defaultActionBuilder
    .refineUnion(request => RequestWithDataCt.empty(request))

  /** Fetches the `AgentApplication` for this `applicationReference` (an id taken directly from the URL, not the caller's own session), rendering a plain "not
    * found" test-only page instead of throwing if it doesn't exist.
    */
  def getApplication(applicationReference: ApplicationReference): ActionBuilderWithData[DataWithApplication] = action
    .refine:
      implicit request: RequestWithData[EmptyData] =>
        testApplicationService.findApplication(applicationReference).map:
          case Some(agentApplication) => request.add(agentApplication)
          case None =>
            given Messages = messagesApi.preferred(request)
            NotFound(simplePage(
              h1 = "Application not found",
              bodyText = Some(s"There is no application under given applicationReference: ${applicationReference.value}")
            ))

  /** As above, but keyed by `agentApplicationId` instead. */
  def getApplication(agentApplicationId: AgentApplicationId): ActionBuilderWithData[DataWithApplication] = action
    .refine:
      implicit request: RequestWithData[EmptyData] =>
        testApplicationService.findApplication(agentApplicationId).map:
          case Some(agentApplication) => request.add(agentApplication)
          case None =>
            given Messages = messagesApi.preferred(request)
            NotFound(simplePage(
              h1 = "Application not found",
              bodyText = Some(s"There is no application under given agentApplicationId: ${agentApplicationId.value}")
            ))

  /** Fetches the `IndividualProvidedDetails` for this `individualProvidedDetailsId` (an id taken directly from the URL), rendering a plain "not found"
    * test-only page instead of throwing if it doesn't exist.
    */
  def getIndividual(individualProvidedDetailsId: IndividualProvidedDetailsId): ActionBuilderWithData[DataWithIndividual] = action
    .refine:
      implicit request: RequestWithData[EmptyData] =>
        testApplicationService.findIndividual(individualProvidedDetailsId).map:
          case Some(individualProvidedDetails) => request.add(individualProvidedDetails)
          case None =>
            given Messages = messagesApi.preferred(request)
            NotFound(simplePage(
              h1 = "Individual not found",
              bodyText = Some(s"There is no individual under given individualProvidedDetailsId: ${individualProvidedDetailsId.value}")
            ))
