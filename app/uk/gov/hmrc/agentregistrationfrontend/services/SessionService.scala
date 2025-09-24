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

package uk.gov.hmrc.agentregistrationfrontend.services

import play.api.mvc.Request
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

object SessionService:

  private val microserviceName = "agent-registration-frontend"
  private val businessTypeKey: String = s"$microserviceName.businessType"

  extension (r: Result)
    def addBusinessTypeToSession(bt: BusinessType): Result = r.addingToSession(businessTypeKey -> bt.toString)(request = null)

  extension (r: Request[?])
    def readBusinessType: Option[BusinessType] = r.session.get(businessTypeKey).map: value =>
      BusinessType
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid BusinessType type in session: '$value'"))
