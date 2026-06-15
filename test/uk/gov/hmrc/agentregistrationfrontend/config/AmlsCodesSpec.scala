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

import com.google.inject.AbstractModule
import uk.gov.hmrc.agentregistration.shared.amls.AmlsName
import uk.gov.hmrc.agentregistration.shared.amls.AmlsSupervisoryBodyCode
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec

class AmlsCodesSpec
extends ISpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      // Leaving configure empty to avoid binding AmlsCodes instance in test context
      override def configure(): Unit = ()

  val amlsCodes: AmlsCodes = app.injector.instanceOf[AmlsCodes]

  "AmlsCodes should load production AMLS codes which are defined in /amls.csv resource" in:
    amlsCodes.amlsCodes shouldBe CsvLoader
      .load("/amls.csv")
      .map: kv =>
        (AmlsSupervisoryBodyCode(kv._1), AmlsName(kv._2))

    amlsCodes.amlsCodes.nonEmpty shouldBe true withClue "sanity check that we actually have some options defined in the amls.csv "

  "getSupervisoryName should return corresponding AMLS name or throw exception" in:
    amlsCodes.getSupervisoryName(AmlsSupervisoryBodyCode("ACCA")) shouldBe AmlsName("Association of Chartered Certified Accountants (ACCA)")

    intercept[RuntimeException](amlsCodes.getSupervisoryName(AmlsSupervisoryBodyCode("sialala")))
      .getMessage should
      startWith("No supervisory body found for AmlsSupervisoryBodyCode(sialala)")
