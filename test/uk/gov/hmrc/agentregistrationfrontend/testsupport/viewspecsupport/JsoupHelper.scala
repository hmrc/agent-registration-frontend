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

import org.scalactic.source.Position
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

object JsoupHelper:

  def positionInfo(using pos: Position): String = s"at (${pos.fileName}:${pos.lineNumber})"

  extension (s: String)
    /** Transforms string so it's easier to inspect visually
      */
    def stripSpaces: String = s
      .replaceAll("[^\\S\\r\\n]+", " ") // replace many consecutive white-spaces (but not new lines) with one space
      .replaceAll("[\r\n]+", "\n") // replace many consecutive new lines with one new line
      .split("\n").map(_.trim) // trim each line
      .filterNot(_ === "") // remove any empty lines
      .mkString("\n")
