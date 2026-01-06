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

package uk.gov.hmrc.agentregistrationfrontend.testsupport.testdata

object TdAll:

  def apply(): TdAll = new TdAll {}

  val tdAll: TdAll = new TdAll {}

/** Test Data (Td) composition trait that combines All available test data instances.
  *
  * Implemented as composable traits to allow flexible customization of test data. For example:
  *
  * {{{
  * val td = new TdAll {
  *   override val saUtr: SaUtr = SaUtr("666667777")
  * }
  *
  * td.
  * }}}
  *
  * This allows reusing default test data while overriding specific values as needed.
  */
trait TdAll
extends TdBase,
  TdRequest,
  TdGrs,
  agentapplication.TdAgentApplicationRequest,
  agentapplication.llp.TdAgentApplicationLlp,
  agentapplication.llp.TdSectionContactDetails,
  agentapplication.llp.TdSectionAgentDetails,
  agentapplication.llp.TdUpload,
  agentapplication.llp.TdSectionAmls,
  agentapplication.soletrader.TdAgentApplicationSoleTrader,
  providedetails.member.TdMemberProvidedDetails,
  providedetails.TdProvideDetailsRequest
