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

package uk.gov.hmrc.agentregistrationfrontend.action

import play.api.mvc.*
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/** Provides extension methods for Play Framework's [[ActionBuilder]] to enable composable request refinement.
  */
object ActionBuilders
extends RequestAwareLogging:

  extension [
    R <: [X] =>> Request[X],
    ContentType // ContentType Represents Play Framework's Content Type parameter, commonly denoted as B
  ](ab: ActionBuilder[R, ContentType])(using ec: ExecutionContext)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensure(
      condition: R[ContentType] => Boolean | Future[Boolean],
      resultWhenConditionNotMet: R[ContentType] => Result
    ): ActionBuilder[R, ContentType] = refineUnion: (request: R[ContentType]) =>

      def process(c: Boolean): R[ContentType] | Result =
        if c
        then request
        else resultWhenConditionNotMet(request)

      condition(request) match
        case c: Boolean => process(c)
        case f: Future[_] => f.asInstanceOf[Future[Boolean]].map(process)

    def refineEither[P[_]](refineF: R[ContentType] => Either[Result, P[ContentType]]): ActionBuilder[P, ContentType] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = Future.successful(
        refineF.asInstanceOf[R[A] => Either[Result, P[A]]](request)
      )
    })

    def refineFutureEither[P[_]](refineF: R[ContentType] => Future[Either[Result, P[ContentType]]]): ActionBuilder[
      P,
      ContentType
    ] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = refineF.asInstanceOf[R[A] => Future[Either[Result, P[A]]]](request)
    })

    def refineUnion[P[_]](refineF: R[ContentType] => Result | P[ContentType] | Future[Result | P[ContentType]]): ActionBuilder[
      P,
      ContentType
    ] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = {
        val rB: R[ContentType] = request.asInstanceOf[R[ContentType]]
        refineF(rB) match
          case r: Result => Future.successful(Left[Result, P[A]](r))
          case f: Future[_] =>
            f.asInstanceOf[Future[Result | P[ContentType]]].map {
              case r: Result => Left[Result, P[A]](r)
              case p => Right[Result, P[A]](p.asInstanceOf[P[A]])
            }
          case p => Future.successful(Right[Result, P[A]](p.asInstanceOf[P[A]]))
      }
    })
