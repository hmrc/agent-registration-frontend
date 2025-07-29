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

package uk.gov.hmrc.agentregistrationfrontend.util

import play.api.libs.json.{Format, JsString, JsValue, Json}
import play.api.mvc.{PathBindable, QueryStringBindable}
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistrationfrontend.util.EnumExtensions.toStringHyphenated
import uk.gov.hmrc.govukfrontend.views.Aliases.{RadioItem, Text}
import uk.gov.hmrc.http.StringContextOps

import java.net.URL

  
enum ExampleEnum:

  case SoleTrader
  case LimitedCompany
  case GeneralPartnership
  case LimitedLiabilityPartnership

object ExampleEnum:
  given Format[ExampleEnum] = EnumFormat.enumFormat[ExampleEnum]
  given PathBindable[ExampleEnum] = EnumBinder.pathBindable[ExampleEnum]
  given QueryStringBindable[ExampleEnum] = EnumBinder.queryStringEnumBinder[ExampleEnum]

class EnumShowcase extends UnitSpec:

  "PathBindable" in {
    val pathBindable: PathBindable[ExampleEnum] = ExampleEnum.given_PathBindable_ExampleEnum
    val st: ExampleEnum = pathBindable.bind("businessType", "sole-trader").value
    st shouldBe ExampleEnum.SoleTrader
    pathBindable.unbind("businessType", ExampleEnum.SoleTrader) shouldBe "sole-trader"
  }

  "QueryStringBindable" in {
    val bindable: QueryStringBindable[ExampleEnum] = EnumBinder.queryStringEnumBinder[ExampleEnum]

    val st: ExampleEnum = bindable.bind(
      "businessType",
      Map("businessType" -> Seq("sole-trader"))
    )
      .value
      .value

    st shouldBe ExampleEnum.SoleTrader
    bindable.unbind("businessType", ExampleEnum.SoleTrader) shouldBe "businessType=sole-trader"

  }

  "toStringHyphenated" in {
    ExampleEnum.SoleTrader.toString shouldBe "SoleTrader"
    
    ExampleEnum.SoleTrader.toStringHyphenated shouldBe "sole-trader"

    //use cases
    //for example, in url param in connector code:
    val bt = ExampleEnum.SoleTrader
    val url: URL = url"http://whatever.mdtp/find-by-business-type/${bt.toStringHyphenated}?businessType=${bt.toStringHyphenated}"
    url.toString shouldBe "http://whatever.mdtp/find-by-business-type/sole-trader?businessType=sole-trader"

    //or as identifier in htmls ...

  }

  "JsoFormat" in {

    Json.toJson(ExampleEnum.SoleTrader) shouldBe Json.parse(""" "SoleTrader" """)
    //alternatively: Json.toJson(BusinessType2.SoleTrader) shouldBe Json.parse(""" "sole-trader" """)
  }
