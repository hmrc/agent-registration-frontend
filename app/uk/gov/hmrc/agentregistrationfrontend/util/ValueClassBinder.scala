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

package uk.gov.hmrc.agentregistrationfrontend.util

import play.api.libs.json.*
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

import scala.reflect.TypeTest

object ValueClassBinder:

  def valueClassBinder[A: Reads](fromAtoString: A => String)(using stringBinder: PathBindable[String]): PathBindable[A] =

    def parseString(str: String): Either[String, A] =
      JsString(str).validate[A] match
        case JsSuccess(a, _) => Right(a)
        case JsError(error) => Left(s"No valid value in path: $str. Error: ${error.toString}")

    new PathBindable[A]:
      override def bind(
        key: String,
        value: String
      ): Either[String, A] = stringBinder.bind(key, value).flatMap(parseString)

      override def unbind(
        key: String,
        a: A
      ): String = stringBinder.unbind(key, fromAtoString(a))

  inline def bindableA[A: Reads](fromAtoString: A => String)(using TypeTest[Any, A]): QueryStringBindable[A] =
    new QueryStringBindable.Parsing[A](
      parse = JsString(_).as[A],
      fromAtoString,
      error =
        (
          key: String,
          _: Exception
        ) => s"Cannot parse param $key as ${summon[TypeTest[Any, A]].toString}"
    )

  inline def queryStringValueBinder[A: Reads](fromAtoString: A => String)(using TypeTest[Any, A]): QueryStringBindable[A] =
    new QueryStringBindable.Parsing[A](
      parse = JsString(_).as[A],
      fromAtoString,
      error = {
        case (key: String, e: JsResultException) =>
          s"Cannot parse param $key as ${summon[TypeTest[Any, A]].toString}. ${e.errors.headOption.flatMap(_._2.headOption.map(_.message)).getOrElse("")}"
        case (key: String, e) => s"Cannot parse param $key as ${summon[TypeTest[Any, A]].toString}. ${e.toString}"
      }
    )
