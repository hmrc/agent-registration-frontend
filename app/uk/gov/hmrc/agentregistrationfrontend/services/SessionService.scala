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
import uk.gov.hmrc.agentregistration.shared.util.RequiredDataExtensions.getOrThrowExpectedDataMissing
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

    def addToSession(at: AgentType)(using request: RequestHeader): Result = r.addingToSession(agentTypeKey -> at.toString)
    def addToSession(bt: BusinessTypeAnswer)(using request: RequestHeader): Result = r.addingToSession(businessTypeKey -> bt.toString)
    def addSession(pt: BusinessType.Partnership)(using request: RequestHeader): Result = r.addingToSession(partnershipTypeKey -> pt.toString)
    def addToSession(tos: TypeOfSignIn)(using request: RequestHeader): Result = r.addingToSession(typeOfSignInKey -> tos.toString)
    def removePartnershipTypeFromSession(using request: RequestHeader): Result = r.removingFromSession(partnershipTypeKey)

  extension (r: Request[?])

    def readAgentType: Option[AgentType] = r.session.get(agentTypeKey).map: value =>
      AgentType
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid AgentType type in session: '$value'"))

    def getAgentType: AgentType = readAgentType.getOrThrowExpectedDataMissing("AgentType")

    def readBusinessTypeAnswer: Option[BusinessTypeAnswer] = r.session.get(businessTypeKey).map: value =>
      BusinessTypeAnswer
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid BusinessTypeSessionValue type in session: '$value'"))
    def getBusinessTypeAnswer: BusinessTypeAnswer = readBusinessTypeAnswer.getOrThrowExpectedDataMissing("BusinessTypeAnswer")

    def readPartnershipType: Option[BusinessType.Partnership] = r.session.get(partnershipTypeKey).map: value =>
      BusinessType
        .Partnership
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid Partnership type in session: '$value'"))

    def getPartnershipType: BusinessType.Partnership = readPartnershipType.getOrThrowExpectedDataMissing("PartnershipType")

    def readTypeOfSignIn: Option[TypeOfSignIn] = r.session.get(typeOfSignInKey).map: value =>
      TypeOfSignIn
        .values
        .find(_.toString === value)
        .getOrElse(throw new RuntimeException(s"Invalid TypeOfSignIn type in session: '$value'"))

    def getTypeOfSignIn: TypeOfSignIn = readTypeOfSignIn.getOrThrowExpectedDataMissing("TypeOfSignIn")

    def readBusinessType: Option[BusinessType] = readBusinessTypeAnswer.flatMap:
      case BusinessTypeAnswer.SoleTrader => Some(BusinessType.SoleTrader)
      case BusinessTypeAnswer.LimitedCompany => Some(BusinessType.LimitedCompany)
      case BusinessTypeAnswer.Other => throw new NotImplementedError(s"readBusinessType: ${BusinessTypeAnswer.Other}")
      case BusinessTypeAnswer.PartnershipType => readPartnershipType

    def getBusinessType: BusinessType = readBusinessType.getOrThrowExpectedDataMissing("BusinessType")
