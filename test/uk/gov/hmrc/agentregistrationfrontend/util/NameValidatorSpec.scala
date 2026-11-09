/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.util

import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class NameValidatorSpec
extends UnitSpec:

  private def char(codePoint: Int): String = codePoint.toChar.toString

  "isValid" should:
    List(
      TestCase(
        name = "Taylor Leadenhall-Lane",
        isValid = true,
        description = "accept a hyphen"
      ),
      TestCase(
        name = "Anne-Marie O'Connor",
        isValid = true,
        description = "accept a straight apostrophe"
      ),
      TestCase(
        name = "Siobhán O’Neill",
        isValid = true,
        description = "accept a precomposed accent and a smart quote"
      ),
      TestCase(
        name = s"Siobha${char(0x0301)}n",
        isValid = true,
        description = "accept an accent that arrives as a combining mark"
      ),
      TestCase(
        name = "Mathias Brask Løvring",
        isValid = true,
        description = "accept a letter outside ASCII"
      ),
      TestCase(
        name = "Dr. John A. Smith",
        isValid = true,
        description = "accept periods"
      ),
      TestCase(
        name = "李小龙",
        isValid = true,
        description = "accept letters in any script"
      ),
      TestCase(
        name = "WilL14m Wilson",
        isValid = false,
        description = "reject a digit"
      ),
      TestCase(
        name = "John Doe!",
        isValid = false,
        description = "reject punctuation that is not part of a name"
      ),
      TestCase(
        name = "Harper Robinson|",
        isValid = false,
        description = "reject the pipe the risking file is delimited by"
      ),
      TestCase(
        name = "Victoria Young,",
        isValid = false,
        description = "reject a comma"
      ),
      TestCase(
        name = "Jane Doe@",
        isValid = false,
        description = "reject an at sign"
      ),
      TestCase(
        name = "Ethan Clark\\",
        isValid = false,
        description = "reject a backslash"
      ),
      TestCase(
        name = "Scarlett Hall<",
        isValid = false,
        description = "reject an angle bracket"
      ),
      TestCase(
        name = s"John${char(0x00a0)}Doe",
        isValid = false,
        description = "reject a non-breaking space"
      ),
      TestCase(
        name = s"John${char(0x200b)}Doe",
        isValid = false,
        description = "reject a zero-width space"
      ),
      TestCase(
        name = s"John${char(0x0008)}Doe",
        isValid = false,
        description = "reject a control character"
      ),
      TestCase(
        name = "",
        isValid = false,
        description = "reject an empty name"
      ),
      TestCase(
        name = "John\tDoe",
        isValid = true,
        description = "accept a tab, because the pattern allows any whitespace"
      ),
      TestCase(
        name = "John\nDoe",
        isValid = true,
        description = "accept a line break, because the pattern allows any whitespace"
      )
    ).foreach: tc =>
      tc.description in:
        NameValidator.isValid(tc.name) shouldBe tc.isValid

  private case class TestCase(
    name: String,
    isValid: Boolean,
    description: String
  )
