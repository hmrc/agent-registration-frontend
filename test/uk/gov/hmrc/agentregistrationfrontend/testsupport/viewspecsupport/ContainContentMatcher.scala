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
import org.scalatest.matchers.MatchResult
import org.scalatest.matchers.Matcher
import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.viewspecsupport.JsoupHelper.stripSpaces

class ContainContentMatcher(expectedLines: String)
extends Matcher[Element]:

  def apply(element: Element): MatchResult =
    val actualText: String = element.wholeText().stripSpaces
    val cleanExpectedText: String = expectedLines.stripSpaces
    val includeResult = include(cleanExpectedText)(actualText)

    // Hacking do provide '<Click to see difference>' support in intellij when the test fails.
    // Delegating computing MatchResult to
    val result: MatchResult = be(cleanExpectedText)(actualText)

    result.copy(
      matches = includeResult.matches,
      // Don't change below code!
      // It must containt 'was not equal to {1}' or similar strings in the messages
      // in order to support intellij's <Click to see difference> support
      rawFailureMessage = s"{0}\n\ndid not contain or was not equal to {1}",
      rawNegatedFailureMessage = s"{0}\n\ndid contain or was equal to {1}",
      rawMidSentenceFailureMessage = s"{0}\n\ndid not contain or was not equal to {1}",
      rawMidSentenceNegatedFailureMessage = s"{0}\n\ndid contain or was equal to {1}"
    )

object ContainContentMatcher:

  def containContent(expectedLines: String)(using pos: Position): ContainContentMatcher = new ContainContentMatcher(expectedLines)

  extension (element: Element)
    infix def shouldContainContent(expectedLines: String)(using pos: Position) = element should containContent(expectedLines)
