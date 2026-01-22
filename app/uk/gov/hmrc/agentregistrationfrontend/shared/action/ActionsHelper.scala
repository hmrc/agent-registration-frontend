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

package uk.gov.hmrc.agentregistrationfrontend.shared.action

import play.api.data.Form
import play.api.data.FormBinding
import play.api.mvc.*
import play.api.mvc.Results.BadRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
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
    ): ActionBuilder[R, B] = ensureAsync(
      request => Future.successful(condition(request)),
      request => Future.successful(resultWhenConditionNotMet(request))
    )

    def ensureAsync(
      condition: R[B] => Future[Boolean],
      resultWhenConditionNotMet: R[B] => Future[Result]
    ): ActionBuilder[R, B] = genericFilterAsync(
      condition,
      implicit request =>
        resultWhenConditionNotMet(request).map: result =>
          logger.warn(s"Condition not met for the request, responding with ${result.header.status}")
          result
    )

    def ensureValidFormGenericAsync[T](
      form: R[B] => Form[T],
      resultToServeWhenFormHasErrors: R[B] => Form[T] => Future[Result]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab.andThen(new ActionRefiner[R, [X] =>> R[X] & FormValue[T]] {

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      override protected def refine[A](rA: R[A]): Future[Either[Result, R[A] & FormValue[T]]] = {
        val rB: R[B] = rA.asInstanceOf[R[B]]
        form(rB).bindFromRequest()(using rB, fb).fold(
          hasErrors = formWithErrors => resultToServeWhenFormHasErrors(rB)(formWithErrors).map(Left(_)),
          success =
            (formValue: T) =>
              val x: R[A] & FormValue[T] = merge.mergeFormValue(rB, formValue).asInstanceOf[R[A] & FormValue[T]]
              Future.successful(Right(x))
        )
      }
      override protected def executionContext: ExecutionContext = ec
    })

    def ensureValidFormGeneric[T](
      form: R[B] => Form[T],
      resultToServeWhenFormHasErrors: R[B] => Form[T] => Result
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ensureValidFormGenericAsync(
      form,
      request => form => Future.successful(resultToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAsync[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab
      .ensureValidFormGenericAsync[T](
        _ => form,
        (r: R[B]) => (f: Form[T]) => viewToServeWhenFormHasErrors(r)(f).map(BadRequest.apply)
      )

    def ensureValidForm[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ensureValidFormAsync(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync[T](
      form: R[B] => Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T],
      ev: B =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab
      .ensureValidFormGenericAsync[T](
        form,
        (r: R[B]) =>
          (f: Form[T]) =>
            viewToServeWhenFormHasErrors(r)(f)
              .map(BadRequest.apply)
              .map(SubmissionHelper.redirectIfSaveForLater(ev.substituteCo(r), _))
      )

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: R[B] => Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T],
      ev: B =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ensureValidFormAndRedirectIfSaveForLaterAsync(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T],
      ev: B =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab.ensureValidFormAndRedirectIfSaveForLaterAsync(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[B] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[B], T],
      ev: B =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], B] = ab.ensureValidFormAndRedirectIfSaveForLater(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def genericFilter(
      condition: R[B] => Boolean,
      resultWhenConditionNotMet: R[B] => Result
    ): ActionBuilder[R, B] = genericFilterAsync(
      request => Future.successful(condition(request)),
      request => Future.successful(resultWhenConditionNotMet(request))
    )

    def genericFilterAsync(
      condition: R[B] => Future[Boolean],
      resultWhenConditionNotMet: R[B] => Future[Result]
    ): ActionBuilder[R, B] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      def filter[A](rA: R[A]): Future[Option[Result]] =
        val rB: R[B] = rA.asInstanceOf[R[B]]
        for
          ok <- condition(rB)
          result <- if ok then Future.successful(None) else resultWhenConditionNotMet(rB).map(Some(_))
        yield result)

    def genericActionFunction[P[_]](f: R[B] => P[B]): ActionFunction[R, P] = ab.andThen(new ActionFunction[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      def invokeBlock[A](
        request: R[A],
        block: P[A] => Future[Result]
      ): Future[Result] = block(f.asInstanceOf[R[A] => P[A]](request))
    })

    def genericActionRefiner[P[_]](f: R[B] => Either[Result, P[B]]): ActionBuilder[P, B] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = Future.successful(
        f.asInstanceOf[R[A] => Either[Result, P[A]]](request)
      )
    })

    def genericActionRefinerAsync[P[_]](f: R[B] => Future[Either[Result, P[B]]]): ActionBuilder[P, B] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = f.asInstanceOf[R[A] => Future[Either[Result, P[A]]]](request)
    })

  extension [
    B // B Represents Play Framework's Content Type parameter, commonly denoted as B
  ](a: Action[B])(using ec: ExecutionContext)

    def mapResult(f: Request[B] => Result => Result): Action[B] =
      new Action[B] {
        override def apply(request: Request[B]): Future[Result] = a(request).map(f(request))
        override def parser: BodyParser[B] = a.parser
        override def executionContext: ExecutionContext = a.executionContext
      }
