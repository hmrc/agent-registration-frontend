/*
 * Copyright 2023 HM Revenue & Customs
 *
 */

package uk.gov.hmrc.agentregistrationfrontend.pagesupport

import org.openqa.selenium.WebDriver
import org.scalatestplus.selenium.WebBrowser._

abstract class Page(
    baseUrl:           String,
    override val path: String
)(implicit protected val webDriver: WebDriver)
  extends Endpoint(baseUrl, path) {

  def expectedH1: String
  def expectedWelshH1: String
  def clickEnglishLink()(implicit webDriver: WebDriver): Unit = click on xpath("""//a[@hreflang="en"]""")
  def clickWelshLink()(implicit webDriver: WebDriver): Unit = click on xpath("""//a[@hreflang="cy"]""")
  def clickBackButton()(implicit webDriver: WebDriver): Unit = click on xpath("""/html/body//a[@class="govuk-back-link"]""")
  def clickBackButtonInBrowser()(implicit webDriver: WebDriver): Unit = webDriver.navigate().back()
  def clickSignOut()(implicit webDriver: WebDriver): Unit = PageUtil.clickByClassName("hmrc-sign-out-nav__link")
  def clickSubmit()(implicit webDriver: WebDriver): Unit = PageUtil.clickByIdOrName("submit")
  def clickServiceName()(implicit webDriver: WebDriver): Unit = PageUtil.clickByXpath(Xpath.serviceName)

  protected def withPageClue[A](testF: => A)(implicit webDriver: WebDriver): A = PageUtil.withPageClue(path)(testF)

  def assertPageIsDisplayedWithTechnicalDifficultiesError(): Unit = withPageClue {
    PageUtil.assertPage(
      baseUrl   = baseUrl,
      path      = path,
      h1        = "Sorry, there is a problem with the service",
      title     = PageUtil.standardTitle("Sorry, there is a problem with the service - 500"),
      welshTest = false,
      ContentExpectation(
        expectedLines = """
                          |Sorry, there is a problem with the service
                          |Try again later
                          |""".stripMargin
      )
    )
    ()
  }

  def assertPageIsDisplayedWithTechnicalDifficultiesErrorInWelsh(): Unit = withPageClue {
    PageUtil.assertPage(
      baseUrl   = baseUrl,
      path      = path,
      h1        = "Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth",
      title     = PageUtil.standardTitleInWelsh("Mae’n ddrwg gennym, mae problem gyda’r gwasanaeth"),
      welshTest = true,
      ContentExpectation(
        expectedLines = """Rhowch gynnig arall arni yn nes ymlaen.
                          |
                          |Rhowch gynnig arall arni
                          |""".stripMargin
      )
    )
    ()
  }

  def assertPageIsDisplayed(welshTest: Boolean, extraExpectations: ContentExpectation*): Unit

  //Please don't add anything which is specific to only one page! Use specific Page for that.
  //Don't add public utility methods here. Use PageUtil for that.
}
