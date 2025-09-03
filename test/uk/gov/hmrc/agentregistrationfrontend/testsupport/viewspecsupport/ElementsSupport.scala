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
import org.jsoup.select.Elements
import org.scalactic.source.Position
import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers.*

import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.language.implicitConversions
import JsoupHelper.*

object ElementsSupport:

  extension (elements: Elements)

    inline def selectOrFail(selector: String)(using pos: Position): Elements =
      val selectedElements: Elements = elements.select(selector)
      if (selectedElements.isEmpty)
        fail(s"selector '$selector' didn't yield any results $hints")
      else
        selectedElements

    /** Will find the element by index. Index lookup is 1-indexed, if you want the first element then pass in 1.
      */
    inline def selectOrFail(index: Int)(using pos: Position): Element = elements
      .toList
      .lift(index - 1)
      .getOrElse(fail(s"Index $index is out of bounds (size: ${elements.size()}) $hints"))

    inline def selectOnlyOneElementOrFail()(using pos: Position): Element =
      if elements.size() != 1 then
        fail(s"Expected elements to contain only one element (size: ${elements.size()}) $hints")
      else elements.first()

    def toList: List[Element] = elements.iterator.asScala.toList
    def headOption: Option[Element] = toList.headOption

    private def elementsHint = s"""\n\nElements content was:\n${elements.toString}\n"""
    private def hints(using pos: Position): String = s"$positionInfo$elementsHint"
