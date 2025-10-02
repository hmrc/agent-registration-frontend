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
import uk.gov.hmrc.agentregistrationfrontend.util.*
import uk.gov.hmrc.play.bootstrap.data.UrlEncodedOnlyFormBinding

import javax.inject.Inject
import javax.inject.Singleton
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

final class CollectedRequest[C, A](
  val underlying: Request[A],
  val collected: C
)
extends WrappedRequest[A](underlying)

@Singleton
class RequestEnsurer @Inject() ()(using ec: ExecutionContext)
extends RequestAwareLogging:
  self =>

  type Collected[C] = [X] =>> CollectedRequest[C, X]

//  type WithForm[
//    T,
//    R <: Request[T]
//  ] = [X] =>> FormRequest[X, R[X], T]

  extension [R[_], B](ab: ActionBuilder[R, B])

    //    @targetName("filterWithAsync")
    def filterWithAsync(
      predicate: R[B] => Future[Boolean],
      onReject: R[B] => Future[Result]
    )(using
      ev: R[B] <:< Request[B]
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec
      def filter[A](request: R[A]): Future[Option[Result]] = predicate(request.asInstanceOf[R[B]]).flatMap { ok =>
        if ok then Future.successful(None)
        else onReject(request.asInstanceOf[R[B]]).map(Some(_))
      })

    def ensureValidForm2[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      ev: R[B] <:< Request[B]
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec

      def filter[A](request: R[A]): Future[Option[Result]] =
        val r1: R[B] = request.asInstanceOf[R[B]]
        given r: Request[B] = ev.apply(request.asInstanceOf[R[B]])
        given FormBinding = formBinding
        form.bindFromRequest().fold[Future[Option[Result]]](
          hasErrors = formWithErrors => Future.successful(Some(BadRequest(viewToServeWhenFormHasErrors(r1)(formWithErrors)))),
          success = _ => Future.successful(None)
        ))

    def refineWith[C](f: R[B] => Either[Result, C])(using
      ev: R[B] <:< Request[B]
    ) = ab.andThen(new ActionRefiner[R, Collected[C]] {
      protected def executionContext: ExecutionContext = ec
      def refine[A](request: R[A]): Future[Either[Result, CollectedRequest[C, A]]] = Future.successful {
        f(request.asInstanceOf[R[B]]) match
          case Left(res) => Left(res)
          case Right(c) =>
            val base = request.asInstanceOf[Request[A]]
            Right(CollectedRequest[C, A](base, c))
      }
    })

    def ensureValidForm[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      ev: R[B] <:< Request[B]
    ): ActionBuilder[[X] =>> FormRequest[X, R[X], T], B] = ab.andThen(new ActionRefiner[R, [X] =>> FormRequest[X, R[X], T]] {
      protected def executionContext: ExecutionContext = ec

      def refine[A](request: R[A]): Future[Either[Result, FormRequest[A, R[A], T]]] =
        val rB: R[B] = request.asInstanceOf[R[B]]
        val baseB: Request[B] = ev(rB)
        form.bindFromRequest()(using baseB, fb).fold(
          hasErrors =
            fe =>
              Future.successful(Left(BadRequest(viewToServeWhenFormHasErrors(rB)(fe)))),
          success =
            value =>
              Future.successful(Right(new FormRequest[A, R[A], T](value, request)))
        )
    })

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

    def ensure(
      condition: R[B] => Boolean,
      resultWhenConditionNotMet: R[B] => Result
    )(using
      ev: R[B] <:< Request[B]
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec

      def filter[A](request: R[A]): Future[Option[Result]] =
        given r: R[B] = request.asInstanceOf[R[B]]
        if condition(r) then
          Future.successful(None)
        else Future.successful(Some(resultWhenConditionNotMet(r))))

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

  private val formBinding: FormBinding = new UrlEncodedOnlyFormBinding
