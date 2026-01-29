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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results
import play.api.mvc.Results.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.agentregistration.shared.AgentApplication
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class RequestWithDataSpec
extends UnitSpec:

//  extension [
//    A,
//    Data <: Tuple
//  ](r: RequestWithData[A, Data])
//    def getString = r.get[String]

  type RequestX[A] = RequestWithData[
    A,
    (String, Int, Option[AgentApplication], (Int, Float))
  ]

  "duplicate test" in:
    RequestWithData(
      request = FakeRequest(),
      data = ("foo", 42, "boo")
    )

  "showcase" in:
    val r: RequestX[AnyContentAsEmpty.type] = RequestWithData(
      request = FakeRequest(),
      data = ("foo", 42, None, (11, 3.14f))
    )

    r.get[String] shouldBe "foo"
    r.get[Int] shouldBe 42
//    r.get[Float] won't compile
    r.get[Option[AgentApplication]] shouldBe None
    val r2 = r.add(Results.BadRequest).add(Results.NotFound)
    r2.get[Status] shouldBe Results.BadRequest
