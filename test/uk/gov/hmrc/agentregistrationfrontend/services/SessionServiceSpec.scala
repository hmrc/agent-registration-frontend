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

import play.api.mvc.Results.Ok
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Session
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.AgentType
import uk.gov.hmrc.agentregistration.shared.BusinessType
import uk.gov.hmrc.agentregistrationfrontend.model.BusinessTypeAnswer
import uk.gov.hmrc.agentregistrationfrontend.services.SessionService.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

class SessionServiceSpec
extends UnitSpec:

  implicit val request: FakeRequest[AnyContentAsEmpty.type] = TdAll.tdAll.baseRequest
  val result: Result = Ok("").withSession("some-preexisting-key" -> "some-value")

  "PartnershipType" should:
    BusinessType.Partnership.values.foreach: pt =>
      s"$pt can be added to the Result and read back from the Request" in:
        val newResult = result
          .addSession(pt)

        newResult
          .asRequest
          .readPartnershipType shouldBe Some(pt)

        newResult.newSession.value.get(
          "agent-registration-frontend.partnershipType"
        ).value shouldBe pt.toString withClue "data should be stored under 'agent-registration-frontend.partnershipType' session key"

        newResult.newSession.value.get(
          "some-preexisting-key"
        ).value shouldBe "some-value" withClue "preexisting session data should not be affected"

    "readPartnershipType should throw exception if the stored data can't be deserialised to enum value " in:
      val throwable: RuntimeException = intercept[RuntimeException]:
        request
          .withSession("agent-registration-frontend.partnershipType" -> "garbage")
          .readPartnershipType

      throwable.getMessage shouldBe "Invalid Partnership type in session: 'garbage'"

    "readPartnershipType should return None when partnership type is not present in session" in:
      request.readPartnershipType shouldBe None

    "removePartnershipType should remove partnership type from session" in:
      val newResult = result
        .addSession(BusinessType.Partnership.LimitedLiabilityPartnership)

      newResult
        .asRequest
        .readPartnershipType shouldBe Some(BusinessType.Partnership.LimitedLiabilityPartnership)

      val resultAfterRemoval =
        newResult
          .removePartnershipTypeFromSession

      resultAfterRemoval
        .asRequest
        .readPartnershipType shouldBe None withClue "partnership type should be removed from session"

  "BusinessType" should:
    BusinessTypeAnswer.values.foreach: bt =>
      s"$bt can be added to the Result and read back from the Request" in:
        val newResult = result
          .addToSession(bt)

        newResult
          .asRequest
          .readBusinessTypeAnswer shouldBe Some(bt)

        newResult.newSession.value.get(
          "agent-registration-frontend.businessType"
        ).value shouldBe bt.toString withClue "data should be stored under 'agent-registration-frontend.businessType' session key"

        newResult.newSession.value.get(
          "some-preexisting-key"
        ).value shouldBe "some-value" withClue "preexisting session data should not be affected"

    "readBusinessType should throw exception if the stored data can't be deserialised to enum value " in:
      val throwable: RuntimeException = intercept[RuntimeException]:
        request
          .withSession("agent-registration-frontend.businessType" -> "garbage")
          .readBusinessTypeAnswer

      throwable.getMessage shouldBe "Invalid BusinessTypeSessionValue type in session: 'garbage'"

    "readBusinessType should return None when business type is not present in session" in:
      request.readBusinessTypeAnswer shouldBe None

  "AgentType" should:
    AgentType.values.foreach: bt =>
      s"$bt can be added to the Result and read back from the Request" in:
        val newResult = result
          .addToSession(bt)

        newResult
          .asRequest
          .readAgentType shouldBe Some(bt)

        newResult.newSession.value.get(
          "agent-registration-frontend.agentType"
        ).value shouldBe bt.toString withClue "data should be stored under 'agent-registration-frontend.agentType' session key"

        newResult.newSession.value.get(
          "some-preexisting-key"
        ).value shouldBe "some-value" withClue "preexisting session data should not be affected"

    "readAgentType should throw exception if the stored data can't be deserialised to enum value " in:
      val throwable: RuntimeException = intercept[RuntimeException]:
        request
          .withSession("agent-registration-frontend.agentType" -> "garbage")
          .readAgentType

      throwable.getMessage shouldBe "Invalid AgentType type in session: 'garbage'"

    "readAgentType should return None when business type is not present in session" in:
      request.readAgentType shouldBe None

  extension (result: Result)
    def asRequest: Request[?] = request.withSession(result.newSession.getOrElse(Session()).data.toSeq*)
