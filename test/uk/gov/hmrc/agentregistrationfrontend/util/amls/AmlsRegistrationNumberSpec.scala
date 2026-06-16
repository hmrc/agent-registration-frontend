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

package uk.gov.hmrc.agentregistrationfrontend.util.amls

import uk.gov.hmrc.agentregistration.shared.amls.AmlsRegistrationNumber
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class AmlsRegistrationNumberSpec
extends UnitSpec:

  "isValidForChosenSupervisoryBody" should:
    "accept a valid HMRC registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "XZML00000999999",
        AmlsSupervisoryBodyCode("HMRC")
      ) shouldBe true

    "reject an invalid HMRC registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "XML0000012345",
        AmlsSupervisoryBodyCode("HMRC")
      ) shouldBe false

    "accept a valid AAT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "123456",
        AmlsSupervisoryBodyCode("AAT")
      ) shouldBe true

    "reject an invalid AAT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "ABC123",
        AmlsSupervisoryBodyCode("AAT")
      ) shouldBe false

    "accept a valid ACCA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "12345",
        AmlsSupervisoryBodyCode("ACCA")
      ) shouldBe true

    "reject an invalid ACCA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("ACCA")
      ) shouldBe false

    "accept a valid AIA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "12345",
        AmlsSupervisoryBodyCode("AIA")
      ) shouldBe true

    "reject an invalid AIA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234A",
        AmlsSupervisoryBodyCode("AIA")
      ) shouldBe false

    "accept a valid ATT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "ATT AML-1-123456",
        AmlsSupervisoryBodyCode("ATT")
      ) shouldBe true

    "reject an invalid ATT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("ATT")
      ) shouldBe false

    "accept a valid CIMA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234567",
        AmlsSupervisoryBodyCode("CIMA")
      ) shouldBe true

    "reject an invalid CIMA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("CIMA")
      ) shouldBe false

    "accept a valid CIOT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "CIOT AML-1-123456",
        AmlsSupervisoryBodyCode("CIOT")
      ) shouldBe true

    "reject an invalid CIOT registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("CIOT")
      ) shouldBe false

    "accept a valid FCA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "123456",
        AmlsSupervisoryBodyCode("FCA")
      ) shouldBe true

    "reject an invalid FCA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("FCA")
      ) shouldBe false

    "accept a valid ICAEW registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "A123456789",
        AmlsSupervisoryBodyCode("ICAEW")
      ) shouldBe true

    "reject an invalid ICAEW registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("ICAEW")
      ) shouldBe false

    "accept a valid ICAS registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "A123456",
        AmlsSupervisoryBodyCode("ICAS")
      ) shouldBe true

    "reject an invalid ICAS registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "123456ABC",
        AmlsSupervisoryBodyCode("ICAS")
      ) shouldBe false

    "accept a valid ICB registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "12345",
        AmlsSupervisoryBodyCode("ICB")
      ) shouldBe true

    "reject an invalid ICB registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "123456XYZ",
        AmlsSupervisoryBodyCode("ICB")
      ) shouldBe false

    "accept a valid IFA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "123456",
        AmlsSupervisoryBodyCode("IFA")
      ) shouldBe true

    "reject an invalid IFA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234",
        AmlsSupervisoryBodyCode("IFA")
      ) shouldBe false

    "accept a valid FRA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "12345",
        AmlsSupervisoryBodyCode("FRA")
      ) shouldBe true

    "reject an invalid FRA registration number" in:
      AmlsRegistrationNumber.isValidForChosenSupervisoryBody(
        "1234567",
        AmlsSupervisoryBodyCode("FRA")
      ) shouldBe false
