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

import java.time.LocalDate

class LocalDateFormatterSpec
extends AnyWordSpec
with Matchers {

  val msgPrefix = "page.service"

  val formatter = new LocalDateFormatter(msgPrefix)

  ".validateDayMonthYear" should {

    val emptyMonth = Left(Seq(FormError("date.month", formatter.monthRequiredKey)))

    "return no form errors when all date values are valid" in {
      formatter.validateDayMonthYear(
        key = "date",
        day = Some("11"),
        month = Right(11),
        year = Some("2000")
      ) shouldBe Nil
    }

    "return form errors" when {

      "the day is invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("f"),
          month = Right(11),
          year = Some("2000")
        ) shouldBe
          Seq(FormError("date.day", formatter.invalidKey))
      }

      "the month is invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = Right(88),
          year = Some("2000")
        ) shouldBe
          Seq(FormError("date.month", formatter.invalidKey))
      }

      "the year is invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = Right(11),
          year = Some("f")
        ) shouldBe
          Seq(FormError("date.year", formatter.invalidKey))
      }

      "the month and year are invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = Right(20),
          year = Some("0")
        ) shouldBe Seq(
          FormError("date.month", formatter.invalidKey),
          FormError("date.year", formatter.invalidKey)
        )
      }

      "the day and year are invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("50"),
          month = Right(11),
          year = Some("0")
        ) shouldBe Seq(
          FormError("date.day", formatter.invalidKey),
          FormError("date.year", formatter.invalidKey)
        )
      }

      "the day and month are invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("5.6"),
          month = Right(99),
          year = Some("2000")
        ) shouldBe Seq(
          FormError("date.day", formatter.invalidKey),
          FormError("date.month", formatter.invalidKey)
        )
      }

      "all three fields are invalid" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("&%£"),
          month = Right(99),
          year = Some("-_-")
        ) shouldBe
          Seq(FormError("date", formatter.invalidKey))
      }

      "the day is empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = None,
          month = Right(11),
          year = Some("2000")
        ) shouldBe
          Seq(FormError("date.day", formatter.dayRequiredKey))
      }

      "the month is empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = emptyMonth,
          year = Some("2000")
        ) shouldBe
          Seq(FormError("date.month", formatter.monthRequiredKey))
      }

      "the year is empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = Right(11),
          year = None
        ) shouldBe
          Seq(FormError("date.year", formatter.yearRequiredKey))
      }

      "the month and year are empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = Some("11"),
          month = emptyMonth,
          year = None
        ) shouldBe Seq(
          FormError("date.month", formatter.monthYearRequiredKey),
          FormError("date.year", formatter.monthYearRequiredKey)
        )
      }

      "the day and year are empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = None,
          month = Right(11),
          year = None
        ) shouldBe Seq(
          FormError("date.day", formatter.dayYearRequiredKey),
          FormError("date.year", formatter.dayYearRequiredKey)
        )
      }

      "the day and month are empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = None,
          month = emptyMonth,
          year = Some("2000")
        ) shouldBe Seq(
          FormError("date.day", formatter.dayMonthRequiredKey),
          FormError("date.month", formatter.dayMonthRequiredKey)
        )
      }

      "all three fields are empty" in {
        formatter.validateDayMonthYear(
          key = "date",
          day = None,
          month = emptyMonth,
          year = None
        ) shouldBe
          Seq(FormError("date", formatter.allRequiredKey))
      }
    }
  }

  ".toDate" should {

    "return a LocalDate when the provided values make a valid date" in {
      formatter.toDate(
        key = "date",
        day = 11,
        month = 11,
        year = 2000
      ) shouldBe Right(LocalDate.parse("2000-11-11"))
    }

    "return a form error when the provided values do not make a valid date" in {
      formatter.toDate(
        key = "date",
        day = 31,
        month = 11,
        year = 2000
      ) shouldBe Left(Seq(FormError("date", formatter.invalidKey)))
    }
  }

  ".bind" should {

    "return a LocalDate when binding was successful" in {
      formatter.bind(
        "date",
        Map(
          "date.day" -> "11",
          "date.month" -> "11",
          "date.year" -> "2000"
        )
      ) shouldBe
        Right(LocalDate.parse("2000-11-11"))
    }

    "return the form errors when binding was unsuccessful" in {
      formatter.bind(
        key = "date",
        data = Map(
          "date.day" -> "fff",
          "date.month" -> "79",
          "date.year" -> "3.142"
        )
      ) shouldBe
        Left(Seq(FormError("date", formatter.invalidKey)))
    }
  }

  ".unbind" should {

    "return form data from a LocalDate" in {
      formatter.unbind(
        key = "date",
        value = LocalDate.parse("2000-11-11")
      ) shouldBe Map(
        "date.day" -> "11",
        "date.month" -> "11",
        "date.year" -> "2000"
      )
    }
  }

}
