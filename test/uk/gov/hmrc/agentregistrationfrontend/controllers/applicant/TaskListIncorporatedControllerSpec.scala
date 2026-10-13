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
import uk.gov.hmrc.agentregistration.shared.AgentApplicationLimitedCompany
import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class TaskListIncorporatedControllerSpec
extends ControllerSpec:

  private val path = "/agent-registration/apply/task-list"

  object agentApplication:

    // the base application needed for verify assertions in all states in this spec
    // the task list behaves the same for all incorporated business types so using agentApplicationLimitedCompany
    val grsDataReceived: AgentApplicationLimitedCompany = tdAll.agentApplicationLimitedCompany.afterGrsDataReceived
    val started: AgentApplicationLimitedCompany = tdAll.agentApplicationLimitedCompany.afterStarted
    val sentForRisking: AgentApplicationLimitedCompany = tdAll.agentApplicationLimitedCompany.afterDeclarationSubmitted

  // these are the HTML document ids used in the task list template for all incorporated business types
  object tasks:

    val businessDetails = "businessDetails-1-status"
    val applicantContactDetails = "contact-1-status"
    val accountDetails = "accountDetails-1-status"
    val amls = "hmrcStandards-1-status"
    val hmrcStandards = "hmrcStandards-2-status"
    val buildListOfIndividuals = "lists-1-status" // e.g. "Directors and other relevant individuals"
    val shareLinkWithIndividuals = "lists-2-status" // e.g. "Ask the directors and tax advisers to sign in"
    val checkProgressOfIndividuals = "lists-3-status" // e.g. "Check who has provided their details"
    val declaration = "declaration-1-status"

  "route should have correct path and method" in:
    AppRoutes.apply.TaskListController.show shouldBe Call(
      method = "GET",
      url = path
    )

  s"GET $path when status is before GrsDataReceived should redirect to start registration" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.started)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.startRegistration.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path when status is after GrsDataReceived should redirect to application status" in:
    ApplyStubHelper.stubsForAuthAction(agentApplication.sentForRisking)
    val response: WSResponse = get(path)

    response.status shouldBe Status.SEE_OTHER
    response.header("Location") shouldBe Some(AppRoutes.apply.AgentApplicationController.applicationStatus.url)
    ApplyStubHelper.verifyConnectorsForAuthAction()

  s"GET $path for limited company afterGlobalAsaEnrolmentCheckPass should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(tdAll.agentApplicationLimitedCompany.afterGlobalAsaEnrolmentCheckPass, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company afterContactDetailsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(tdAll.agentApplicationLimitedCompany.afterContactDetailsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company afterAgentDetailsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(tdAll.agentApplicationLimitedCompany.afterAgentDetailsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company afterAmlsComplete should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(tdAll.agentApplicationLimitedCompany.afterAmlsComplete, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company afterHmrcStandardForAgentsAgreed should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(tdAll.agentApplicationLimitedCompany.afterHmrcStandardForAgentsAgreed, List.empty)
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.INCOMPLETE
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company after building the list of individuals to sign in should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = tdAll.agentApplicationLimitedCompany.afterIndividualsDefined,
      individuals = List(
        tdAll.providedDetails.precreated.copy(individualName = IndividualName("Steve Austin")),
        tdAll.providedDetails.precreated.copy(individualName = IndividualName("Beverly Hills"))
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.CANNOT_START_YET
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company after ask individuals to sign in should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = tdAll.agentApplicationLimitedCompany.afterIndividualsDefined,
      individuals = List(
        tdAll.providedDetails.afterAccessConfirmed.copy(
          individualName = IndividualName("Steve Austin")
        ),
        tdAll.providedDetails.afterAccessConfirmed.copy(
          individualName = IndividualName("Beverly Hills")
        )
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.CANNOT_START_YET
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)

  s"GET $path for limited company after all individuals provided details should render correct status tags" in:
    ApplyStubHelper.stubsForApplicationBprAndIndividuals(
      application = tdAll.agentApplicationLimitedCompany.afterIndividualsDefined,
      individuals = List(
        tdAll.providedDetails.afterFinished.copy(
          individualName = IndividualName("Steve Austin")
        ),
        tdAll.providedDetails.afterFinished.copy(
          individualName = IndividualName("Beverly Hills")
        )
      )
    )
    val response: WSResponse = get(path)

    response.status shouldBe Status.OK
    val doc = response.parseBodyAsJsoupDocument
    doc.title() shouldBe "Application for Test Company Name - Apply for an agent services account - GOV.UK"
    doc.getTaskStatus(tasks.businessDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.applicantContactDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.accountDetails) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.amls) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.hmrcStandards) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.buildListOfIndividuals) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.shareLinkWithIndividuals) shouldBe Constants.COMPLETED
    doc.getTaskStatus(tasks.checkProgressOfIndividuals) shouldBe Constants.COMPLETED // becomes COMPLETED
    doc.getTaskStatus(tasks.declaration) shouldBe Constants.INCOMPLETE // becomes INCOMPLETE
    ApplyStubHelper.verifyConnectorsForApplicationBprAndIndividuals(agentApplication.grsDataReceived)
