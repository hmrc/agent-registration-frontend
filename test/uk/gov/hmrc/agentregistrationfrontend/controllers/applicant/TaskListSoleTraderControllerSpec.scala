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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant

import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class TaskListSoleTraderControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/task-list"
  // the base application needed for verify assertions in all states in this spec
  private val baseApplication = tdAll.agentApplicationSoleTrader.afterGrsDataReceived

  // these are the HTML document ids used in the task list template for this business type
  object tasks:

    val businessDetails = "businessDetails-1-status"
    val applicantContactDetails = "contact-1-status"
    val accountDetails = "accountDetails-1-status"
    val amls = "hmrcStandards-1-status"
    val hmrcStandards = "hmrcStandards-2-status"
    val proveIdentity = "lists-1-status"
    val declaration = "declaration-1-status"

  "route should have correct path and method" in:
    AppRoutes.apply.TaskListController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path for sole trader owner afterDeceasedCheckPass should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(tdAll.agentApplicationSoleTrader.afterDeceasedCheckPass, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)

  s"GET $path for sole trader owner afterContactDetailsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(tdAll.agentApplicationSoleTrader.afterContactDetailsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)

  s"GET $path for sole trader owner afterAgentDetailsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(tdAll.agentApplicationSoleTrader.afterAgentDetailsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)

  s"GET $path for sole trader owner afterAmlsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(tdAll.agentApplicationSoleTrader.afterAmlsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)

  s"GET $path for sole trader owner afterHmrcStandardForAgentsAgreed should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(tdAll.agentApplicationSoleTrader.afterHmrcStandardForAgentsAgreed, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)

  s"GET $path for sole trader owner after proved identity with individual record should render correct status tags" in:
    ApplyStubHelper.stubsForTaskListPage(
      application = tdAll.agentApplicationSoleTrader.afterHmrcStandardForAgentsAgreed,
      individuals = List(
        tdAll.providedDetails.afterProvidedDetailsConfirmed
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.proveIdentity) shouldBe Constants.COMPLETED // becomes COMPLETED by virtue of individual list having a completed record
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    ApplyStubHelper.verifyConnectorsForTaskListPage(baseApplication)
