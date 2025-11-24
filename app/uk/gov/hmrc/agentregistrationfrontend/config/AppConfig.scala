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
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

@Singleton
class AppConfig @Inject() (
  servicesConfig: ServicesConfig,
  configuration: Configuration
):

  val govukStartPageUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.govuk-start-page", configuration)
  val thisFrontendBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.this-frontend", configuration)
  val feedbackFrontendBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.feedback-frontend", configuration)
  private val basFrontendSignBaseInBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.bas-gateway-sign-in", configuration)
  val basFrontendSignOutUrlBase: String = ConfigHelper.readConfigAsValidUrlString("urls.bas-gateway-sign-out", configuration)
  val emailVerificationFrontendBaseUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.email-verification-frontend", configuration)
  val asaDashboardUrl: String = ConfigHelper.readConfigAsValidUrlString("urls.asa-fe-dashboard-url", configuration)
  val taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment: String = ConfigHelper.readConfigAsValidUrlString(
    "urls.taxAndSchemeManagementToSelfServeAssignmentOfAsaEnrolment",
    configuration
  )
  val addressLookupFrontendBaseUrl: String = servicesConfig.baseUrl("address-lookup-frontend")
  val agentsExternalStubsBaseUrl: String = servicesConfig.baseUrl("agents-external-stubs")
  val companiesHouseApiProxyBaseUrl: String = servicesConfig.baseUrl("companies-house-api-proxy")
  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")
  val agentRegistrationBaseUrl: String = servicesConfig.baseUrl("agent-registration")
  val emailVerificationBaseUrl: String = servicesConfig.baseUrl("email-verification")
  val selfBaseUrl: String = servicesConfig.baseUrl("agent-registration-frontend")
  val hmrcAsAgentEnrolment: Enrolment = Enrolment(key = "HMRC-AS-AGENT")
  val citizenDetailsBaseUrl: String = servicesConfig.baseUrl("citizen-details")

  def signInUri(
    continueUri: Uri,
    affinityGroup: AffinityGroup
  ): Uri = uri"$basFrontendSignBaseInBaseUrl"
    .addParam("continue_url", continueUri.toString())
    .addParam("origin", "agent-registration-frontend")
    .addParam("affinityGroup", affinityGroup.toString.toLowerCase)

  def applicationLink(linkId: String): Uri = uri"$thisFrontendBaseUrl/agent-registration/provide-details/start/$linkId"

  val welshLanguageSupportEnabled: Boolean = configuration.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)
  val contactFrontendId: String = configuration.get[String]("contact-frontend.serviceId") // TODO placeholder
  val accessibilityStatementPath: String = configuration.get[String]("accessibility-statement.service-path")
  val ignoreEmailVerification: Boolean = configuration
    .getOptional[Boolean]("ignoreEmailVerification")
    .getOrElse(false)

  /*
   * GRS CONFIG START
   */
  val enableGrsStub: Boolean = configuration.getOptional[Boolean]("features.grs-stub").getOrElse(false)

  val soleTraderIdBaseUrl: String = servicesConfig.baseUrl("sole-trader-identification-frontend")
  val incorpIdBaseUrl: String = servicesConfig.baseUrl("incorporated-entity-identification-frontend")
  val partnershipIdBaseUrl: String = servicesConfig.baseUrl("partnership-identification-frontend")

  /*
   * UPSCAN CONFIG START
   */
  val upscanInitiateHost: String = servicesConfig.baseUrl("upscan")
  val upscanRedirectBase: String = configuration.get[String]("microservice.services.upscan.redirect-base")
  val fileUploadMaxPolls: Int = configuration.get[Int]("uploads.maximum-js-polls")
  val millisecondsBeforePoll: Int = configuration.get[Int]("uploads.milliseconds-before-poll")
  val upscanCallbackEndpoint: String = s"$agentRegistrationBaseUrl/agent-registration/upscan-callback"
  val maxFileSize: Int = configuration.get[Int]("uploads.max-file-size-in-bytes")
  val allowedCorsOrigin: String = configuration.get[String]("microservice.services.upscan.redirect-base")

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
