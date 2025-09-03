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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.viewspecsupport

import org.jsoup.nodes.Element
import org.scalactic.source.Position
import sttp.model.Uri
import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers.*
import ElementSupport.*
import ElementsSupport.*

import scala.util.chaining.scalaUtilChainingOps

object ViewSelectors:

  import Selectors.*

  extension (element: Element)

    def hasLanguageSwitch: Boolean = element.select(languageSwitcher).headOption.nonEmpty
    def h1(using pos: Position): String = element.mainContent.selectOrFail(Selectors.h1).selectOnlyOneElementOrFail().text()
    def mainContent(using pos: Position): Element = element.selectOrFail(main).selectOnlyOneElementOrFail()

    inline def toLink(using pos: Position): TestLink = {
      element.tagName() shouldBe "a"
      TestLink(text = element.text(), href = element.selectAttrOrFail("href"))
    }

    inline def toInputField(using pos: Position): TestInputField = TestInputField(
      label = element.selectOrFail(label).selectOnlyOneElementOrFail().text(),
      hint = element.select(hint).headOption.map(_.text),
      inputName = element.selectOrFail(input).selectOnlyOneElementOrFail().selectAttrOrFail("name")
    )

    inline def extractLink(id: String)(using pos: Position): TestLink =
      element
        .selectOrFail(s"#$id")
        .selectOrFail(link)
        .selectOnlyOneElementOrFail()
        .toLink

    inline def extractLink(index: Int)(using pos: Position): TestLink =
      element
        .selectOrFail(link)
        .selectOrFail(index)
        .toLink

    inline def extractSubmitButtonText(using pos: Position): String = element
      .mainContent
      .selectOrFail("form button[type=submit]") // Note that button must be inside a form
      .selectOnlyOneElementOrFail()
      .text()

    inline def extractLinkButton(index: Int)(using pos: Position): TestLink =
      element
        .selectOrFail(button)
        .selectOrFail(index)
        .toLink

    inline def extractLinkButton(id: String)(using pos: Position): TestLink =
      element
        .selectOrFail(s"#$id")
        .selectOrFail(button)
        .selectOnlyOneElementOrFail()
        .toLink

    inline def extractRadioGroup(index: Int = 1): TestRadioGroup = element
      .selectOrFail(fieldSet)
      .selectOrFail(index)
      .pipe: element =>
        TestRadioGroup(
          legend = element.selectOrFail(fieldSetLegend).first().text(),
          options = element.selectOrFail(".govuk-radios__item").toList.map(el => (el.selectOrFail("label").text(), el.selectOrFail("input").attr("value"))),
          hint = element.select(fieldSetHint).toList.headOption.map(_.text)
        )

    inline def extractRadioGroupWithHints(index: Int = 1): TestRadioGroup = element
      .selectOrFail(fieldSet)
      .selectOrFail(index)
      .pipe: element =>
        TestRadioGroup(
          legend = element.selectOrFail(fieldSetLegend).first().text(),
          options = element.selectOrFail(".govuk-radios__item").toList.map(el => (el.selectOrFail("label").text(), el.selectOrFail("input").attr("value"))),
          hint = element.select(fieldSetHint).toList.headOption.map(_.text),
          optionHints = element.selectOrFail(".govuk-radios__item").toList.map(el =>
            (el.select("label").text(), el.select(".govuk-radios__hint").headOption.map(_.text()))
          )
        )

    inline def extractInputField(index: Int = 1): TestInputField = element
      .selectOrFail(formGroup)
      .selectOrFail(index)
      .pipe:
        _.toInputField

    inline def extractSelect(index: Int = 1): TestSelect = element
      .selectOrFail(select)
      .selectOrFail(index)
      .pipe: element =>
        TestSelect(
          inputName = element.select(select).selectOnlyOneElementOrFail().selectAttrOrFail("name"),
          options = element.select("option").toList.map(el => (el.selectAttrOrFail("value"), el.text()))
        )

    def toSelect: TestSelect = {
      element.tagName() shouldBe "select"
      TestSelect(
        inputName = element.selectAttrOrFail("name"),
        options = element.select("option").toList.map(el => (el.selectAttrOrFail("value"), el.text()))
      )
    }

    inline def extractSummaryList(index: Int = 1): TestSummaryList = element
      .selectOrFail(summaryList)
      .selectOrFail(index)
      .pipe: element =>
        TestSummaryList(
          rows = element.selectOrFail(summaryListRow).toList.map { (row: Element) =>
            val key = row.selectOrFail(summaryListRowKey).text()
            val value = row.selectOrFail(summaryListRowValue).text()
            val changeLink = row.selectOrFail(s"${summaryListRowActions} > a").selectOnlyOneElementOrFail().selectAttrOrFail("href")
            TestSummaryRow(
              key = key,
              value = value,
              action = changeLink
            )
          }
        )

    inline def extractTable(
      index: Int,
      numberOfCols: Int
    ): TestTable = element
      .selectOrFail(table)
      .selectOrFail(index)
      .pipe: element =>
        TestTable(
          caption = element.selectOrFail("caption").text(),
          rows = element.selectOrFail("tbody tr").toList.map { row =>
            for (i <- 0 until numberOfCols)
              yield row.selectOrFail("td").selectOrFail(i).text()
          }
        )

  final case class TestInputField(
    label: String,
    hint: Option[String],
    inputName: String
  )

  final case class TestLink(
    text: String,
    href: String
  ):

    def uri: Uri = Uri.parse(href).value
    def uriRelative: Uri = uri.copy(authority = None, scheme = None)

  final case class TestRadioGroup(
    legend: String,
    options: List[(String, String)],
    hint: Option[String],
    optionHints: List[(String, Option[String])] = Nil
  )

  final case class TestSelect(
    inputName: String,
    options: Seq[(String, String)]
  )

  final case class TestTable(
    caption: String,
    rows: List[IndexedSeq[String]]
  )

  final case class TestSummaryRow(
    key: String,
    value: String,
    action: String
  )

  final case class TestSummaryList(rows: List[TestSummaryRow])

  object Selectors:

    // Outside main-content
    val languageSwitcher = ".hmrc-language-select"
    val main = "#main-content"

    // Inside main-content
    val h1 = "h1"
    val h2 = "h2"
    val h3 = "h3"
    val p = "p"
    val label = "label"
    val visuallyHidden = ".govuk-visually-hidden"
    val link = ".govuk-link"
    val list = ".govuk-list"
    val inset = ".govuk-inset-text"
    val captionM = ".govuk-caption-m"
    val captionL = ".govuk-caption-l"
    val captionXL = ".govuk-caption-xl"
    val button = ".govuk-button"
    val li = "li"

    val errorSummary = ".govuk-error-summary"
    val errorSummaryList = ".govuk-error-summary__list"
    val fieldSet = ".govuk-fieldset"
    val fieldSetLegend = ".govuk-fieldset__legend"
    val fieldSetHint = ".govuk-fieldset > .govuk-hint"
    val hint = ".govuk-hint"
    val formGroup = ".govuk-form-group"
    val input = ".govuk-input"
    val select = ".govuk-select"

    val table = ".govuk-table"
    val tableCaption = ".govuk-table__caption"
    val tableHead = ".govuk-table__head"
    val tableBody = ".govuk-table__body"
    val tableRow = ".govuk-table__row"
    val tableRowHeader = ".govuk-table__header"
    val tableRowCell = ".govuk-table__cell"

    val summaryList = ".govuk-summary-list"
    val summaryListRow = ".govuk-summary-list__row"
    val summaryListRowKey = ".govuk-summary-list__key"
    val summaryListRowValue = ".govuk-summary-list__value"
    val summaryListRowActions = ".govuk-summary-list__actions"

    val notificationBannerTitle = ".govuk-notification-banner__title"
    val notificationBannerContent = ".govuk-notification-banner__content"

    val detailsSummary = ".govuk-details__summary-text"
    val detailsContent = ".govuk-details__text"

    val warning = ".govuk-warning-text"

    val tabLink = ".govuk-tabs__tab"
