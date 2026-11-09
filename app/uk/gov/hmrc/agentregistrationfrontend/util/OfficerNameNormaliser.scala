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

import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.canonicalise

import scala.util.matching.Regex

object OfficerNameNormaliser:

  def normalise(rawName: String): String =
    val canonicalised: String = rawName.canonicalise
    splitNameParts(canonicalised) match
      case Some(nameParts) => s"${capitaliseNameParts(nameParts.forenames)} ${capitaliseNameParts(nameParts.surname)}"
      // already "Forename Surname", or surname first with nothing to swap
      case None => capitaliseNameParts(canonicalised)

  /** Companies House gives a name as "SURNAME, Forename(s), Title", where the title is optional and we do not use it. None when the name does not carry both a
    * surname and forenames in that format.
    */
  def splitNameParts(canonicalisedName: String): Option[NameParts] =
    if canonicalisedName.contains(",")
    then
      canonicalisedName.split(",").map(_.trim).toList match
        case surname :: forenames :: _ => Some(NameParts(surname = surname, forenames = forenames))
        case _ => None
    else None

  final case class NameParts(
    surname: String,
    forenames: String
  )

  /** Lowercases the name, then capitalises each part: SMITH-JONES becomes Smith-Jones and O'CONNOR becomes O'Connor, not O'connor.
    */
  def capitaliseNameParts(name: String): String = namePartDelimiter.replaceAllIn(name.toLowerCase, m => m.group(1).toUpperCase)

  /** Matches each part's first letter. The lookbehind (?<=...) excludes the delimiter from the match, so only the letter is replaced. Parts start at the name's
    * beginning (^) or after whitespace, a full stop, a straight apostrophe, a right smart quote or a hyphen. The capture group (\p{L}) holds the letter to
    * capitalise; \p{L} means a letter in any script.
    */
  private val namePartDelimiter: Regex = "(?<=^|[\\s.'’-])(\\p{L})".r
