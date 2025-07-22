/*
 * Copyright 2024 HM Revenue & Customs
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

import org.jsoup.nodes.Element
import org.jsoup.select.Elements

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.language.implicitConversions

trait ViewSpecHelper extends Selectors {
  extension (elements: Elements)
    def toList: List[Element] = elements.iterator.asScala.toList
    def headOption: Option[Element] = toList.headOption


  case class TestLink(text: String, href: String)

  case class TestRadioGroup(legend: String, options: List[(String, String)], hint: Option[String], optionHints: List[(String, Option[String])] = Nil)

  case class TestInputField(label: String, hint: Option[String], inputName: String)

  case class TestSelect(inputName: String, options: Seq[(String, String)])

  case class TestSummaryList(rows: List[(String, String, String)])

  case class TestTable(caption: String, rows: List[IndexedSeq[String]])

  private val elementToLink: Element => TestLink = element => TestLink(element.text(), element.attr("href"))

  extension (el: Element)
    def hasLanguageSwitch: Boolean = el.select(languageSwitcher).headOption.nonEmpty
    def mainContent: Element = el.select(main).headOption.get

    //  Will find the element by id or index and return the element as option
    //  Index lookup is 1-indexed, if you want the first element then pass in 1
    //  Can be chained to extract elements inside other elements without keeping track of indeces in the whole view like:
    //    val para = extractByIndex(para, 1)
    //    para.extractText(visuallyHidden, 1)
    //    para.extractLink(1)
    def extractByIndex(selector: String, index: Int): Option[Element] = el.select(selector).toList.lift(index - 1)
    def extractById(selector: String, id: String): Option[Element] = el.select(selector).select(s"#$id").headOption

    def extractText(selector: String, index: Int): Option[String] = extractByIndex(selector, index).map(_.text())
    def extractText(selector: String, id: String): Option[String] = extractById(selector, id).map(_.text())

    def extractList(index: Int): List[String] = extractByIndex(list, index).map(_.getElementsByTag("li").toList.map(_.text())).getOrElse(Nil)

    def extractLink(index: Int): Option[TestLink] = extractByIndex(link, index).map(elementToLink)
    def extractLink(id: String): Option[TestLink] = extractById(link, id).map(elementToLink)

    def extractLinkButton(index: Int): Option[TestLink] = extractByIndex(button, index).map(elementToLink)
    def extractLinkButton(id: String): Option[TestLink] = extractById(button, id).map(elementToLink)

    def extractRadios(index: Int = 1): Option[TestRadioGroup] = extractByIndex(fieldSet, index).map { element =>
      TestRadioGroup(
        legend = element.select(fieldSetLegend).first().text(),
        options = element.select(".govuk-radios__item").toList.map(el => (el.select("label").text(), el.select("input").attr("value"))),
        hint = element.select(fieldSetHint).toList.headOption.map(_.text)
      )
    }

    def extractRadiosWithHints(index: Int = 1): Option[TestRadioGroup] = extractByIndex(fieldSet, index).map { element =>
      TestRadioGroup(
        legend = element.select(fieldSetLegend).first().text(),
        options = element.select(".govuk-radios__item").toList.map(el => (el.select("label").text(), el.select("input").attr("value"))),
        hint = element.select(fieldSetHint).toList.headOption.map(_.text),
        optionHints = element.select(".govuk-radios__item").toList.map(el => (el.select("label").text(), el.select(".govuk-radios__hint").headOption.map(_.text()))),
      )
    }

    def extractInputField(index: Int = 1): Option[TestInputField] = extractByIndex(formGroup, index).map { elem =>
      TestInputField(
        label = elem.select(label).first().text(),
        hint = elem.select(hint).toList.headOption.map(_.text),
        inputName = elem.select(input).attr("name")
      )
    }

    def extractSelectElement(index: Int = 1): Option[TestSelect] = extractByIndex(select, index).map { elem =>
      TestSelect(
        inputName = elem.select(select).attr("name"),
        options = elem.select("option").toList.map(el => (el.attr("value"), el.text()))
      )
    }

    def extractSummaryList(index: Int = 1): Option[TestSummaryList] = extractByIndex(summaryList, index).map { elem =>
      TestSummaryList(
        rows = elem.select(summaryListRow).toList.map { row =>
          val key = row.select(summaryListRowKey).text()
          val value = row.select(summaryListRowValue).text()
          val changeLink = row.select(s"$summaryListRowActions > a").attr("href")
          (key, value, changeLink)
        }
      )
    }

    def extractTable(index: Int, numberOfCols: Int): Option[TestTable] = extractByIndex(table, index).map { elem =>
      TestTable(
        caption = elem.select("caption").text(),
        rows = elem.select("tbody tr").toList.map { row =>
          for (i <- 0 until numberOfCols) yield row.select("td").get(i).text()
        }
      )
    }
}
