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

package uk.gov.hmrc.agentregistrationfrontend.journey

import play.api.mvc.Request
import uk.gov.hmrc.agentregistrationfrontend.action.AuthorisedUtrRequest
import uk.gov.hmrc.agentregistrationfrontend.repository.JourneyRepo
import uk.gov.hmrc.agentregistrationfrontend.util.{Errors, RequestAwareLogging}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyService @Inject()(
    journeyRepo:    JourneyRepo,
    journeyFactory: JourneyFactory
)(implicit ec: ExecutionContext) extends RequestAwareLogging {

  def tryRestoreJourney()(implicit request: AuthorisedUtrRequest[_]) = {

    //TODO: find journey in backend, if found return it, if not create a new journey
    val maybeJourneyFromBackend: Option[Journey] = None //this is a placeholder
    maybeJourneyFromBackend
      .map(_.copy(sessionId = request.sessionId))
      .getOrElse(newJourney())

  }

  def newJourney()(implicit request: AuthorisedUtrRequest[_]): Future[Journey] = {
    val journey: Journey = journeyFactory.makeNewJourney(sessionId = request.sessionId)

    upsert(journey)
      .map{ _ =>
        logger.info(s"Started new journey [journeyId:${journey.id.value}]")
        journey
      }
  }

  def get(journeyId: JourneyId)(implicit request: Request[_]): Future[Journey] = find(journeyId).map { maybeJourney =>
    maybeJourney
      .getOrElse(Errors.throwServerErrorException(s"Expected journey to be found"))
  }


  def upsert[J <: Journey](journey: J)(implicit request: Request[_]): Future[J] = {
    logger.info(s"Upserting new journey...")
    journeyRepo
      .upsert(journey)
      .map(_ => journey)
  }

  def find(journeyId: JourneyId): Future[Option[Journey]] =
    journeyRepo
      .findById(journeyId)

  def find(sessionId: SessionId): Future[Option[Journey]] =
    journeyRepo
      .findBySessionId(sessionId)

}
