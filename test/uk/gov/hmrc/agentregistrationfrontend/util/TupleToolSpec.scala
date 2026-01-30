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
import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*

import scala.compiletime.testing.typeCheckErrors

class TupleToolSpec
extends UnitSpec:

  trait Animal
  case class Fish(name: String)
  extends Animal
  case class Chicken(name: String)
  extends Animal
  case class Frog(name: String)
  extends Animal
  type CanSwim = Animal & (Fish | Frog)

  "showcase" in:
    //    EmptyTuple.get[Boolean]
    val kermit: Frog = Frog("Kermit")
    val t = (1, "string", true, kermit)
    t.get[Int] shouldBe 1
    t.get[String] shouldBe "string"
    t.get[Boolean] shouldBe true
    //    t.get[Any]
    //    t.get[Animal] shouldBe Frog("kermit")
    t.get[Frog] shouldBe kermit
    val t2: (Int, Int) = (1, 2)
    t2.add(2.0)

//    case class UniqueTuple[Data <: Tuple](t: Data)(using HasDuplicates[Data] =:= false)

//    (1, 2).ensureUniqueTypes
//    (1, 2).update(123) shouldBe (123, 2)
    (1, 2).replace[Int, Long](123) shouldBe (123, 2)

  "TupleTool" should:

    "addByType" should:
      "add a new element to the tuple" in:
        val t = (1, "string")
        val result = t.add(true)
        result shouldBe (true, 1, "string")

      "fail to compile when adding a duplicate type" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string")
          t.add("duplicate")
        """)
        errors.map(_.message) shouldBe List(
          """Type 'String' is already present in the tuple.
            |Available types:
            |  * Int
            |  * String""".stripMargin
        )

    "get" should:
      "retrieve an existing element" in:
        val t: (Int, String, Boolean) = (1, "string", true)
        t.get[Int] shouldBe 1
        t.get[String] shouldBe "string"
        t.get[Boolean] shouldBe true

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.get[Double]
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "update" should:
      "update an existing element" in:
        val t = (1, "string", true)
        val result = t.update("new string")
        result shouldBe (1, "new string", true)

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.update(2.0)
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "replace" should:
      "replace an existing type with a new type" in:
        val t = (1, "string", true)
        val result = t.replace[String, Double](2.0)
        result shouldBe (1, 2.0, true)

      "fail to compile when old type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.replace[Double, Char]('c')
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "delete" should:
      "remove an existing element" in:
        val t = (1, "string", true)
        val result = t.delete[String]
        result shouldBe (1, true)

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.delete[Double]
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "ensureUnique" should:
      "pass for a tuple with unique types" in:
        val t = (1, "string", true)
        t.ensureUniqueTypes shouldBe t

      "fail to compile for a tuple with duplicate types" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", 1)
          t.ensureUnique
        """)
        errors.map(_.message) shouldBe List(
          """Tuple isn't unique. Type 'Int' occurs more more then once:
            |  * Int
            |  * String
            |  * Int""".stripMargin
        )
