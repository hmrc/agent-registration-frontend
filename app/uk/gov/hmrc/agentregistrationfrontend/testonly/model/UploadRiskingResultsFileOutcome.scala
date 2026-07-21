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

/** Outcome of uploading a risking results file to `agent-registration-risking`'s test-only SDES endpoint. That endpoint responds `409 Conflict` when a file
  * with the same name already exists, since results files are treated as write-once.
  */
enum UploadRiskingResultsFileOutcome:

  case Uploaded
  case AlreadyExists
