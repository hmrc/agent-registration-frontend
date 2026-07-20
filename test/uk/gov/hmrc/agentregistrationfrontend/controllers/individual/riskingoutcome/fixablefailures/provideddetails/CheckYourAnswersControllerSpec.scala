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

package uk.gov.hmrc.agentregistrationfrontend.controllers.individual.riskingoutcome.fixablefailures.provideddetails

import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import play.api.mvc.Call
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistrationfrontend.controllers.individual.ProvideDetailsStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  val linkId: LinkId = tdAll.linkId

  private val path = s"/agent-registration/provide-details/conditions-not-yet-met/check-your-answers/${linkId.value}"
  private val agentApplication =
    tdAll
      .agentApplicationLlp
      .afterRiskingCompletedApprovedWithFixableIndividuals

  "route should have correct path and method" in:
    AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.show(linkId) shouldBe Call(
      method = "GET",
      url = path
    )

  private object individualProvideDetails:

    val complete: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetails
    val missingSaUtr: IndividualProvidedDetails = tdAll.providedDetails.AfterNino.afterNinoProvided.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )
    val missingDateOfBirth: IndividualProvidedDetails = tdAll.providedDetails.afterEmailAddressVerified.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )
    val missingNino: IndividualProvidedDetails = tdAll.providedDetails.AfterDateOfBirth.afterDateOfBirthProvided.copy(riskingOutcomeIndividual =
      Some(tdAll.riskingOutcomeIndividualDetailsFixMissingSaUtr)
    )

  private final case class TestCaseForCya(
    providedDetails: IndividualProvidedDetails,
    name: String
  )
  // in the fixable failures journeys the CYA page always begins with all required values provided,
  // missing values (None) should NOT result in redirection, missing values in this context means we don't require them to be fixed!
  List(
    TestCaseForCya(
      providedDetails = individualProvideDetails.complete,
      name = "all details provided"
    )
  ).foreach: testCase =>
    s"GET $path with ${testCase.name} should return 200 and render page" in:
      ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
        agentApplication,
        testCase.providedDetails
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

    s"GET $path with ${testCase.name} should render change links pointing to the correct controllers" in:
      ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
        agentApplication,
        testCase.providedDetails
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      val changeLinkHrefs: Seq[String] = doc
        .mainContent
        .select(".govuk-summary-list__actions a")
        .eachAttr("href")
        .toArray
        .toSeq
        .map(_.toString)

      changeLinkHrefs should contain(
        AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualDateOfBirthController.show(linkId).url
      )
      changeLinkHrefs should contain(
        AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualNinoController.show(linkId).url
      )
      changeLinkHrefs should contain(
        AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.IndividualSaUtrController.show(linkId).url
      )

    s"GET $path with ${testCase.name} should render a submit button posting to the submit route" in:
      ProvideDetailsStubHelper.stubAuthAndMatchIndividualProvidedDetails(
        agentApplication,
        testCase.providedDetails
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      val form = doc.mainContent.select("form")
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe
        AppRoutes.providedetails.riskingoutcome.fixablefailures.provideddetails.CheckYourAnswersController.submit(linkId).url
      doc.extractSubmitButtonText shouldBe "Confirm and continue"

    s"POST $path with ${testCase.name} should confirm the individual details fix and redirect to the fixable task list" in:
      ProvideDetailsStubHelper.stubFixableFailureUpdate(
        agentApplication = agentApplication,
        individualProvidedDetails = testCase.providedDetails,
        updatedIndividualProvidedDetails = testCase.providedDetails
      )
      val response: WSResponse = post(path)(Map.empty)
      response.status shouldBe Status.SEE_OTHER
      response.header(HeaderNames.LOCATION).value shouldBe
        AppRoutes.providedetails.riskingoutcome.fixablefailures.FixableTaskListController.show(linkId).url
