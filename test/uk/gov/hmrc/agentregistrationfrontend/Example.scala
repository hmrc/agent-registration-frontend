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

package uk.gov.hmrc.agentregistrationfrontend

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonNaming
import play.api.libs.json.OFormat
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

sealed trait Example

final case class Example1(a: String)
extends Example

object Example1:
  given jsonFormat: OFormat[Example1] = Json.format[Example1]

final case class Example2(
  a: String,
  b: Int
)
extends Example

object Example2:
  given jsonFormat: OFormat[Example2] = Json.format[Example2]

case object Example3Whatever
extends Example

object Example:

  private given OFormat[Example3Whatever.type] = Json.format[Example3Whatever.type]

  given configuration: JsonConfiguration = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming { fullName =>
      fullName.split('.').last // Extract just the class name
    }
  )

  given jsonFormat: OFormat[Example] = Json.format[Example]

class ExampleSpec
extends UnitSpec:

  import uk.gov.hmrc.agentregistrationfrontend.testsupport.RichMatchers.*

  val e1: Example = Example1("sialala")
  val e1Json: JsValue = Json.toJson(e1)
  val e2: Example = Example2("sialala", 123)
  val e2Json: JsValue = Json.toJson(e2)
  val e3: Example = Example3Whatever
  val e3Json: JsValue = Json.toJson(e3)

  println(Json.toJson(e1))
  println(e1Json.as[Example])

  Json.toJson(e1Json.as[Example]) shouldBe e1Json
  e1Json.as[Example] shouldBe e1
  println()

  println(Json.toJson(e2))
  println(e2Json.as[Example])
  Json.toJson(e2Json.as[Example]) shouldBe e2Json
  e2Json.as[Example] shouldBe e2

  println()

  println(Json.toJson(e3))
  println(e3Json.as[Example])
  Json.toJson(e3Json.as[Example]) shouldBe e3Json
  e3Json.as[Example] shouldBe e3

  println()

//  println(Json.toJson(Example3))
