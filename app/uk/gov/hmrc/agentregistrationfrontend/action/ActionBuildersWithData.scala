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

/** Action builders and refiners tailored for working with [[RequestWithData]] types.
  */
object ActionBuildersWithData
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
    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: RequestWithData[Data] => Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidForm(
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
    def ensureValidFormAndRedirectIfSaveForLater[T](
      form: Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidFormAndRedirectIfSaveForLater(_ => form, resultToServeWhenFormHasErrors)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidForm[T](
      form: Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = ensureValidForm(_ => form, resultToServeWhenFormHasErrors)

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensureValidForm[T](
      form: RequestWithData[Data] => Form[T] | Future[Form[T]],
      resultToServeWhenFormHasErrors: RequestWithData[Data] => Form[T] => Result | HtmlFormat.Appendable | Future[Result | HtmlFormat.Appendable]
    )(using
      FormBinding,
      T AbsentIn Data
    ): ActionBuilderWithData[T *: Data] = refineWithData:
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

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    def ensure(
      condition: RequestWithData[Data] => Boolean | Future[Boolean],
      resultWhenConditionNotMet: RequestWithData[Data] => Result | Future[Result]
    ): ActionBuilderWithData[Data] = refineWithData: request =>

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

    def refineWithData[NewData <: Tuple](
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
