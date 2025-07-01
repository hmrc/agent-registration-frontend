/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock

import com.github.tomakehurst.wiremock.client.{MappingBuilder, WireMock => wm}
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders}
import com.github.tomakehurst.wiremock.matching.{ContentPattern, RequestPatternBuilder, StringValuePattern, UrlPattern}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status

import scala.jdk.CollectionConverters.{MapHasAsJava, SeqHasAsJava}
import scala.util.chaining.scalaUtilChainingOps



object StubMaker {
  val wireMockPort: Int = 11112

  sealed trait HttpMethod
  object HttpMethod {
    case object GET    extends HttpMethod
    case object POST   extends HttpMethod
    case object DELETE extends HttpMethod
    case object PUT    extends HttpMethod
  }

  def make(
                   httpMethod: HttpMethod = HttpMethod.GET,
                   urlPattern: UrlPattern = wm.urlPathEqualTo("/example/path"),
                   queryParams: Map[String, StringValuePattern] = Map.empty,
                   requestHeaders: Seq[(String, StringValuePattern)] = Nil,
                   requestBody: ContentPattern[_] = null, // for example equalToJson(Json.prettyPrint(Json.toJson(caseClass)))
                   responseBody: String = "",
                   responseStatus: Int = Status.OK,
                   responseHeaders: Seq[(String, String)] = Nil,
                   atPriority: Integer = null
                 ): StubMapping = requestHeaders
    .foldLeft(initialMappingBuilder(httpMethod)(urlPattern))((acc, c) => acc.withHeader(c._1, c._2))
    .withQueryParams(queryParams.asJava)
    .pipe(mb => Option(requestBody).fold(mb)(mb.withRequestBody))
    .pipe(mb => Option(atPriority).fold(mb)(mb.atPriority(_)))
    .willReturn(
      wm.aResponse()
        .withStatus(responseStatus)
        .withBody(responseBody)
        .withHeaders(new HttpHeaders(responseHeaders.map(t => new HttpHeader(t._1, t._2)).asJava))
    )
    .pipe(wm.stubFor)

  def verify(
              httpMethod: HttpMethod = HttpMethod.GET,
              urlPattern: UrlPattern = wm.urlPathEqualTo("/example/path"),
              queryParams: Map[String, StringValuePattern] = Map.empty,
              requestHeaders: Seq[(String, StringValuePattern)] = Nil,
              count: Int = 1
            ): Unit = requestHeaders
    .foldLeft(initialRequestPatternBuilder(httpMethod)(urlPattern))((acc, c) => acc.withHeader(c._1, c._2))
    .pipe(rpb => queryParams.foldLeft(rpb)((acc, c) => acc.withQueryParam(c._1, c._2)))
    .tap(rpb => wm.verify(wm.exactly(count), rpb))
    .tap(_ => ())

  private def initialMappingBuilder(httpMethod: HttpMethod): UrlPattern => MappingBuilder = httpMethod match {
    case HttpMethod.GET    => wm.get
    case HttpMethod.POST   => wm.post
    case HttpMethod.DELETE => wm.delete
    case HttpMethod.PUT    => wm.put
  }

  private def initialRequestPatternBuilder(httpMethod: HttpMethod): UrlPattern => RequestPatternBuilder =
    httpMethod match {
      case HttpMethod.GET    => wm.getRequestedFor
      case HttpMethod.POST   => wm.postRequestedFor
      case HttpMethod.DELETE => wm.deleteRequestedFor
      case HttpMethod.PUT    => wm.putRequestedFor
    }
}
