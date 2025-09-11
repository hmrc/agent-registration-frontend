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
import uk.gov.hmrc.agentregistration.shared.AmlsCode
import uk.gov.hmrc.agentregistration.shared.AmlsName
import uk.gov.hmrc.agentregistrationfrontend.testsupport.ISpec

class AmlsCodesSpec
extends ISpec:

  override lazy val overridesModule: AbstractModule =
    new AbstractModule:
      // Leaving configure empty to avoid binding AmlsCodes instance in test context
      override def configure(): Unit = ()

  "AmlsCodes should load prodcution AMLS codes which are defined in /amls.csv resource" in:
    val amlsCodes: AmlsCodes = app.injector.instanceOf[AmlsCodes]
    amlsCodes.amlsCodes shouldBe CsvLoader
      .load("/amls.csv")
      .map: kv =>
        (AmlsCode(kv._1), AmlsName(kv._2))

    amlsCodes.amlsCodes.nonEmpty shouldBe true withClue "sanity check that we actually have some options defiend in the amls.csv "
