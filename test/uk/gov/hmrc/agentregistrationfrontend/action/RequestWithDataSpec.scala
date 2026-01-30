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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results
import play.api.mvc.Results.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class RequestWithDataSpec
extends UnitSpec:

  type RequestX[A] = RequestWithData[
    A,
    (
      String,
      Int,
      Option[AgentApplication],
      (Int, Float)
    )
  ]

  type IsExotic = (Orange | Banana.type) & Fruit
  type IsNotExotic = (Apple | Pear) & Fruit

  sealed trait Fruit

  final case class Apple(smell: Int)
  extends Fruit
  final case class Pear(smell: Int)
  extends Fruit
  final case class Orange(smell: Int)
  extends Fruit
  case object Banana
  extends Fruit

  "showcase" in:
    val r: RequestX[AnyContentAsEmpty.type] = RequestWithData(
      request = FakeRequest(),
      data = ("foo", 42, None, (11, 3.14f))
    )

    r.get[String] shouldBe "foo"
    r.get[Int] shouldBe 42
//    r.get[Float] won't compile
    r.get[Option[AgentApplication]] shouldBe None
    // r.add(Results.BadRequest).add(Results.NotFound)
    val r2 = r.add(Results.BadRequest)
    r2.get[Status] shouldBe Results.BadRequest
    r2.update(Results.NotFound).get[Status] shouldBe Results.NotFound
    val r3: RequestWithData[AnyContentAsEmpty.type, (Fruit, Status, String, Int, Option[AgentApplication], (Int, Float))] = r2.add(Banana)

    r3.get[Fruit] shouldBe Banana
    r3.update[Fruit](Apple(1)).get[Fruit] shouldBe Apple(1)

    val r4: RequestWithData[
      AnyContentAsEmpty.type,
      (
        Fruit,
        Status,
        String,
        Int,
        Option[AgentApplication],
        (Int, Float)
      )
    ] = r3.update[Fruit](Apple(1))

    val r5: RequestWithData[
      AnyContentAsEmpty.type,
      (
        IsNotExotic,
        Status,
        String,
        Int,
        Option[AgentApplication],
        (Int, Float)
      )
    ] = r4.replace[Fruit, IsNotExotic](Apple(2))

    val r6: RequestWithData[
      AnyContentAsEmpty.type,
      (
        IsNotExotic,
        Status,
        String,
        Int,
        Option[AgentApplication]
      )
    ] = r5.delete[(Int, Float)]

    println(r6)
