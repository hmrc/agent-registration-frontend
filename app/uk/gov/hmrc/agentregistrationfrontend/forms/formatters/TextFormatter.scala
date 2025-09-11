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

/** Simple text formatter that allows customizing error messages. It accepts any string, even empty, and returns it.
  */
class TextFormatter(
  errorMessageIfMissing: String,
  args: String*
)
extends Formatter[String]:

  def bind(
    key: String,
    data: Map[String, String]
  ) = data
    .get(key)
    .toRight(Seq(FormError(
      key = key,
      message = errorMessageIfMissing,
      args = args
    )))

  def unbind(
    key: String,
    value: String
  ) = Map(key -> value)
