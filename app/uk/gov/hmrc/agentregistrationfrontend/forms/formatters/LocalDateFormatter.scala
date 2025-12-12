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

import play.api.data.FormError
import play.api.data.format.Formatter
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.LocalDate
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class LocalDateFormatter(messagePrefix: String)
extends Formatter[LocalDate] {

  val invalidKey = s"$messagePrefix.error.invalid"
  val allRequiredKey = s"$messagePrefix.error.required"
  val dayRequiredKey = s"$messagePrefix.error.day.required"
  val monthRequiredKey = s"$messagePrefix.error.month.required"
  val yearRequiredKey = s"$messagePrefix.error.year.required"
  val dayMonthRequiredKey = s"$messagePrefix.error.day-month.required"
  val dayYearRequiredKey = s"$messagePrefix.error.day-year.required"
  val monthYearRequiredKey = s"$messagePrefix.error.month-year.required"

  def toDate(
    key: String,
    day: Int,
    month: Int,
    year: Int
  ): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(
      year,
      month,
      day
    )) match {
      case Success(date) => Right(date)
      case Failure(_) => Left(Seq(FormError(key, invalidKey)))
    }

  def validateDayMonthYear(
    key: String,
    day: Option[String],
    month: Either[Seq[FormError], Int],
    year: Option[String]
  ): Seq[FormError] = {

    def validateDay: Boolean = Try(day.get.toInt).toOption.exists(value => 1 to 31 contains value)
    def validateMonth(value: Int): Boolean = 1 to 12 contains value
    def validateYear: Boolean = Try(year.get.toInt).toOption.exists(value => value > 0)

    def emptyMonthResult = Seq(FormError(s"$key.month", monthRequiredKey))

    (day, month, year) match {
      case (Some(_), Right(monthInt), Some(_)) =>
        (validateDay, validateMonth(monthInt), validateYear) match {
          case (true, true, true) => Nil
          case (false, true, true) => Seq(FormError(s"$key.day", invalidKey))
          case (true, false, true) => Seq(FormError(s"$key.month", invalidKey))
          case (true, true, false) => Seq(FormError(s"$key.year", invalidKey))
          case (true, false, false) =>
            Seq(
              FormError(s"$key.month", invalidKey),
              FormError(s"$key.year", invalidKey)
            )
          case (false, true, false) =>
            Seq(
              FormError(s"$key.day", invalidKey),
              FormError(s"$key.year", invalidKey)
            )
          case (false, false, true) =>
            Seq(
              FormError(s"$key.day", invalidKey),
              FormError(s"$key.month", invalidKey)
            )
          case (false, false, false) => Seq(FormError(key, invalidKey))
        }
      case (None, Right(_), Some(_)) => Seq(FormError(s"$key.day", dayRequiredKey))
      case (Some(_), Right(_), None) => Seq(FormError(s"$key.year", yearRequiredKey))
      case (Some(_), Left(monthError), None) if monthError === emptyMonthResult =>
        Seq(FormError(s"$key.month", monthYearRequiredKey)) ++ Seq(FormError(s"$key.year", monthYearRequiredKey))
      case (Some(_), Left(monthError), None) => monthError ++ Seq(FormError(s"$key.year", yearRequiredKey))
      case (None, Left(monthError), Some(_)) if monthError === emptyMonthResult =>
        Seq(FormError(s"$key.day", dayMonthRequiredKey)) ++ Seq(FormError(s"$key.month", dayMonthRequiredKey))
      case (None, Left(monthError), Some(_)) => Seq(FormError(s"$key.day", dayRequiredKey)) ++ monthError
      case (Some(_), Left(monthError), Some(_)) if monthError === emptyMonthResult => monthError
      case (Some(_), Left(monthError), Some(_)) =>
        (validateDay, validateYear) match {
          case (true, true) => monthError
          case (false, true) => Seq(FormError(s"$key.day", invalidKey)) ++ monthError
          case (true, false) => monthError ++ Seq(FormError(s"$key.year", invalidKey))
          case (false, false) => Seq(FormError(key, invalidKey)) // all three fields are invalid
        }
      case (None, Right(_), None) =>
        Seq(
          FormError(s"$key.day", dayYearRequiredKey),
          FormError(s"$key.year", dayYearRequiredKey)
        )
      case _ => Seq(FormError(key, allRequiredKey))
    }
  }

  override def bind(
    key: String,
    data: Map[String, String]
  ): Either[Seq[FormError], LocalDate] = {

    val dayKey: String = s"$key.day"
    val monthKey: String = s"$key.month"
    val yearKey: String = s"$key.year"

    val dayValue: Option[String] = data.get(dayKey).filter(_.nonEmpty)
    val monthValue: Option[String] = data.get(monthKey).filter(_.nonEmpty)
    val yearValue: Option[String] = data.get(yearKey).filter(_.nonEmpty)

    val monthFormatter =
      new MonthFormatter(
        invalidKey = invalidKey,
        missingKey = monthRequiredKey
      )
    val monthFormatted = monthFormatter.bind(s"$key.month", Map(monthKey -> monthValue.getOrElse("")))

    val errors = validateDayMonthYear(
      key = key,
      day = dayValue,
      month = monthFormatted,
      year = yearValue
    )

    errors match {
      case Nil =>
        toDate(
          key = key,
          day = dayValue.get.toInt,
          month = monthFormatted.toOption.get,
          year = yearValue.get.toInt
        )
      case _ => Left(errors)
    }
  }

  override def unbind(
    key: String,
    value: LocalDate
  ): Map[String, String] = Map(
    s"$key.day" -> value.getDayOfMonth.toString,
    s"$key.month" -> value.getMonthValue.toString,
    s"$key.year" -> value.getYear.toString
  )

}
