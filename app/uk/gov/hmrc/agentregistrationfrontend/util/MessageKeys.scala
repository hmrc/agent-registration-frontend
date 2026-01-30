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

import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

object MessageKeys:

  /* @function singularOrPlural
   * Returns the appropriate message key based on whether the count is singular or plural. Messages can be customized
   * by providing different singular and plural keys.
   * @param count The number to evaluate.
   * @param singularKey The message key to return if count is singular (default: "singular").
   * @param pluralKey The message key to return if count is plural (default: "plural").
   * @return The appropriate message key based on the count.
   * 
   * Example usage: singularOrPlural(1) returns "singular"
   *                singularOrPlural(0) returns "plural" - even though zero is not plural in strict grammatical terms, it is treated as plural here.
   *                singularOrPlural(5) returns "plural"

   * Example twirl usage:
   *  @messages(s"itemsFound.${singularOrPlural(howManyItems)}", howManyItems)
   *
   * Example messages file entries:
   *   itemsFound.singular = "1 item found"
   *   itemsFound.plural = "{0} items found"
   *
   */
  def singularOrPlural(
    count: Int,
    singularKey: String = "singular",
    pluralKey: String = "plural"
  ): String = if count === 1 then singularKey else pluralKey

  def ordinalKey(
    existingSize: Int,
    isOnlyOne: Boolean
  ): String =
    if isOnlyOne then "only"
    else
      existingSize match
        case a: Int if a === 0 => "first"
        case _ => "subsequent"
