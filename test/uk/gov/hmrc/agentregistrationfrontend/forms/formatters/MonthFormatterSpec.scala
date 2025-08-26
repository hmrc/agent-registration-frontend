/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.forms.formatters

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.FormError

class MonthFormatterSpec
extends AnyWordSpec
with Matchers {

  val invalidKey = "dateFieldName.error.invalid"
  val missingKey = "dateFieldName.error.required"

  val monthKey: String = "date.month"

  val monthNames = Map[Int, String](
    1 -> "January",
    2 -> "February",
    3 -> "March",
    4 -> "April",
    5 -> "May",
    6 -> "June",
    7 -> "July",
    8 -> "August",
    9 -> "September",
    10 -> "October",
    11 -> "November",
    12 -> "December"
  )

  val formatter =
    new MonthFormatter(
      invalidKey = invalidKey,
      missingKey = missingKey
    )

  ".bind" should {

    monthNames.foreach { case (monthNumber, monthFullName) =>
      val monthShortName = monthFullName.take(3)
      s"return an Int when binding was successful from a valid month number with leading zero: 0$monthNumber" in {
        formatter.bind(
          key = monthKey,
          data = Map(monthKey -> s"0$monthNumber")
        ) shouldBe
          Right(monthNumber)
      }
      s"return an Int when binding was successful from a valid month number without leading zero: $monthNumber" in {
        formatter.bind(
          key = monthKey,
          data = Map(monthKey -> s"$monthNumber")
        ) shouldBe
          Right(monthNumber)
      }
      s"return an Int when binding was successful from a valid month short name: $monthShortName" in {
        formatter.bind(
          key = monthKey,
          data = Map(monthKey -> monthShortName)
        ) shouldBe
          Right(monthNumber)
      }
      s"return an Int when binding was successful from a valid month full name: $monthFullName" in {
        formatter.bind(
          key = monthKey,
          data = Map(monthKey -> monthFullName)
        ) shouldBe
          Right(monthNumber)
      }
    }

    "return the form errors when binding was unsuccessful due to missing value" in {
      formatter.bind(
        key = monthKey,
        data = Map(monthKey -> "")
      ) shouldBe
        Left(Seq(FormError(
          key = "date.month",
          message = missingKey
        )))
    }

    "return the form errors when binding was unsuccessful due to invalid input" in {
      formatter.bind(
        key = monthKey,
        data = Map(monthKey -> "invalid")
      ) shouldBe
        Left(Seq(FormError(
          key = "date.month",
          message = invalidKey
        )))
    }
  }

  ".unbind" should {

    "return form data from a valid month Int" in {
      formatter.unbind(
        key = monthKey,
        value = 11
      ) shouldBe Map(
        "date.month" -> "11"
      )
    }
  }

}
