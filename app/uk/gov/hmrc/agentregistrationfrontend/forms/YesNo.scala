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

package uk.gov.hmrc.agentregistrationfrontend.forms

enum YesNo:

  case Yes
  case No

//TODO PAV - hmm con convinced that is the right plave to configure that 
object YesNo:

  extension (yn: YesNo)
    def toBoolean: Boolean =
      yn match
        case YesNo.Yes => true
        case YesNo.No => false

  extension (b: Boolean)
    def toYesNo: YesNo = if b then YesNo.Yes else YesNo.No
