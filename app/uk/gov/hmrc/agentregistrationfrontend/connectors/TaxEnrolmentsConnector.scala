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

package uk.gov.hmrc.agentregistrationfrontend.connectors

import play.api.libs.json.Json
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistration.shared.Arn
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext

@Singleton
class TaxEnrolmentsConnector @Inject() (
  appConfig: AppConfig,
  http: HttpClientV2
)(implicit
  val ec: ExecutionContext
)
extends Connector:

  val taxEnrolmentsBaseUrl: String = appConfig.taxEnrolmentsBaseUrl

  // EACD's ES6 API
  def addKnownFacts(
    arn: String,
    knownFactKey: String,
    knownFactValue: String
  )(implicit
    rh: RequestHeader
  ): Future[Unit] =
    val request = KnownFactsRequest(List(KnownFact(knownFactKey, knownFactValue)), None)
    val url = url"$taxEnrolmentsBaseUrl/tax-enrolments/enrolments/${enrolmentKey(arn)}"
    http
      .put(url)
      .withBody(Json.toJson(request))
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
      .andLogOnFailure(s"Failed add known facts for $arn")

  // EACD's ES8 API
  def enrol(
    groupId: String,
    arn: Arn,
    enrolmentRequest: EnrolmentRequest
  )(implicit
    rh: RequestHeader
  ): Future[Unit] =
    val url = url"$taxEnrolmentsBaseUrl/tax-enrolments/groups/$groupId/enrolments/${enrolmentKey(arn.value)}"
    http
      .post(url)
      .withBody(Json.toJson(enrolmentRequest))
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
      .andLogOnFailure(s"Failed enrol $arn")

case class Legacy(previousVerifiers: Seq[KnownFact])

object Legacy:
  given format: OFormat[Legacy] = Json.format

case class KnownFactsRequest(
  verifiers: Seq[KnownFact],
  legacy: Option[Legacy]
)

object KnownFactsRequest:
  given format: OFormat[KnownFactsRequest] = Json.format

case class KnownFact(
  key: String,
  value: String
)

object KnownFact:
  given format: OFormat[KnownFact] = Json.format

case class EnrolmentRequest(
  userId: String,
  `type`: String,
  friendlyName: String,
  verifiers: Seq[KnownFact]
)

object EnrolmentRequest:
  given formats: OFormat[EnrolmentRequest] = Json.format

private def enrolmentKey(arn: String): String = s"HMRC-AS-AGENT~AgentReferenceNumber~$arn"
