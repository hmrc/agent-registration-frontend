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
import uk.gov.hmrc.agentregistrationfrontend.action.Actions.*
import uk.gov.hmrc.agentregistrationfrontend.forms.helpers.SubmissionHelper
import uk.gov.hmrc.agentregistrationfrontend.util.RequestAwareLogging
import uk.gov.hmrc.agentregistrationfrontend.util.UniqueTuple.AbsentIn

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object ActionsHelper
extends RequestAwareLogging:

  type ActionBuilderWithData[Data <: Tuple] = ActionBuilder[[X] =>> RequestWithDataCt[X, Data], AnyContent]

  type ActionRefinerWithData[
    Data <: Tuple,
    NewData <: Tuple
  ] = ActionRefiner[[X] =>> RequestWithDataCt[X, Data], [X] =>> RequestWithDataCt[X, NewData]]

  extension [
    Data <: Tuple
  ](ab: ActionBuilderWithData[Data])(using ec: ExecutionContext)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidFormAndRedirectIfSaveForLater4[T](
      form: RequestWithData[Data] => Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidForm4(
      form = form,
      resultToServeWhenFormHasErrors =
        request =>
          form =>
            resultToServeWhenFormHasErrors(request)(form) match
              case r: Result => SubmissionHelper.redirectIfSaveForLater(request, r)
              case r: HtmlFormat.Appendable => SubmissionHelper.redirectIfSaveForLater(request, BadRequest(r))
              case r: Future[_] =>
                r.asInstanceOf[Future[Result | HtmlFormat.Appendable]].map:
                  case r: Result => SubmissionHelper.redirectIfSaveForLater(request, r)
                  case r: HtmlFormat.Appendable => SubmissionHelper.redirectIfSaveForLater(request, BadRequest(r))
    )

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidFormAndRedirectIfSaveForLater4[T](
      form: Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidFormAndRedirectIfSaveForLater4(_ => form, resultToServeWhenFormHasErrors)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidForm4[T](
      form: Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidForm4(_ => form, resultToServeWhenFormHasErrors)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidForm4[T](
      form: RequestWithData[Data] => Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = refine4:
      implicit request =>

        def handleErrors(formWithErrors: Form[T]): Future[Result] =
          resultToServeWhenFormHasErrors(request)(formWithErrors) match
            case r: Result => Future.successful(r)
            case r: HtmlFormat.Appendable => Future.successful(BadRequest(r))
            case f: Future[_] =>
              f.asInstanceOf[Future[Result | HtmlFormat.Appendable]].map {
                case r: Result => r
                case r: HtmlFormat.Appendable => BadRequest(r)
              }

        def bind(f: Form[T]): Future[Result | RequestWithData[T *: Data]] = f.bindFromRequest().fold(
          hasErrors = handleErrors,
          success = t => Future.successful(request.add(t))
        )

        form(request) match
          case f: Future[_] => f.asInstanceOf[Future[Form[T]]].flatMap(bind)
          case f: Form[_] => bind(f.asInstanceOf[Form[T]])

    def ensure4(
      condition: RequestWithData[Data] => Boolean,
      resultWhenConditionNotMet: Result
    ): ActionBuilderWithData[Data] = ensure4(condition, _ => resultWhenConditionNotMet)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensure4(
      condition: RequestWithData[Data] => Boolean | Future[Boolean],
      resultWhenConditionNotMet: RequestWithData[Data] => Result | Future[Result]
    ): ActionBuilderWithData[Data] = refine4: request =>

      def computeResult(condition: Boolean): Result | RequestWithData[Data] | Future[Result | RequestWithData[Data]] =
        if condition then request else resultWhenConditionNotMet(request)

      condition(request) match
        case c: Boolean => computeResult(c)
        case f: Future[_] =>
          f.asInstanceOf[Future[Boolean]].flatMap: c =>
            computeResult(c) match
              case result: Result => Future.successful(result)
              case request: RequestWithDataCt[_, _] => Future.successful(request.asInstanceOf[RequestWithData[Data]])
              case f: Future[_] => f.asInstanceOf[Future[Result | RequestWithData[Data]]]

    def refine4[NewData <: Tuple](
      refineF: RequestWithData[Data] => Result | RequestWithData[NewData] | Future[Result | RequestWithData[NewData]]
    ): ActionBuilderWithData[
      NewData
    ] = ab.andThen(new ActionRefinerWithData[Data, NewData] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      override protected def refine[A](request: RequestWithDataCt[
        A,
        Data
      ]): Future[Either[Result, RequestWithDataCt[A, NewData]]] = {
        val rB: RequestWithData[Data] = request.asInstanceOf[RequestWithData[Data]]
        refineF(rB) match
          case r: Result => Future.successful(Left(r))
          case f: Future[_] =>
            f.asInstanceOf[Future[Result | RequestWithData[NewData]]].map {
              case r: Result => Left(r)
              case p => Right(p.asInstanceOf[RequestWithDataCt[A, NewData]])
            }
          case p => Future.successful(Right(p.asInstanceOf[RequestWithDataCt[A, NewData]]))
      }
    })

  extension [
    Data <: Tuple,
    R <: [X] =>> RequestWithDataCt[X, Data],
    ContentType
  ](ab: ActionBuilder[R, ContentType])(using ec: ExecutionContext)

    def ensureValidFormGeneric2[T](
      form: R[ContentType] => Form[T],
      resultToServeWhenFormHasErrors: R[ContentType] => Form[T] => Result
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ensureValidFormGenericAsync2[T](
      form,
      request => form => Future.successful(resultToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAsync2[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ab
      .ensureValidFormGenericAsync2[T](
        _ => form,
        (r: R[ContentType]) => (f: Form[T]) => viewToServeWhenFormHasErrors(r)(f).map(BadRequest.apply)
      )

    def ensureValidForm2[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ensureValidFormAsync2(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync2[T](
      form: R[ContentType] => Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      ev: ContentType =:= AnyContent,
      ev2: T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ab
      .ensureValidFormGenericAsync2[T](
        form,
        (r: R[ContentType]) =>
          (f: Form[T]) =>
            viewToServeWhenFormHasErrors(r)(f)
              .map(BadRequest.apply)
              .map(SubmissionHelper.redirectIfSaveForLater(ev.substituteCo(r), _))
      )

    def ensureValidFormAndRedirectIfSaveForLater2[T](
      form: R[ContentType] => Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      ev: ContentType =:= AnyContent,
      ev2: T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ensureValidFormAndRedirectIfSaveForLaterAsync2(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync2[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      ev: ContentType =:= AnyContent,
      ev2: T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ab.ensureValidFormAndRedirectIfSaveForLaterAsync2(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def ensureValidFormAndRedirectIfSaveForLater2[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      ev: ContentType =:= AnyContent,
      ev2: T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ab.ensureValidFormAndRedirectIfSaveForLater2(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def ensureValidFormGenericAsync2[T](
      form: R[ContentType] => Form[T],
      resultToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[Result]
    )(using
      fb: FormBinding,
      ev: T AbsentIn Data
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, T *: Data], ContentType] = ab.andThen(new ActionRefiner[R, [X] =>> RequestWithDataCt[X, T *: Data]] {

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      override protected def refine[A](rA: R[A]): Future[Either[Result, RequestWithDataCt[A, T *: Data]]] = {
        val rB: R[ContentType] = rA.asInstanceOf[R[ContentType]]
        form(rB).bindFromRequest()(using rB, fb).fold(
          hasErrors = formWithErrors => resultToServeWhenFormHasErrors(rB)(formWithErrors).map(Left(_)),
          success =
            (formValue: T) =>
              val x: RequestWithDataCt[A, T *: Data] = rB.add(formValue).asInstanceOf[RequestWithDataCt[A, T *: Data]]
              Future.successful(Right(x))
        )
      }

      override protected def executionContext: ExecutionContext = ec
    })

  extension [
    R <: [X] =>> Request[X],
    ContentType // ContentType Represents Play Framework's Content Type parameter, commonly denoted as B
  ](ab: ActionBuilder[R, ContentType])(using ec: ExecutionContext)

    def ensure(
      condition: R[ContentType] => Boolean,
      resultWhenConditionNotMet: => Result
    ): ActionBuilder[R, ContentType] = ensure(condition, _ => resultWhenConditionNotMet)

    def ensure(
      condition: R[ContentType] => Boolean,
      resultWhenConditionNotMet: R[ContentType] => Result
    ): ActionBuilder[R, ContentType] = ensureAsync(
      request => Future.successful(condition(request)),
      request => Future.successful(resultWhenConditionNotMet(request))
    )

    def ensureAsync(
      condition: R[ContentType] => Future[Boolean],
      resultWhenConditionNotMet: R[ContentType] => Future[Result]
    ): ActionBuilder[R, ContentType] = genericFilterAsync(
      condition,
      implicit request =>
        resultWhenConditionNotMet(request).map: result =>
          logger.warn(s"Condition not met for the request, responding with ${result.header.status}")
          result
    )

    def ensureValidFormGenericAsync[T](
      form: R[ContentType] => Form[T],
      resultToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[Result]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ab.andThen(new ActionRefiner[R, [X] =>> R[X] & FormValue[T]] {

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      override protected def refine[A](rA: R[A]): Future[Either[Result, R[A] & FormValue[T]]] = {
        val rB: R[ContentType] = rA.asInstanceOf[R[ContentType]]
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
      form: R[ContentType] => Form[T],
      resultToServeWhenFormHasErrors: R[ContentType] => Form[T] => Result
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ensureValidFormGenericAsync(
      form,
      request => form => Future.successful(resultToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAsync[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ab
      .ensureValidFormGenericAsync[T](
        _ => form,
        (r: R[ContentType]) => (f: Form[T]) => viewToServeWhenFormHasErrors(r)(f).map(BadRequest.apply)
      )

    def ensureValidForm[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T]
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ensureValidFormAsync(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync[T](
      form: R[ContentType] => Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T],
      ev: ContentType =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ab
      .ensureValidFormGenericAsync[T](
        form,
        (r: R[ContentType]) =>
          (f: Form[T]) =>
            viewToServeWhenFormHasErrors(r)(f)
              .map(BadRequest.apply)
              .map(SubmissionHelper.redirectIfSaveForLater(ev.substituteCo(r), _))
      )

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: R[ContentType] => Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T],
      ev: ContentType =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ensureValidFormAndRedirectIfSaveForLaterAsync(
      form,
      request => form => Future.successful(viewToServeWhenFormHasErrors(request)(form))
    )

    def ensureValidFormAndRedirectIfSaveForLaterAsync[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => Future[HtmlFormat.Appendable]
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T],
      ev: ContentType =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ab.ensureValidFormAndRedirectIfSaveForLaterAsync(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: Form[T],
      viewToServeWhenFormHasErrors: R[ContentType] => Form[T] => HtmlFormat.Appendable
    )(using
      fb: FormBinding,
      merge: MergeFormValue[R[ContentType], T],
      ev: ContentType =:= AnyContent
    ): ActionBuilder[[X] =>> R[X] & FormValue[T], ContentType] = ab.ensureValidFormAndRedirectIfSaveForLater(
      _ => form,
      viewToServeWhenFormHasErrors
    )

    def genericFilter(
      condition: R[ContentType] => Boolean,
      resultWhenConditionNotMet: R[ContentType] => Result
    ): ActionBuilder[R, ContentType] = genericFilterAsync(
      request => Future.successful(condition(request)),
      request => Future.successful(resultWhenConditionNotMet(request))
    )

    def genericFilterAsync(
      condition: R[ContentType] => Future[Boolean],
      resultWhenConditionNotMet: R[ContentType] => Future[Result]
    ): ActionBuilder[R, ContentType] = ab.andThen(new ActionFilter[R]:
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      def filter[A](rA: R[A]): Future[Option[Result]] =
        val rB: R[ContentType] = rA.asInstanceOf[R[ContentType]]
        for
          ok <- condition(rB)
          result <- if ok then Future.successful(None) else resultWhenConditionNotMet(rB).map(Some(_))
        yield result)

    def refine[P[_]](refineF: R[ContentType] => Either[Result, P[ContentType]]): ActionBuilder[P, ContentType] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = Future.successful(
        refineF.asInstanceOf[R[A] => Either[Result, P[A]]](request)
      )
    })

    def refineAsync[P[_]](refineF: R[ContentType] => Future[Either[Result, P[ContentType]]]): ActionBuilder[
      P,
      ContentType
    ] = ab.andThen(new ActionRefiner[R, P] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, P[A]]] = refineF.asInstanceOf[R[A] => Future[Either[Result, P[A]]]](request)
    })

    def refine3[NewData <: Tuple](
      refineF: R[ContentType] => Result | RequestWithDataCt[ContentType, NewData] | Future[Result | RequestWithDataCt[ContentType, NewData]]
    ): ActionBuilder[[X] =>> RequestWithDataCt[X, NewData], ContentType] = ab.andThen(new ActionRefiner[R, [X] =>> RequestWithDataCt[X, NewData]] {
      protected def executionContext: ExecutionContext = ec

      @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
      protected def refine[A](request: R[A]): Future[Either[Result, RequestWithDataCt[A, NewData]]] = {
        val rB: R[ContentType] = request.asInstanceOf[R[ContentType]]
        refineF(rB) match
          case r: Result => Future.successful(Left(r))
          case f: Future[_] =>
            f.asInstanceOf[Future[Result | RequestWithDataCt[ContentType, NewData]]].map {
              case r: Result => Left(r)
              case p => Right(p.asInstanceOf[RequestWithDataCt[A, NewData]])
            }
          case p => Future.successful(Right(p.asInstanceOf[RequestWithDataCt[A, NewData]]))
      }
    })

    def refine2[P[_]](refineF: R[ContentType] => Result | P[ContentType] | Future[Result | P[ContentType]]): ActionBuilder[
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

  extension [
    B // B Represents Play Framework's Content Type parameter, commonly denoted as B
  ](a: Action[B])(using ec: ExecutionContext)

    def mapResult(f: Request[B] => Result => Result): Action[B] =
      new Action[B] {
        override def apply(request: Request[B]): Future[Result] = a(request).map(f(request))
        override def parser: BodyParser[B] = a.parser
        override def executionContext: ExecutionContext = a.executionContext
      }
