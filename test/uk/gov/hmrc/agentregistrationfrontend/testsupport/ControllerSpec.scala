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

package uk.gov.hmrc.agentregistrationfrontend.testsupport

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.AgentApplicationId
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.EnumExtensions.*
import uk.gov.hmrc.agentregistrationfrontend.applicant.model.BusinessTypeAnswer

class ControllerSpec
extends ISpec,
  WsHelper:

  export viewspecsupport.JsoupSupport.*
  export play.api.mvc.Call
  export play.api.http.Status

  object Constants:

    val EMPTY_STRING = ""
    val OTHER = "other"

  def addAgentTypeToSession(agentType: AgentType): WSResponse = get(
    s"/agent-registration/test-only/add-agent-type/${agentType.toStringHyphenated}"
  )

  def addBusinessTypeToSession(businessType: BusinessTypeAnswer): WSResponse = get(
    s"/agent-registration/test-only/add-business-type/${businessType.toStringHyphenated}"
  )

  def addPartnershipTypeToSession(partnershipType: BusinessType.Partnership): WSResponse = get(
    s"/agent-registration/test-only/add-partnership-type/${partnershipType.toString}"
  )

  def addAgentApplicationIdToSession(agentApplicationId: AgentApplicationId): WSResponse = get(
    s"/agent-registration/test-only/add-agent-applicationId/${agentApplicationId.value}"
  )
