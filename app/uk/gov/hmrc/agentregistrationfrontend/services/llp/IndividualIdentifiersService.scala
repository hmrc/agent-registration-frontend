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

package uk.gov.hmrc.agentregistrationfrontend.services.llp

import uk.gov.hmrc.agentregistrationfrontend.action.providedetails.llp.MemberProvideDetailsRequest
import uk.gov.hmrc.agentregistrationfrontend.connectors.llp.CitizenDetailsConnector
import uk.gov.hmrc.agentregistrationfrontend.model.llp.IndividualIdentifiers
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import play.api.mvc.RequestHeader

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

@Singleton
class IndividualIdentifiersService @Inject() (
  citizenDetailsConnector: CitizenDetailsConnector
)(using ec: ExecutionContext)
extends RequestAwareLogging:

  def extract[A](mpdRequest: MemberProvideDetailsRequest[A])(using rh: RequestHeader): Future[IndividualIdentifiers] =
    (mpdRequest.nino, mpdRequest.saUtr) match
      case (Some(nino), None) =>
        citizenDetailsConnector
          .getCitizenDetails(nino)
          .map { citizenDetails =>
            IndividualIdentifiers(
              nino = mpdRequest.nino,
              saUtr = citizenDetails.saUtr
            )
          }

      case _ =>
        Future.successful(
          IndividualIdentifiers(
            nino = mpdRequest.nino,
            saUtr = mpdRequest.saUtr
          )
        )
