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

package uk.gov.hmrc.agentregistrationfrontend.model.agentdetails

import uk.gov.hmrc.agentregistration.shared.agentdetails.AgentCorrespondenceAddress
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import uk.gov.hmrc.agentregistration.shared.util.StringExtensions.replaceCommasWithSpaces
import uk.gov.hmrc.agentregistrationfrontend.model.addresslookup.GetConfirmedAddressResponse

object AgentCorrespondenceAddressHelper:

  extension (agentCorrespondenceAddress: AgentCorrespondenceAddress)

    /** Render the address as a single comma-separated string.
      *
      * Useful for comparing to other addresses stored in this format such as radio values. Commas within address lines are replaced with spaces to avoid
      * mis-alignment of fields, so for example the line "12a, Baker Street" becomes "12a Baker Street".
      *
      * @return
      *   a comma-separated single-line representation of the address
      */
    def toValueString: String = Seq(
      agentCorrespondenceAddress.addressLine1.replaceCommasWithSpaces,
      agentCorrespondenceAddress.addressLine2.getOrElse("").replaceCommasWithSpaces,
      agentCorrespondenceAddress.addressLine3.getOrElse("").replaceCommasWithSpaces,
      agentCorrespondenceAddress.addressLine4.getOrElse("").replaceCommasWithSpaces,
      agentCorrespondenceAddress.postalCode.getOrElse("").replaceCommasWithSpaces,
      agentCorrespondenceAddress.countryCode
    )
      .filter(_.nonEmpty).mkString(", ")

    def toHtml: String = Seq(
      Some(
        agentCorrespondenceAddress.addressLine1
      ),
      agentCorrespondenceAddress.addressLine2,
      agentCorrespondenceAddress.addressLine3,
      agentCorrespondenceAddress.addressLine4,
      agentCorrespondenceAddress.postalCode,
      Some(agentCorrespondenceAddress.countryCode)
    ).flatten.mkString("<br>")

  /* Parse from a string created by toValueString on either Companies House
   * Registered Office addresses (ChroAddress) or the BPR addresses (DesBusinessAddress).
   * Both produce a string with address lines separated by commas. Input lines will have
   * had commas found in individual source lines replaced with spaces so this is a safe operation.
   */
  def fromValueString(address: String): AgentCorrespondenceAddress =
    val parts = address.split(",").map(_.trim)
    if parts.length === 6
    then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = parts.lift(2),
        addressLine4 = parts.lift(3),
        postalCode = parts.lift(4),
        countryCode = parts.lift(5).getOrElse("")
      )
    else if parts.length === 5
    then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = parts.lift(2),
        addressLine4 = None,
        postalCode = parts.lift(3),
        countryCode = parts.lift(4).getOrElse("")
      )
    else if parts.length === 4
    then
      AgentCorrespondenceAddress(
        addressLine1 = parts.headOption.getOrElse(""),
        addressLine2 = parts.lift(1),
        addressLine3 = None,
        addressLine4 = None,
        postalCode = parts.lift(2),
        countryCode = parts.lift(3).getOrElse("")
      )
    else
      throw new IllegalArgumentException(s"Cannot parse CorrespondenceAddress from string: $address")

  /** Create an `AgentCorrespondenceAddress` from an `AddressLookupFrontendAddress`.
    *
    * Only the first four lines from the ALF address are used to remain compatible with this type.
    *
    * @param address
    *   the Address Lookup Frontend address
    * @return
    *   reconstructed `AgentCorrespondenceAddress`
    */
  def fromAddressLookupAddress(address: GetConfirmedAddressResponse): AgentCorrespondenceAddress =
    val lines = address.lines
    AgentCorrespondenceAddress(
      addressLine1 = lines.headOption.getOrElse(""),
      addressLine2 = lines.lift(1),
      addressLine3 = lines.lift(2),
      addressLine4 = lines.lift(3),
      postalCode = address.postcode,
      countryCode = address.country.code
    )
