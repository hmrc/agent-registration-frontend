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

package uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.fixablefailures.soletraderfailures.provideddetails

import com.softwaremill.quicklens.modify
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import uk.gov.hmrc.agentregistration.shared.individual.IndividualProvidedDetails
import uk.gov.hmrc.agentregistration.shared.risking.IndividualFix
import uk.gov.hmrc.agentregistration.shared.risking.RiskingOutcomeIndividual
import uk.gov.hmrc.agentregistrationfrontend.controllers.applicant.ApplyStubHelper
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ControllerSpec

class CheckYourAnswersControllerSpec
extends ControllerSpec:

  override def configOverrides: Map[String, Any] =
    super.configOverrides ++ Map(
      "features.fixable-failures" -> true
    )

  private def applyIndividualFix: Seq[IndividualFix] => String => Seq[IndividualFix] =
    fixes =>
      failureCode =>
        fixes.map:
          case a: IndividualFix if a.toString === failureCode => a.modify(_.isConfirmed).setTo(Some(true))
          case other => other

  private def updatedRiskingOutcomeIndividual(
    riskingOutcomeIndividual: RiskingOutcomeIndividual,
    failureCode: String
  ): RiskingOutcomeIndividual.FailedFixable =
    riskingOutcomeIndividual match
      case fixableIndividual: RiskingOutcomeIndividual.FailedFixable =>
        fixableIndividual
          .modify(_.fixes)
          .setTo(applyIndividualFix(fixableIndividual.fixes)(failureCode))
      case _ => throw new IllegalStateException("Expected a FailedFixable outcome")

  private val path = "/agent-registration/conditions-not-yet-met/sole-trader/check-your-answers"
  private val agentApplication =
    tdAll
      .agentApplicationSoleTrader
      .riskingOutcomeEntityFailedFixableNoEntityFailures

  private val changeLinks: Map[String, String] = Map(
    "dateOfBirth" -> AppRoutes.fixablefailures.soletraderfailures.provideddetails.IndividualDateOfBirthController.show.url,
    "nino" -> AppRoutes.fixablefailures.soletraderfailures.provideddetails.IndividualNinoController.show.url,
    "nino-yesNo" -> AppRoutes.fixablefailures.soletraderfailures.provideddetails.IndividualNinoController.show.url,
    "saUtr" -> AppRoutes.fixablefailures.soletraderfailures.provideddetails.IndividualSaUtrController.show.url,
    "saUtr-yesNo" -> AppRoutes.fixablefailures.soletraderfailures.provideddetails.IndividualSaUtrController.show.url
  )

  "routes should have correct path and method" in:
    AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.show shouldBe Call(
      method = "GET",
      url = path
    )
    AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.submit shouldBe Call(
      method = "POST",
      url = path
    )
    AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.submit.url shouldBe AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.show.url

  private object individualProvideDetails:

    val complete: IndividualProvidedDetails = tdAll.providedDetails.afterRiskedFixableIndividualDetails
    val onlySaUtr: IndividualProvidedDetails = complete.copy(
      riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualDetailsFixOnlySaUtr)
    )
    val idsNotProvided: IndividualProvidedDetails = complete.copy(
      riskingOutcomeIndividual = Some(tdAll.riskingOutcomeIndividualDetailsFixWithoutIds)
    )

  private final case class TestCaseForCya(
    providedDetails: IndividualProvidedDetails,
    name: String,
    expectedChangeLinks: List[String]
  )
  // in the fixable failures journeys the CYA page always begins with all required values provided,
  // missing values (None) should NOT result in redirection, missing values in this context means we don't require them to be fixed!
  List(
    TestCaseForCya(
      providedDetails = individualProvideDetails.complete,
      name = "all details need fixing",
      expectedChangeLinks = List(
        changeLinks("dateOfBirth"),
        changeLinks("nino-yesNo"),
        changeLinks("nino"),
        changeLinks("saUtr-yesNo"),
        changeLinks("saUtr")
      )
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.onlySaUtr,
      name = "only SaUtr that needs fixing",
      expectedChangeLinks = List(
        changeLinks("saUtr-yesNo"),
        changeLinks("saUtr")
      )
    ),
    TestCaseForCya(
      providedDetails = individualProvideDetails.idsNotProvided,
      name = "only saUtr and nino need fixing",
      expectedChangeLinks = List(
        changeLinks("nino-yesNo"),
        changeLinks("saUtr-yesNo")
      )
    )
  ).foreach: testCase =>
    s"GET $path with ${testCase.name} should return 200 and render page" in:
      ApplyStubHelper.stubsForApplicationBprAndIndividuals(
        application = agentApplication,
        individuals = List(testCase.providedDetails)
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      doc.title() shouldBe "Check your answers - Apply for an agent services account - GOV.UK"

    s"GET $path with ${testCase.name} should render change links pointing to the correct controllers" in:
      ApplyStubHelper.stubsForApplicationBprAndIndividuals(
        application = agentApplication,
        individuals = List(testCase.providedDetails)
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      val changeLinkHrefs: List[String] =
        doc
          .mainContent
          .select(".govuk-summary-list__actions a")
          .eachAttr("href")
          .toArray
          .map(_.toString)
          .toList

      changeLinkHrefs shouldBe testCase.expectedChangeLinks

    s"GET $path with ${testCase.name} should render a submit button posting to the submit route" in:
      ApplyStubHelper.stubsForApplicationBprAndIndividuals(
        application = agentApplication,
        individuals = List(testCase.providedDetails)
      )
      val response: WSResponse = get(path)
      response.status shouldBe Status.OK
      val doc = response.parseBodyAsJsoupDocument
      val form = doc.mainContent.select("form")
      form.attr("method") shouldBe "POST"
      form.attr("action") shouldBe
        AppRoutes.fixablefailures.soletraderfailures.provideddetails.CheckYourAnswersController.submit.url
      doc.extractSubmitButtonText shouldBe "Confirm and continue"

    s"POST $path with ${testCase.name} should confirm the individual details fix and redirect to the fixable task list" in:
      ApplyStubHelper.stubFixableFailureUpdate(
        agentApplication = agentApplication,
        individualProvidedDetails = testCase.providedDetails,
        updatedIndividualProvidedDetails = Some(testCase.providedDetails.copy(
          riskingOutcomeIndividual = Some(updatedRiskingOutcomeIndividual(
            testCase.providedDetails.getRiskingOutcomeIndividual,
            "IndividualFix.10.IndividualDetailsFix"
          ))
        )),
        updatedApplication = None
      )
      val response: WSResponse = post(path)(Map.empty)
      response.status shouldBe Status.SEE_OTHER
      response.header(HeaderNames.LOCATION).value shouldBe
        AppRoutes.fixablefailures.FixableTaskListController.show.url
