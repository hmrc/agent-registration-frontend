/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.agentregistrationfrontend.services.applicant

import play.api.mvc.RequestHeader
import uk.gov.hmrc.agentregistrationfrontend.model.ProvidedByApplicant
import uk.gov.hmrc.agentregistrationfrontend.repository.ProvidedByApplicantRepo

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.Future

@Singleton
class ProvidedByApplicantService @Inject() (providedByApplicantRepo: ProvidedByApplicantRepo):

  def find()(using RequestHeader): Future[Option[ProvidedByApplicant]] = providedByApplicantRepo.find()

  def upsert(providedByApplicant: ProvidedByApplicant)(using RequestHeader): Future[Unit] = providedByApplicantRepo.upsert(providedByApplicant)
