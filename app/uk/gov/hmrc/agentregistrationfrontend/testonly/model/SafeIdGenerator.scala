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

package uk.gov.hmrc.agentregistrationfrontend.testonly.model

import uk.gov.hmrc.agentregistration.shared.SafeId

import scala.util.Random

/** Generates a random SafeId matching the real HMRC format confirmed against the DES1170 (Business Partner) schema: `X` + one uppercase letter + literal `000`
  * + 10 digits, 15 characters total, e.g. `"XA0001234567890"` — matching regex `^X[A-Z]000[0-9]{10}$`.
  *
  * Must be unique per test run/journey, never a fixed value — agents-external-stubs' session-less DES/HIP-style lookups (e.g. the subscription call risking
  * makes once risking completes) can only find the BPR this safeId is stored under if it's unique to this run; a shared/fixed safeId would let concurrent
  * fast-forward runs or GRS-stub journeys collide (see FastForwardController/GrsStubController).
  */
object SafeIdGenerator:

  private val random: Random = new Random()

  @SuppressWarnings(Array("org.wartremover.warts.SeqApply"))
  def generateSafeId(): SafeId =
    val letter: Char = ('A' to 'Z')(random.nextInt(26))
    val digits: String = (1 to 10).map(_ => random.nextInt(10)).mkString
    SafeId(s"X${letter}000$digits")
