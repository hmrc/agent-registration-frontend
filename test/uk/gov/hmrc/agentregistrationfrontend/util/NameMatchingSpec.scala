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

import uk.gov.hmrc.agentregistration.shared.lists.IndividualName
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class NameMatchingSpec extends UnitSpec:

  private val officerList = Seq(
    IndividualName("Steve Austin"),
    IndividualName("Pauline Austin"),
    IndividualName("Beverly Hills"),
    IndividualName("Justine Hills"),
    IndividualName("Sandra Hills"),
    IndividualName("Steve Palmer")
  )

  "individualNameMatching" should:

    "match when the name is exactly the same" in:
      NameMatching.individualNameMatching(IndividualName("Steve Austin"), officerList) shouldBe Some(IndividualName("Steve Austin"))

    "match case-insensitively" in:
      NameMatching.individualNameMatching(IndividualName("steve austin"), officerList) shouldBe Some(IndividualName("Steve Austin"))

    "match with mixed case" in:
      NameMatching.individualNameMatching(IndividualName("PAULINE AUSTIN"), officerList) shouldBe Some(IndividualName("Pauline Austin"))

    "not match when only surname matches" in:
      NameMatching.individualNameMatching(IndividualName("Dave Austin"), officerList) shouldBe None

    "not match when only first name matches" in:
      NameMatching.individualNameMatching(IndividualName("Steve Hills"), officerList) shouldBe None

    "not match when name is not in the list" in:
      NameMatching.individualNameMatching(IndividualName("Unknown Person"), officerList) shouldBe None

    "not match when list is empty" in:
      NameMatching.individualNameMatching(IndividualName("Steve Austin"), Seq.empty) shouldBe None

    "match the correct officer when multiple officers share a surname" in:
      NameMatching.individualNameMatching(IndividualName("Sandra Hills"), officerList) shouldBe Some(IndividualName("Sandra Hills"))

    "match the correct officer when multiple officers share a first name" in:
      NameMatching.individualNameMatching(IndividualName("Steve Palmer"), officerList) shouldBe Some(IndividualName("Steve Palmer"))

    "not match a partial name" in:
      NameMatching.individualNameMatching(IndividualName("Steve"), officerList) shouldBe None

    "not match with swapped first and last name" in:
      NameMatching.individualNameMatching(IndividualName("Austin Steve"), officerList) shouldBe None

    "not match with extra spaces" in:
      NameMatching.individualNameMatching(IndividualName("Steve  Austin"), officerList) shouldBe None
