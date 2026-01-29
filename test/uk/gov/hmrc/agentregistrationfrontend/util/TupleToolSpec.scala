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

  "TupleTool" should:

    "addByType" should:
      "add a new element to the tuple" in:
        val t = (1, "string")
        val result = t.addByType(true)
        result shouldBe (true, 1, "string")

      "fail to compile when adding a duplicate type" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string")
          t.addByType("duplicate")
        """)
        errors.map(_.message) shouldBe List("Type 'String' is already present in the tuple.")

    "getByType" should:
      "retrieve an existing element" in:
        val t: (Int, String, Boolean) = (1, "string", true)
        t.getByType[Int] shouldBe 1
        t.getByType[String] shouldBe "string"
        t.getByType[Boolean] shouldBe true

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.getByType[Double]
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "updateByType" should:
      "update an existing element" in:
        val t = (1, "string", true)
        val result = t.updateByType("new string")
        result shouldBe (1, "new string", true)

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.updateByType(2.0)
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "replaceByType" should:
      "replace an existing type with a new type" in:
        val t = (1, "string", true)
        val result = t.replaceByType[String, Double](2.0)
        result shouldBe (1, 2.0, true)

      "fail to compile when old type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.replaceByType[Double, Char]('c')
        """)
        errors.map(_.message) shouldBe List(
          """Type 'Double' is not present in the tuple.
            |Available types:
            |  * Int
            |  * String
            |  * Boolean""".stripMargin
        )

    "deleteByType" should:
      "remove an existing element" in:
        val t = (1, "string", true)
        val result = t.deleteByType[String]
        result shouldBe (1, true)

      "fail to compile when type is missing" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", true)
          t.deleteByType[Double]
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
        t.ensureUnique shouldBe t

      "fail to compile for a tuple with duplicate types" in:
        val errors = typeCheckErrors("""
          import uk.gov.hmrc.agentregistrationfrontend.util.TupleTool.*
          val t = (1, "string", 1)
          t.ensureUnique
        """)
        errors.map(_.message) shouldBe List("Type 'Int' is already present in the tuple.")
