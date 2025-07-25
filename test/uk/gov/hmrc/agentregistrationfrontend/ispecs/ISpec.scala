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

package uk.gov.hmrc.agentregistrationfrontend.ispecs

import com.google.inject.AbstractModule
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.{DefaultTestServerFactory, TestServerFactory}
import play.api.{Application, Logging, Mode}
import play.core.server.ServerConfig
import uk.gov.hmrc.agentregistrationfrontend.ispecs.wiremock.WireMockSupport
import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers
import uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata.TdAll

import java.time.{Clock, Instant, ZoneId}

trait ISpec
  extends AnyFreeSpecLike
    with RichMatchers
    with BeforeAndAfterEach
    with GuiceOneServerPerSuite
    with WireMockSupport:

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = scaled(Span(3, Seconds)), interval = scaled(Span(300, Millis)))
  private val testServerPort = ISpec.testServerPort
  private val baseUrl: String = s"http://localhost:${testServerPort.toString}"
  private val databaseName: String = "agent-registration-frontend-it"
  lazy val webdriverUrl: String = s"http://localhost:${port.toString}"

  lazy val tdAll: TdAll = TdAll()
  lazy val frozenInstant: Instant = tdAll.instant
  lazy val clock: Clock = Clock.fixed(frozenInstant, ZoneId.of("UTC"))

  protected def configMap: Map[String, Any] = Map[String, Any](
    "mongodb.uri" -> s"mongodb://localhost:27017/$databaseName",
    "play.http.router" -> "testOnlyDoNotUseInAppConf.Routes",
    "auditing.consumer.baseUri.port" -> WireMockSupport.port,
    "auditing.enabled" -> false,
    "auditing.traceRequests" -> false
  ) ++ configOverrides

  protected def configOverrides: Map[String, Any] = Map[String, Any]()


  lazy val overridesModule: AbstractModule = new AbstractModule:
    override def configure(): Unit =
      bind(classOf[Clock]).toInstance(clock)

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(GuiceableModule.fromGuiceModules(Seq(overridesModule)))
      .configure(configMap).build()

  override protected def testServerFactory: TestServerFactory = CustomTestServerFactory

  object CustomTestServerFactory extends DefaultTestServerFactory:
    override protected def serverConfig(app: Application): ServerConfig =
      val sc = ServerConfig(port = Some(testServerPort), sslPort = None, mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))

  override def beforeEach(): Unit =
    super.beforeEach()

//  lazy val pages = new Pages(baseUrl)

object ISpec extends Logging:

  lazy val testServerPort: Int = 19001
