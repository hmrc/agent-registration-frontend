/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.agentregistrationfrontend.pagesupport

import org.openqa.selenium.WebDriver

class Endpoint(baseUrl: String, val path: String)(implicit webDriver: WebDriver) {
  final def url: String = baseUrl + path
  final def open(): Unit = webDriver.get(url)
}
