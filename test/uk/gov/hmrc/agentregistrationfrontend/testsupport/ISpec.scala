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

package uk.gov.hmrc.agentregistrationfrontend.testsupport

import com.google.inject.AbstractModule
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.Logging
import play.api.Mode
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceableModule
import play.api.test.DefaultTestServerFactory
import play.api.test.TestServerFactory
import play.core.server.ServerConfig
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistration.shared.LinkId
import uk.gov.hmrc.agentregistration.shared.LinkIdGenerator
import uk.gov.hmrc.agentregistrationfrontend.config.AmlsCodes
import uk.gov.hmrc.agentregistrationfrontend.config.CsvLoader
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll
import uk.gov.hmrc.agentregistrationfrontend.testsupport.wiremock.WireMockSupport

import java.time.Clock
import java.time.Instant

trait ISpec
extends AnyWordSpecLike,
  RichMatchers,
  BeforeAndAfterEach,
  GuiceOneServerPerSuite,
  WireMockSupport:

//  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)), interval = scaled(Span(300, Millis)))
  private val testServerPort = ISpec.testServerPort

  lazy val tdAll: TdAll = TdAll.tdAll
  lazy val frozenInstant: Instant = tdAll.nowAsInstant
  lazy given clock: Clock = tdAll.clock

  val CentralisedRoutes = uk.gov.hmrc.agentregistrationfrontend.controllers.AppRoutes

  protected def configMap: Map[String, Any] =
    Map[String, Any](
      "microservice.services.agent-registration.port" -> WireMockSupport.port,
      "microservice.services.auth.port" -> WireMockSupport.port,
      "microservice.services.companies-house-api-proxy.port" -> WireMockSupport.port,
      "microservice.services.email-verification.port" -> WireMockSupport.port,
      "microservice.services.enrolment-store-proxy.port" -> WireMockSupport.port,
      "microservice.services.sole-trader-identification-frontend.port" -> WireMockSupport.port,
      "microservice.services.incorporated-entity-identification-frontend.port" -> WireMockSupport.port,
      "microservice.services.partnership-identification-frontend.port" -> WireMockSupport.port,
      "microservice.services.upscan.port" -> WireMockSupport.port,
      "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "auditing.consumer.baseUri.port" -> WireMockSupport.port,
      "auditing.enabled" -> false,
      "auditing.traceRequests" -> false,
      "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
      "features.grs-stub" -> false
    ) ++ configOverrides

  protected def configOverrides: Map[String, Any] = Map[String, Any]()

  lazy val overridesModule: AbstractModule =
    new AbstractModule:
      override def configure(): Unit =
        bind(classOf[Clock]).toInstance(clock)
        bind(classOf[AmlsCodes]).toInstance(
          new AmlsCodes {
            override val amlsCodes: Map[AmlsCode, AmlsName] = CsvLoader.load("/testAmlsCodes.csv").map: kv =>
              (AmlsCode(kv._1), AmlsName(kv._2))
          }
        )
        bind(classOf[LinkIdGenerator]).toInstance(new LinkIdGenerator {
          override def nextLinkId(): LinkId = tdAll.linkId
        })

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(overridesModule)))
    .configure(configMap).build()

  override protected def testServerFactory: TestServerFactory = CustomTestServerFactory

  object CustomTestServerFactory
  extends DefaultTestServerFactory:
    override protected def serverConfig(app: Application): ServerConfig =
      val sc = ServerConfig(
        port = Some(testServerPort),
        sslPort = None,
        mode = Mode.Test,
        rootDir = app.path
      )
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))

  override def beforeEach(): Unit = super.beforeEach()

object ISpec
extends Logging:

  lazy val testServerPort: Int = 19001
