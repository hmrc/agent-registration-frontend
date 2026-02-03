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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLlp
import uk.gov.hmrc.agentregistration.shared.BusinessPartnerRecordResponse
import uk.gov.hmrc.agentregistration.shared.GroupId
import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.auth.core.retrieve.Credentials

class RequestWithDataSpec
extends UnitSpec:

  "RequestWithData" should:

    val agentApplication: AgentApplication = TdAll.tdAll.agentApplicationLlp.afterStarted
    val internalUserId: InternalUserId = TdAll.tdAll.internalUserId
    val groupId: GroupId = TdAll.tdAll.groupId
    val credentials: Credentials = TdAll.tdAll.credentials
    val businessPartnerRecordResponse: BusinessPartnerRecordResponse = TdAll.tdAll.businessPartnerRecordResponse
    val maybeBusinessPartnerRecordResponse: Option[BusinessPartnerRecordResponse] = Some(businessPartnerRecordResponse)

    type ExampleData =
      (
        AgentApplication,
        InternalUserId,
        GroupId,
        Credentials
      )

    val request: RequestWithData[
      AnyContent,
      ExampleData
    ] = RequestWithData(
      request = FakeRequest(),
      data =
        (
          agentApplication,
          internalUserId,
          groupId,
          credentials
        )
    )

    "can get elements from data structure by type" in:
      request.get[AgentApplication] shouldBe agentApplication
      request.get[InternalUserId] shouldBe internalUserId
      request.get[GroupId] shouldBe groupId
      request.get[Credentials] shouldBe credentials

    "can add elements by data type" in:
      val r2: RequestWithData[
        AnyContent,
        (
          BusinessPartnerRecordResponse,
          AgentApplication,
          InternalUserId,
          GroupId,
          Credentials
        )
      ] = request.add(businessPartnerRecordResponse)

      r2.get[BusinessPartnerRecordResponse] shouldBe businessPartnerRecordResponse
      r2.data.tuple shouldBe (
        businessPartnerRecordResponse,
        agentApplication,
        internalUserId,
        groupId,
        credentials
      )

      val r3: RequestWithData[
        AnyContent,
        (
          Option[BusinessPartnerRecordResponse],
          BusinessPartnerRecordResponse,
          AgentApplication,
          InternalUserId,
          GroupId,
          Credentials
        )
      ] = r2.add(maybeBusinessPartnerRecordResponse)
      r3.get[Option[BusinessPartnerRecordResponse]] shouldBe maybeBusinessPartnerRecordResponse
      r3.data.tuple shouldBe (
        maybeBusinessPartnerRecordResponse,
        businessPartnerRecordResponse,
        agentApplication,
        internalUserId,
        groupId,
        credentials
      )

    "can update elements" in:
      val newAgentApplication: AgentApplication = TdAll.tdAll.agentApplicationLlp.afterGrsDataReceived
      val r2 = request.update(newAgentApplication)
      r2.get[AgentApplication] shouldBe newAgentApplication
      r2.get[InternalUserId] shouldBe internalUserId
      r2.data.tuple shouldBe (
        newAgentApplication,
        internalUserId,
        groupId,
        credentials
      )

    "can replace element types" in:
      val llp: AgentApplicationLlp = TdAll.tdAll.agentApplicationLlp.afterStarted
      val r2: RequestWithData[
        AnyContent,
        (AgentApplicationLlp, InternalUserId, GroupId, Credentials)
      ] = request.replace[AgentApplication, AgentApplicationLlp](llp)
      r2.get[AgentApplicationLlp] shouldBe llp
      r2.data.tuple shouldBe (
        llp,
        internalUserId,
        groupId,
        credentials
      )

    "can delete elements by type" in:
      val r2 = request.delete[Credentials]
      r2.data.tuple shouldBe (agentApplication, internalUserId, groupId)

//      r2.get[Credentials] // should not compile
