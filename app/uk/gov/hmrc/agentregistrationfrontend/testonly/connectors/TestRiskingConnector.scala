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

package uk.gov.hmrc.agentregistrationfrontend.testonly.connectors

import play.api.libs.json.JsNull
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.connectors.Connector
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class TestRiskingConnector @Inject() (
  httpClient: HttpClientV2,
  appConfig: AppConfig
)(using
  ec: ExecutionContext
)
extends Connector:

  def deleteAllApplications()(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/applications"
    httpClient
      .delete(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case Status.NO_CONTENT => ()
          case other =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "DELETE",
              url = url,
              status = other,
              response = response
            )
      .andLogOnFailure("Failed to delete all risking Agent Applications")

  def uploadRiskingResultsFile(
    filename: String,
    body: JsValue
  )(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/risking-results-file/$filename"
    httpClient
      .post(url)
      .withBody(body)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to upload risking results file: $filename")

  def runRisking()(using RequestHeader): Future[Unit] =
    val url: URL = url"$baseUrl/run-risking"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to run risking")

  def listSubmittedRiskingResultsFilenames()(using RequestHeader): Future[Set[String]] =
    val url: URL = url"${appConfig.agentRegistrationRiskingBaseUrl}/files-available/list/informationTypePlaceholder"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => response.json.as[Seq[JsValue]].map(file => (file \ "filename").as[String]).toSet
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to list submitted risking results filenames")

  def viewRiskingResultsFile(filename: String)(using RequestHeader): Future[Option[String]] =
    val url: URL = url"$baseUrl/risking-results-file/$filename"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => Some(response.body)
          case status if status === Status.NOT_FOUND => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to view risking results file: $filename")

  /** Simulates SDES notifying agent-registration-risking that a file is ready to be picked up, which triggers it to process any pending risking results files.
    * The body content is only used for logging by the receiving endpoint — any well-formed FileReady notification will do.
    */
  def triggerRiskingResultsProcessing()(using RequestHeader): Future[Unit] =
    val url: URL = url"${appConfig.agentRegistrationRiskingBaseUrl}/agent-registration-risking/receive-sdes-notifications"
    val body: JsValue = Json.obj(
      "notification" -> "FileReady",
      "filename" -> "test-only-trigger.json",
      "correlationID" -> "test-only-trigger",
      "availableUntil" -> "2099-12-31T23:59:59Z",
      "dateTime" -> java.time.Instant.now().toString
    )
    httpClient
      .post(url)
      .withBody(body)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if is2xx(status) => ()
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "POST",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure("Failed to trigger risking results processing")

  def findApplicationForRisking(applicationReference: ApplicationReference)(using RequestHeader): Future[Option[JsValue]] =
    val url: URL = url"$baseUrl/application-for-risking/${applicationReference.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => Some(response.json)
          case status if status === Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find application for risking for applicationReference: ${applicationReference.value}")

  def findIndividualForRisking(personReference: PersonReference)(using RequestHeader): Future[Option[JsValue]] =
    val url: URL = url"$baseUrl/individual-for-risking/${personReference.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => Some(response.json)
          case status if status === Status.NO_CONTENT => None
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find individual for risking for personReference: ${personReference.value}")

  /** Not a test-only endpoint — this is the real, authenticated risking-progress endpoint the individual journey itself uses (see
    * AgentRegistrationRiskingConnector.getRiskingProgressForApplicant). It's authorised on the backend, so this call relies on the current request's own
    * session/auth being forwarded via the implicit HeaderCarrier.
    */
  def findRiskingProgress(applicationReference: ApplicationReference)(using RequestHeader): Future[JsValue] =
    val url: URL = url"${appConfig.agentRegistrationRiskingBaseUrl}/agent-registration-risking/risking-progress/for-applicant/${applicationReference.value}"
    httpClient
      .get(url)
      .execute[HttpResponse]
      .map: response =>
        response.status match
          case status if status === Status.OK => response.json
          case status if status === Status.NO_CONTENT => JsNull
          case status =>
            Errors.throwUpstreamErrorResponse(
              httpMethod = "GET",
              url = url,
              status = status,
              response = response
            )
      .andLogOnFailure(s"Failed to find risking progress for applicationReference: ${applicationReference.value}")

  private val baseUrl: String = appConfig.agentRegistrationRiskingBaseUrl + "/agent-registration-risking/test-only"
