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

package uk.gov.hmrc.agentregistrationfrontend.config

import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.mockito.MockitoSugar

import scala.collection.immutable.ListMap

class CsvLoaderSpec
extends AnyWordSpecLike
with Matchers
with MockitoSugar:

  implicit val mockAppConfig: AppConfig = mock[AppConfig]
  def csvLoader = new CsvLoader()

  ".load" should:

    "return a ListMap of key value pairs when the CSV file is loaded and parsed correctly" in:
      when(mockAppConfig.amlsCodesPath).thenReturn("/testAmlsCodes.csv")
      csvLoader.load(
        location = mockAppConfig.amlsCodesPath
      )
        .shouldBe(ListMap(
          "ATT" -> "Association of TaxationTechnicians (ATT)",
          "HMRC" -> "HM Revenue and Customs (HMRC)"
        ))

    "return a ListMap of names as values when the CSV file is loaded with namesAsValues being true" in:
      when(mockAppConfig.amlsCodesPath).thenReturn("/testAmlsCodes.csv")
      csvLoader.load(
        namesAsValues = true,
        location = mockAppConfig.amlsCodesPath
      )
        .shouldBe(ListMap(
          "Association of TaxationTechnicians (ATT)" -> "Association of TaxationTechnicians (ATT)",
          "HM Revenue and Customs (HMRC)" -> "HM Revenue and Customs (HMRC)"
        ))

    "return an exception" when:

      "the CSV file is loaded but no values were parsed" in:
        when(mockAppConfig.amlsCodesPath).thenReturn("/testNoCodes.csv")
        intercept[RuntimeException](csvLoader.load(
          location = mockAppConfig.amlsCodesPath
        ))
          .getMessage
          .shouldBe("No keys or values found")

      "the file path is empty" in:
        when(mockAppConfig.amlsCodesPath).thenReturn("")
        intercept[RuntimeException](csvLoader.load(
          location = mockAppConfig.amlsCodesPath
        ))
          .getMessage
          .shouldBe("requirement failed: The file path should not be empty")

      "the target file is not a CSV type" in:
        when(mockAppConfig.amlsCodesPath).thenReturn("/testInvalidFileType.txt")
        intercept[RuntimeException](csvLoader.load(
          location = mockAppConfig.amlsCodesPath
        ))
          .getMessage
          .shouldBe("requirement failed: The file should be a csv file")
