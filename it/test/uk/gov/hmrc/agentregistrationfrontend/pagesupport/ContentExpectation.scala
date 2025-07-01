/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.agentregistrationfrontend.pagesupport

final case class ContentExpectation(atXpath: String = PageUtil.Xpath.mainContent, expectedLines: String)
