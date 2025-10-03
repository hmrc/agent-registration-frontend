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

import play.api.data.Form
import play.api.data.FormBinding
import play.api.mvc.*
import play.api.mvc.Results.BadRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ActionsHelper
extends RequestAwareLogging:

  extension [
    R <: [X] =>> Request[X],
    B // B Represents Play Framework's Content Type parameter, commonly denoted as B
  ](ab: ActionBuilder[R, B])(using ec: ExecutionContext)

    def ensure(
      condition: R[B] => Boolean,
      resultWhenConditionNotMet: => Result
    ): ActionBuilder[R, B] = ensure(condition, _ => resultWhenConditionNotMet)

    def ensure(
      condition: R[B] => Boolean,
      resultWhenConditionNotMet: R[B] => Result
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec

      def filter[A](rA: R[A]): Future[Option[Result]] = Future.successful:
        given rB: R[B] = rA.asInstanceOf[R[B]]
        if condition(rB) then None
        else
          val result: Result = resultWhenConditionNotMet(rB)
          logger.warn(s"Condition not met for the request, responding with ${result.header.status}")
          Some(result))

    def ensureAsync(
      condition: R[B] => Future[Boolean],
      resultWhenConditionNotMet: R[B] => Future[Result]
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec
      def filter[A](rA: R[A]): Future[Option[Result]] =
        val rB: R[B] = rA.asInstanceOf[R[B]]
        for
          ok <- condition(rB)
          result <- if ok then Future.successful(None) else resultWhenConditionNotMet(rB).map(Some(_))
        yield result)

    def ensureValidForm[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab.andThen(new ActionRefiner[R, [X] =>> R[X] & FormValue[T]] {

      override protected def refine[A](rA: R[A]): Future[Either[Result, R[A] & FormValue[T]]] = Future.successful {
        val rB: R[B] = rA.asInstanceOf[R[B]]
        summon[R[B] <:< Request[B]]

        //        val requestB: Request[B] = ev.apply(rB)

        form.bindFromRequest()(using rB, fb).fold(
          hasErrors = formWithErrors => Left(BadRequest(viewToServeWhenFormHasErrors(rB)(formWithErrors))),
          success =
            (formValue: T) =>
              val x: R[A] & FormValue[T] = merge.mergeFormValue(rB, formValue).asInstanceOf[R[A] & FormValue[T]]
              Right(x)
        )

      }

      override protected def executionContext: ExecutionContext = ec
    })
