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

package uk.gov.hmrc.agentregistrationfrontend.config

import play.api.Configuration
import sttp.model.Uri
import sttp.model.Uri.UriContext
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

@Singleton
class AppConfig @Inject()(
                           servicesConfig: ServicesConfig,
                           configuration: Configuration
                         ):

  val thisFrontendBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.this-frontend", configuration)
  val feedbackFrontendBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.feedback-frontend", configuration)
  private val basFrontendSignBaseInBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.bas-gateway-sign-in", configuration)
  val basFrontendSignOutUrlBase: String = ConfigHelper.readConfigAsValidUrlString("urls.bas-gateway-sign-out", configuration)

  val asaDashboardUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.asa-fe-dashboard-url", configuration)
  val taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment: String = ConfigHelper.readConfigAsValidUrlString("urls.taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment", configuration)

  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  
  def signInUri(continueUri: Uri): Uri = uri"$basFrontendSignBaseInBaseUrl"
    .addParam("continue_url", continueUri.toString())
    .addParam("origin", "agent-registration-frontend")
    .addParam("accountType", "agent") // specifying this to bypass unnecessary screens intended for Individuals

  val welshLanguageSupportEnabled: Boolean = configuration.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  object ApplicationRepo:
    val applicationRepoTtl: FiniteDuration = ConfigHelper.readFiniteDuration("mongodb.application-repo-ttl", servicesConfig)

object ConfigHelper:

  /** The application loads the configuration from the provided `configPath` and checks if it's a valid URL. If it's not a valid URL, an exception is thrown.
   * This exception is triggered early during the application's startup to highlight a malformed configuration, thus increasing the chances of it being
   * rectified promptly.
   */
  def readConfigAsValidUrlString(
                                  configPath: String,
                                  configuration: Configuration
                                ): String =
    val url: String = configuration.get[String](configPath)
    Try(new java.net.URI(url).toURL).fold[String](
      e => throw new RuntimeException(s"Invalid URL in config under [$configPath], value was [$url]", e),
      _ => url
    )

  def readFiniteDuration(
                          configPath: String,
                          servicesConfig: ServicesConfig
                        ): FiniteDuration =
    servicesConfig.getDuration(configPath) match
      case d: FiniteDuration => d
      case _: Infinite => throw new RuntimeException(s"Infinite Duration in config for the key [$configPath]")
