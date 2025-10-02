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

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ActionsHelper:

  extension [
    R <: [X] =>> Request[X],
    B // Represents Play Framework's Content Type parameter, commonly denoted as B
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
        else Some(resultWhenConditionNotMet(rB)))

    //    @targetName("ensureAsync")
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

//    def ensureValidForm2[T](
//      form: Form[T],
//      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
//    )(using
//      ev: R[B] <:< Request[B]
//    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
//      protected def executionContext: ExecutionContext = ec
//
//      def filter[A](request: R[A]): Future[Option[Result]] =
//        val r1: R[B] = request.asInstanceOf[R[B]]
//        given r: Request[B] = ev.apply(request.asInstanceOf[R[B]])
//        given FormBinding = formBinding
//        form.bindFromRequest().fold[Future[Option[Result]]](
//          hasErrors = formWithErrors => Future.successful(Some(BadRequest(viewToServeWhenFormHasErrors(r1)(formWithErrors)))),
//          success = _ => Future.successful(None)
//        ))
//
//    def refineWith[C](f: R[B] => Either[Result, C])(using
//      ev: R[B] <:< Request[B]
//    ) = ab.andThen(new ActionRefiner[R, Collected[C]] {
//      protected def executionContext: ExecutionContext = ec
//      def refine[A](request: R[A]): Future[Either[Result, CollectedRequest[C, A]]] = Future.successful {
//        f(request.asInstanceOf[R[B]]) match
//          case Left(res) => Left(res)
//          case Right(c) =>
//            val base = request.asInstanceOf[Request[A]]
//            Right(CollectedRequest[C, A](base, c))
//      }
//    })
//
//    def ensureValidForm[T](
//      form: Form[T],
//      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
//    )(using
//      fb: FormBinding,
//      ev: R[B] <:< Request[B]
//    ): ActionBuilder[[X] =>> FormRequest[X, R[X], T], B] = ab.andThen(new ActionRefiner[R, [X] =>> FormRequest[X, R[X], T]] {
//      protected def executionContext: ExecutionContext = ec
//
//      def refine[A](request: R[A]): Future[Either[Result, FormRequest[A, R[A], T]]] =
//        val rB: R[B] = request.asInstanceOf[R[B]]
//        val baseB: Request[B] = ev(rB)
//        form.bindFromRequest()(using baseB, fb).fold(
//          hasErrors =
//            fe =>
//              Future.successful(Left(BadRequest(viewToServeWhenFormHasErrors(rB)(fe)))),
//          success =
//            value =>
//              Future.successful(Right(new FormRequest[A, R[A], T](value, request)))
//        )
//    })

//    def ensureValidForm[T](
//      form: Form[T],
//      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
//    )(using
//      ev: R[B] <:< Request[B]
//    ): ActionBuilder[[X] =>> FormRequest[
//      X,
//      R[X],
//      T
//    ], B] = ab.andThen(new ActionRefiner[R, [X] =>> FormRequest[
//      X,
//      R[X],
//      T
//    ]]:
//      protected def executionContext: ExecutionContext = ec
//
//      def refine[A](request: R[A]): Future[Either[Result, FormRequest[
//        A,
//        R[A],
//        T
//      ]]] = Future.successful {
//        val rB: R[B] = request.asInstanceOf[R[B]]
//        val baseB: Request[B] = ev(rB)
////        val r1: R[B] = request.asInstanceOf[R[B]]
////        given r: Request[B] = ev.apply(request.asInstanceOf[R[B]])
//
////        given FormBinding = formBinding
//        form.bindFromRequest()(using baseB, formBinding).fold[Either[Result, FormRequest[
//          A,
//          R[A],
//          T
//        ]]](
//          hasErrors =
//            formWithErrors =>
//              Left(BadRequest(viewToServeWhenFormHasErrors(rB)(formWithErrors))),
//          success =
//            t =>
//              Right(FormRequest[
//                A,
//                R[A],
//                T
//              ](formValue = t, request = request))
//        )
//
//      }

//      def filter[A](request: R[A]): Future[Option[Result]] =
//        val r1: R[B] = request.asInstanceOf[R[B]]
//        given r: Request[B] = ev.apply(request.asInstanceOf[R[B]])
//        given FormBinding = formBinding
//        form.bindFromRequest().fold[Future[Option[Result]]](
//          hasErrors = formWithErrors => Future.successful(Some(BadRequest(viewToServeWhenFormHasErrors(r1)(formWithErrors)))),
//          success = _ => Future.successful(None)
//        ))

//  extension [
//    ContentType,
//    R[_] <: Request[ContentType]
//  ](action: ActionBuilder[R, ContentType])
//
//    def ensure(
//      condition: R[ContentType] => Boolean,
//      resultWhenConditionNotMet: R[ContentType] => Result
//    ): ActionBuilder[R, ContentType] = action.andThen(self.ensure(
//      condition = condition,
//      resultWhenConditionNotMet = resultWhenConditionNotMet
//    ))
//
//    def ensureAsync(
//      condition: R[ContentType] => Future[Boolean],
//      resultWhenConditionNotMet: R[ContentType] => Future[Result]
//    ): ActionBuilder[R, ContentType] = action.andThen(self.ensureAsync(
//      condition = condition,
//      resultWhenConditionNotMet = resultWhenConditionNotMet
//    ))
//
//    def ensureValidForm[T](
//      form: Form[T],
//      viewWhenFormHasErrors: R[ContentType] => HtmlFormat.Appendable
//    ): ActionBuilder[R, ContentType] = {
//      given FormBinding = formBinding
//      action.andThen(self.ensure(
//        condition = request => !form.bindFromRequest()(using request).hasErrors,
//        resultWhenConditionNotMet = request => BadRequest(viewWhenFormHasErrors(request))
//      ))
//    }
//
//  /** Checks if the Request matches the condition. If it doesn't, it will respond with the given result.
//    *
//    * Add extra logging when needed to the `resultWhenConditionNotMet` function.
//    */
//  private def ensureAsync[
//    T,
//    R[_] <: Request[T]
//  ](
//    condition: R[T] => Future[Boolean],
//    resultWhenConditionNotMet: R[T] => Future[Result]
//  ): ActionFilter[R] =
//
//    new ActionFilter[R]:
//      override def filter[A](request: R[A]): Future[Option[Result]] =
//        for
//          c <- condition(request.asInstanceOf[R[T]])
//          result <-
//            if (c)
//              Future.successful(None)
//            else
//              resultWhenConditionNotMet(request.asInstanceOf[R[T]]).map(Some(_))
//        yield result
//
//      override protected def executionContext: ExecutionContext = ec
//
//  /** Checks if the Request matches the condition. If it doesn't, it will respond with the given result.
//    *
//    * Add extra logging when needed to the `resultWhenConditionNotMet` function.
//    */
//  private def ensure[
//    T,
//    R[_] <: Request[T]
//  ](
//    condition: R[T] => Boolean,
//    resultWhenConditionNotMet: R[T] => Result
//  ): ActionFilter[R] = ensureAsync[T, R](
//    condition = r => Future.successful(condition(r)),
//    resultWhenConditionNotMet = r => Future.successful(resultWhenConditionNotMet(r))
//  )
