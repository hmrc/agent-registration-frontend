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

///*
// * Copyright 2025 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.agentregistration.shared
//
//import play.api.libs.json.Format
//import play.api.libs.json.Json
//import uk.gov.hmrc.agentregistration.shared.contactdetails.ApplicantName
//import uk.gov.hmrc.agentregistration.shared.util.JsonFormatsFactory
//
////
////  def getApplicantName: String =
////    applicantName match
////      case NameOfMember(
////            Some(_),
////            Some(officer),
////            _
////          ) =>
////        officer.name
////      case NameOfAuthorised(Some(name), _) => name
////      case _ => throw new RuntimeException("No applicant name found")
////
////  def getMemberNameQuery: CompaniesHouseNameQuery =
////    applicantName match
////      case NameOfMember(
////            Some(nameQuery),
////            _,
////            _
////          ) =>
////        nameQuery
////      case _ => throw new RuntimeException("No member name query found")
////
////  def readMemberNameQuery: Option[CompaniesHouseNameQuery] =
////    applicantName match
////      case NameOfMember(
////            maybeNameQuery,
////            _,
////            _
////          ) =>
////        maybeNameQuery
////      case _ => throw new RuntimeException("Member name query is only available for member types")
