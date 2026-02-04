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

package uk.gov.hmrc.agentregistration.shared.util

import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class SafeEqualsSpec
extends UnitSpec:

  "SafeEquals" should:
    "return true when values are equal" in:
      (1 === 1) shouldBe true
      ("test" === "test") shouldBe true
      (Option(1) === Some(1)) shouldBe true

    "return false when values are not equal" in:
      (1 === 2) shouldBe false
      ("test" === "other") shouldBe false
      (Option(1) === None) shouldBe false

    "return true when values are not equal using =!=" in:
      (1 =!= 2) shouldBe true

    "return false when values are equal using =!=" in:
      (1 =!= 1) shouldBe false

    "compile when types are subtypes of each other" in:
      val a: Option[Int] = Some(1)
      val b: Some[Int] = Some(1)
      (a === b) shouldBe true
      (b === a) shouldBe true

      trait S
      final case class A(i: Int)
      extends S
      final case class B(i: Int)
      extends S

      val a1: A = A(1)
      val b1: B = B(1)
      val sa1: S = A(1)
      val sb1: S = B(1)

      sa1 === a1 shouldBe true
      a1 === sa1 shouldBe true
      a1 =!= sb1 shouldBe true
      b1 =!= sa1 shouldBe true
      b1 === sb1 shouldBe true
      sa1 =!= sb1 shouldBe true

    "not compile when types are unrelated" in:
      assertTypeError("""
        import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
        1 === "1"
      """)

      assertTypeError("""
        import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
        val f: Int => Int = _ + 1
        val o: Option[Int] = Some(1)
        f === o
      """)

      assertTypeError("""
        import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
        case class A(i: Int)
        case class B(i: Int)
        A(1) === B(1)
      """)

      assertTypeError("""
        import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.*
        sealed trait S
        case class A(i: Int) extends S
        case class B(i: Int) extends S
        A(1) === B(1)
      """)
