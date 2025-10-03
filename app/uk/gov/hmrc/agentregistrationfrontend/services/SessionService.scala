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
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.model.TypeOfSignIn

object SessionService:

  private val microserviceName = "agent-registration-frontend"
  private val agentTypeKey: String = s"$microserviceName.agentType"
  private val businessTypeKey: String = s"$microserviceName.businessType"
  private val partnershipTypeKey: String = s"$microserviceName.partnershipType"
  private val typeOfSignInKey: String = s"$microserviceName.typeOfSignIn"

  extension (r: Result)

    def addAgentTypeToSession(at: AgentType)(implicit request: Request[?]): Result = r.addingToSession(agentTypeKey -> at.toString)
    def addBusinessTypeAnswerToSession(bt: BusinessTypeAnswer)(implicit request: Request[?]): Result = r.addingToSession(businessTypeKey -> bt.toString)
    def addPartnershipTypeToSession(pt: BusinessType.Partnership)(implicit request: Request[?]): Result = r.addingToSession(partnershipTypeKey -> pt.toString)
    def addTypeOfSignInToSession(tos: TypeOfSignIn)(implicit request: Request[?]): Result = r.addingToSession(typeOfSignInKey -> tos.toString)
    def removePartnershipTypeFromSession(implicit request: RequestHeader): Result = r.removingFromSession(partnershipTypeKey)

  extension (r: Request[?])

    def readAgentType: Option[AgentType] = r.session.get(agentTypeKey).map: value =>
      AgentType
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid AgentType type in session: '$value'"))

    def readBusinessType: Option[BusinessTypeAnswer] = r.session.get(businessTypeKey).map: value =>
      BusinessTypeAnswer
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid BusinessTypeSessionValue type in session: '$value'"))

    def readPartnershipType: Option[BusinessType.Partnership] = r.session.get(partnershipTypeKey).map: value =>
      BusinessType
        .Partnership
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid Partnership type in session: '$value'"))

    def readTypeOfSignIn: Option[TypeOfSignIn] = r.session.get(typeOfSignInKey).map: value =>
      TypeOfSignIn
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid TypeOfSignIn type in session: '$value'"))

    //
    def requireAgentTypeAndBusinessType: (AgentType, BusinessType) =
      val at = readAgentType.getOrElse(throw new RuntimeException("No AgentType in session"))
      val bt = readPartnershipType
        .getOrElse(
          readBusinessType
            .getOrElse(throw new RuntimeException("No BusinessType in session"))
            .toBusinessType
            .getOrElse(throw new RuntimeException("No BusinessType in session"))
        )
      (at, bt)
