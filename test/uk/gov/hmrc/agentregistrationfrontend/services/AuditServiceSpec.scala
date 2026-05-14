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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.defaultAwaitTimeout
import play.api.test.Helpers.status
import uk.gov.hmrc.agentregistration.shared.audit.SessionId
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.RequestWithData
import uk.gov.hmrc.agentregistrationfrontend.action.RequestWithDataCt
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions
import uk.gov.hmrc.agentregistrationfrontend.action.applicant.ApplicantActions.DataWithApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdSupport.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuditStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AuthStubs
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.stubs.AgentRegistrationStubs

import scala.concurrent.Future

class AuditServiceSpec
extends ISpec:

  private lazy val applicantActions = app.injector.instanceOf[ApplicantActions]

  override def configOverrides: Map[String, Any] = Map[String, Any](
    "auditing.enabled" -> true
  )

  "auditContinueApplication" should:

    "send a continue event when the same user logs back in with a new session" in:
      AuthStubs.stubAuthorise()
      AuditStubs.stubAudit()

      val applicationFromPreviousSession = tdAll.agentApplicationLlp.afterStarted.copy(cachedSessionId = SessionId("first-session-id"))

      val updatedApplication = tdAll.agentApplicationLlp.afterStarted.copy(cachedSessionId = SessionId("second-session-id"))

      AgentRegistrationStubs.stubGetAgentApplication(applicationFromPreviousSession)
      AgentRegistrationStubs.stubUpdateAgentApplication(updatedApplication)

      val secondLoggedInRequest: RequestWithData[EmptyTuple] = RequestWithDataCt.empty(
        FakeRequest()
          .withAuthTokenInSession("second-auth-token")
          .withSession("sessionId" -> "second-session-id")
      )

      status(runGetApplication(secondLoggedInRequest)).shouldBe(200)

      AgentRegistrationStubs.verifyGetAgentApplication()
      AgentRegistrationStubs.verifyUpdateAgentApplication()
      AuditStubs.verifyAuditEvent(auditType = "StartOrContinueApplication", journeyType = Some("Continue"))

  private def runGetApplication(
    request: RequestWithData[EmptyTuple]
  ): Future[Result] = applicantActions.getApplication.invokeBlock(
    request,
    (_: RequestWithData[DataWithApplication]) => Future.successful(play.api.mvc.Results.Ok)
  )
