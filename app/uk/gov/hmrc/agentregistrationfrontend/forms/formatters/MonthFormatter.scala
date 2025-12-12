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

package uk.gov.hmrc.agentregistrationfrontend.forms.formatters

import play.api.data.FormError
import play.api.data.format.Formatter
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import java.time.Month

class MonthFormatter(
  invalidKey: String,
  missingKey: String,
  args: Seq[String] = Seq.empty
)
extends Formatter[Int] {

  private val baseFormatter =
    new Formatter[String] {

      override def bind(
        key: String,
        data: Map[String, String]
      ): Either[Seq[FormError], String] =
        data.get(key) match {
          case None =>
            Left(Seq(FormError(
              key = key,
              message = missingKey,
              args = args
            )))
          case Some(s) if s.trim.isEmpty =>
            Left(Seq(FormError(
              key = key,
              message = missingKey,
              args = args
            )))
          case Some(s) => Right(s)
        }

      override def unbind(
        key: String,
        value: String
      ): Map[String, String] = Map(key -> value)
    }

  def bind(
    key: String,
    data: Map[String, String]
  ): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap {
        str =>
          months
            .find(m => m.getValue.toString === str.replaceAll("^0+", "") || m.toString === str.toUpperCase || m.toString.take(3) === str.toUpperCase)
            .map(x => Right(x.getValue))
            .getOrElse(Left(List(FormError(
              key = key,
              message = invalidKey,
              args = args
            ))))
      }
  }

  override def unbind(
    key: String,
    value: Int
  ): Map[String, String] = Map(key -> value.toString)

}
