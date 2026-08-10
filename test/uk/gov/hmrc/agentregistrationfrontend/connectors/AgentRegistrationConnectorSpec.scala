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

package uk.gov.hmrc.agentregistrationfrontend.connectors

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.github.tomakehurst.wiremock.client.WireMock as wm
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec

import scala.jdk.CollectionConverters.*

class AgentRegistrationConnectorSpec
extends ISpec:

  private val connectorLoggerName = "uk.gov.hmrc.agentregistrationfrontend.connectors.AgentRegistrationConnector"

  private def loggerContext(): LoggerContext =
    LoggerFactory.getILoggerFactory match
      case ctx: LoggerContext => ctx
      case other => fail(s"expected Logback LoggerContext, got ${other.getClass.getName}")

  private def attachListAppender(): ListAppender[ILoggingEvent] =
    val context = loggerContext()
    val logbackLogger = context.getLogger(connectorLoggerName)
    val appender = ListAppender[ILoggingEvent]()
    appender.setContext(context)
    appender.start()
    logbackLogger.setLevel(Level.ERROR)
    logbackLogger.addAppender(appender)
    appender

  private def detachAppender(appender: ListAppender[ILoggingEvent]): Unit =
    val _ = loggerContext().getLogger(connectorLoggerName).detachAppender(appender)

  "upsertApplication" should:
    "invoke data integrity checks and log violations at ERROR via RequestAwareLogger" in:
      val appender = attachListAppender()
      try
        val badApplication = tdAll.agentApplicationLlp.afterStarted.copy(
          submittedAt = Some(tdAll.nowAsInstant)
        )

        wm.stubFor(
          wm.post(wm.urlPathEqualTo("/agent-registration/application"))
            .willReturn(wm.aResponse().withStatus(200))
        )

        given RequestHeader = FakeRequest()
        val connector = app.injector.instanceOf[AgentRegistrationConnector]
        connector.upsertApplication(badApplication).futureValue

        val errorMessages: Seq[String] = appender.list.asScala.toSeq
          .filter(_.getLevel.equals(Level.ERROR))
          .map(_.getFormattedMessage)

        withClue(s"captured ERROR log lines:\n${errorMessages.mkString("\n")}\n"):
          errorMessages.exists(_.contains("submittedAt should not be defined in Started state")) shouldBe true
          errorMessages.exists(_.contains(s"applicationReference=${badApplication.applicationReference.value}")) shouldBe true
      finally
        detachAppender(appender)
