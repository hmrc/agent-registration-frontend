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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Request, Result}
import uk.gov.hmrc.agentregistrationfrontend.config.AppConfig
import uk.gov.hmrc.agentregistrationfrontend.journey.{JourneyId, JourneyService}
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GetJourneyActionRefiner @Inject() (
    journeyService: JourneyService,
    appConfig:      AppConfig
)(implicit ec: ExecutionContext)
  extends ActionRefiner[AuthorisedUtrRequest, JourneyRequest]
    with RequestAwareLogging {

  override protected def refine[A](request: AuthorisedUtrRequest[A]): Future[Either[Result, JourneyRequest[A]]] = {
    implicit val r: Request[A] = request

      for {
          maybeJourney <- journeyService.find(request.sessionId)
        } yield {
          maybeJourney match {
            case Some(journey) => Right(new JourneyRequest(journey, request))
            case None =>
              logger.warn(s"Journey not found based on the sessionId from session, redirecting to the restore-journey route")
              Left(
                Redirect(
                  uk.gov.hmrc.agentregistrationfrontend.controllers.routes.JourneyController.tryToRestoreJourney
                )
              )
          }
        }
    }

  override protected def executionContext: ExecutionContext = ec
}
