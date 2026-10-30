/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import uk.gov.hmrc.agentregistration.shared.InternalUserId
import uk.gov.hmrc.agentregistrationfrontend.testsupport.UnitSpec

class UserSpec
extends UnitSpec:

  Seq(
    ("theUser@thePlanet", Some((UserId("theUser"), PlanetId("thePlanet"))), "a manual stub sign-in"),
    ("applicant_68f2a1@MMTAR_68f2a1", Some((UserId("applicant_68f2a1"), PlanetId("MMTAR_68f2a1"))), "a fast-forward application"),
    ("theUser@hmrc", Some((UserId("theUser"), PlanetId("hmrc"))), "a user with no planet of their own"),
    ("theUser@the@Planet", Some((UserId("theUser"), PlanetId("the@Planet"))), "a planet containing the separator"),
    ("Int-7c3f1a2b-5d4e", None, "an id from the real auth service"),
    ("theUser@", None, "a missing planet"),
    ("@thePlanet", None, "a missing user"),
    ("@", None, "nothing but the separator"),
    ("", None, "an empty id")
  ).foreach:
    (
      internalUserId,
      expected,
      description
    ) =>
      s"parseIds should give $expected for $description" in:
        User.parseIds(InternalUserId(internalUserId)) shouldBe expected
