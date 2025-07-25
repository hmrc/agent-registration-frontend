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

package uk.gov.hmrc.agentregistrationfrontend.services

import uk.gov.hmrc.agentregistrationfrontend.model.application.Application
import uk.gov.hmrc.agentregistrationfrontend.model.application.ApplicationState
import uk.gov.hmrc.agentregistrationfrontend.model.application.SessionId

import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationFactory @Inject() (
  clock: Clock,
  applicationIdGenerator: ApplicationIdGenerator
):

  def makeNewApplication(sessionId: SessionId): Application = Application(
    _id = applicationIdGenerator.nextApplicationId(),
    createdAt = Instant.now(clock),
    sessionId = sessionId,
    applicationState = ApplicationState.InProgress,
    nino = None,
    utr = None
  )
