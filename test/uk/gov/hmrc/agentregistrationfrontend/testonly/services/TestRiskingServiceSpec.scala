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

package uk.gov.hmrc.agentregistrationfrontend.testonly.services

import play.api.libs.json.Json
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec
import uk.gov.hmrc.agentregistration.shared.ApplicationReference
import uk.gov.hmrc.agentregistration.shared.PersonReference
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsFilename
import uk.gov.hmrc.agentregistrationfrontend.testonly.model.RiskingResultsRecord

class TestRiskingServiceSpec
extends UnitSpec:

  "combineResultsJson" should:
    "return an empty JSON array when there are no files to combine" in:
      TestRiskingService.combineResultsJson(Seq.empty) shouldBe Json.arr()

    "append every record from each source file in order" in:
      val entityFilename = RiskingResultsFilename("test-only-entity-XARN0000001")
      val individualFilename = RiskingResultsFilename("test-only-individual-PERS0000001")

      val combined = TestRiskingService.combineResultsJson(Seq(
        entityFilename ->
          // language=JSON
          """[
            |  {
            |    "recordType": "Entity",
            |    "applicationReference": "XARN0000001",
            |    "failures": []
            |  }
            |]""".stripMargin,
        individualFilename ->
          // language=JSON
          """[
            |  {
            |    "recordType": "Individual",
            |    "personReference": "PERS0000001",
            |    "failures": []
            |  },
            |  {
            |    "recordType": "Individual",
            |    "personReference": "PERS0000002",
            |    "failures": [
            |      {
            |        "reasonCode": "6.1",
            |        "reasonDescription": "Some failure",
            |        "checkId": "6",
            |        "checkDescription": "Sanctions"
            |      }
            |    ]
            |  }
            |]""".stripMargin
      ))

      combined shouldBe Json.arr(
        Json.obj(
          "recordType" -> "Entity",
          "applicationReference" -> "XARN0000001",
          "failures" -> Json.arr()
        ),
        Json.obj(
          "recordType" -> "Individual",
          "personReference" -> "PERS0000001",
          "failures" -> Json.arr()
        ),
        Json.obj(
          "recordType" -> "Individual",
          "personReference" -> "PERS0000002",
          "failures" -> Json.arr(Json.obj(
            "reasonCode" -> "6.1",
            "reasonDescription" -> "Some failure",
            "checkId" -> "6",
            "checkDescription" -> "Sanctions"
          ))
        )
      )

    "fail with a helpful message when a source file is not a JSON array" in:
      val filename = RiskingResultsFilename("test-only-bad-file")

      val thrown = intercept[IllegalArgumentException]:
        // language=JSON
        TestRiskingService.combineResultsJson(Seq(filename -> """{"recordType":"Entity"}"""))

      thrown.getMessage should include(filename.value)
      thrown.getMessage should include("did not contain a JSON array")

  "parseRecordReferences should read who each record is about" in:
    TestRiskingService.parseRecordReferences(
      RiskingResultsFilename("test-only-combined-20261015T100000000Z"),
      // language=JSON
      """[
        |  { "recordType": "Entity", "applicationReference": "XARN0000001", "failures": [] },
        |  { "recordType": "Individual", "personReference": "PERS0000001", "failures": [] }
        |]""".stripMargin
    ) shouldBe Seq(
      RiskingResultsRecord.ForApplication(ApplicationReference("XARN0000001")),
      RiskingResultsRecord.ForPerson(PersonReference("PERS0000001"))
    )

  "parseRecordReferences should fail on a record of unknown type" in:
    val thrown = intercept[IllegalArgumentException]:
      // language=JSON
      TestRiskingService.parseRecordReferences(RiskingResultsFilename("test-only-bad-file"), """[{"recordType":"Agent"}]""")

    thrown.getMessage shouldBe "Risking results file 'test-only-bad-file' has a record of unknown type 'Agent'"
