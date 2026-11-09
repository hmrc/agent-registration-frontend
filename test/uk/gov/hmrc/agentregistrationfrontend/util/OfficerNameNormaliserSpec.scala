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
import uk.gov.hmrc.agentregistrationfrontend.util.OfficerNameNormaliser.NameParts

class OfficerNameNormaliserSpec
extends UnitSpec:

  "normalise" should:
    List(
      NormaliseTestCase(
        apiFormat = "O'CONNOR, Anne-Marie",
        titleCase = "Anne-Marie O'Connor",
        description = "move the surname after the forenames and title case both"
      ),
      NormaliseTestCase(
        apiFormat = "ANNE-MARIE O'CONNOR",
        titleCase = "Anne-Marie O'Connor",
        description = "title case a name that is not surname first"
      ),
      NormaliseTestCase(
        apiFormat = "  ANNE-MARIE   O'CONNOR  ",
        titleCase = "Anne-Marie O'Connor",
        description = "trim the name and collapse the space inside it"
      ),
      NormaliseTestCase(
        apiFormat = "O'CONNOR,",
        titleCase = "O'Connor,",
        description = "title case a surname whose forenames are missing"
      )
    ).foreach: tc =>
      tc.description in:
        OfficerNameNormaliser.normalise(tc.apiFormat) shouldBe tc.titleCase

  "splitNameParts" should:
    List(
      SplitTestCase(
        apiFormat = "O'CONNOR, Anne-Marie",
        nameParts = Some(NameParts(surname = "O'CONNOR", forenames = "Anne-Marie")),
        description = "split a surname from its forenames"
      ),
      SplitTestCase(
        apiFormat = "LØVRING, Mathias Brask",
        nameParts = Some(NameParts(surname = "LØVRING", forenames = "Mathias Brask")),
        description = "keep several forenames together"
      ),
      SplitTestCase(
        apiFormat = "O'CONNOR, Anne-Marie, Mrs",
        nameParts = Some(NameParts(surname = "O'CONNOR", forenames = "Anne-Marie")),
        description = "ignore the title"
      ),
      SplitTestCase(
        apiFormat = "  O'CONNOR ,  Anne-Marie  ",
        nameParts = Some(NameParts(surname = "O'CONNOR", forenames = "Anne-Marie")),
        description = "trim each of its parts"
      ),
      SplitTestCase(
        apiFormat = "ANNE-MARIE O'CONNOR",
        nameParts = None,
        description = "find no parts in a name that is not surname first"
      ),
      SplitTestCase(
        apiFormat = "O'CONNOR,",
        nameParts = None,
        description = "find no parts when the forenames are missing"
      ),
      SplitTestCase(
        apiFormat = "",
        nameParts = None,
        description = "find no parts in an empty name"
      )
    ).foreach: tc =>
      tc.description in:
        OfficerNameNormaliser.splitNameParts(tc.apiFormat) shouldBe tc.nameParts

  "capitaliseNameParts" should:
    List(
      TitleCaseTestCase(
        anyCase = "ANNE-MARIE",
        titleCase = "Anne-Marie",
        description = "capitalise the letter after a hyphen"
      ),
      TitleCaseTestCase(
        anyCase = "LØVRING",
        titleCase = "Løvring",
        description = "capitalise a non-ASCII letter"
      ),
      TitleCaseTestCase(
        anyCase = "O'CONNOR",
        titleCase = "O'Connor",
        description = "capitalise the letter after a straight apostrophe"
      ),
      TitleCaseTestCase(
        anyCase = "O’NEILL",
        titleCase = "O’Neill",
        description = "capitalise the letter after a smart quote"
      ),
      TitleCaseTestCase(
        anyCase = "VAN DER WAALS",
        titleCase = "Van Der Waals",
        description = "capitalise every word"
      ),
      TitleCaseTestCase(
        anyCase = "ST.JOHN",
        titleCase = "St.John",
        description = "capitalise the letter after a period"
      ),
      TitleCaseTestCase(
        anyCase = "mathias brask",
        titleCase = "Mathias Brask",
        description = "capitalise a name that arrives in lower case"
      ),
      TitleCaseTestCase(
        anyCase = "",
        titleCase = "",
        description = "leave an empty name empty"
      )
    ).foreach: tc =>
      tc.description in:
        OfficerNameNormaliser.capitaliseNameParts(tc.anyCase) shouldBe tc.titleCase

  private case class NormaliseTestCase(
    apiFormat: String,
    titleCase: String,
    description: String
  )

  private case class SplitTestCase(
    apiFormat: String,
    nameParts: Option[NameParts],
    description: String
  )

  private case class TitleCaseTestCase(
    anyCase: String,
    titleCase: String,
    description: String
  )
