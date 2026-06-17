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

package uk.gov.hmrc.agentregistration.shared.risking

import uk.gov.hmrc.agentregistration.shared.util.DisjointUnions

object EntityFailureHelper:

  import EntityFailure.*

  type IsAmls = (_3._1.type | _3._2.type | _3._3.type | _3._4.type | _3._5.type) & Fixable

  type IsNotAmls =
    (_4._1.type | _4._2.type | _4._3.type | _4._4.type
      | _5._1.type | _5._2.type | _5._3.type | _5._4.type | _5._5.type | _5._6.type | _5._7.type
      | _8._5.type | _8._7.type) & Fixable

  DisjointUnions.prove[
    Fixable,
    IsAmls,
    IsNotAmls
  ]
