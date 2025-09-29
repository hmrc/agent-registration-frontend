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

package uk.gov.hmrc.agentregistration.shared.util

import play.api.libs.json.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===
import play.api.libs.functional.syntax.*
import play.api.libs.json.Format
import scala.compiletime.erasedValue
import scala.compiletime.error
import scala.deriving.Mirror
import scala.reflect.ClassTag

object JsonFormatsFactory:

  /** Creates a Format for Scala 3 enums by automatically retrieving all enum values.
    */
  inline def makeEnumFormat[E <: reflect.Enum](using ct: ClassTag[E]): Format[E] = makeFormat(EnumValues.all[E])

  /** Creates a Format for sealed objects
    */
  inline def makeSealedObjectFormat[E](using ct: ClassTag[E]): Format[E] = makeFormat(SealedObjects.all[E])

  /** Creates a Format for Scala 3 enums with explicitly provided enum values.
    */
  private def makeFormat[E](values: Iterable[E])(using ct: ClassTag[E]): Format[E] =
    val enumName = ct.runtimeClass.getSimpleName
    Format(
      Reads { json =>
        json.validate[String].flatMap { str =>
          values
            .find(_.toString === str)
            .fold[JsResult[E]](JsError(s"Unknown value for enum $enumName: '$str'"))(JsSuccess(_))
        }
      },
      Writes(e => JsString(e.toString))
    )

  /** Compile-time checked derivation of play json Format for case classes of the form: case class X(value: String)
    */
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  inline def makeValueClassFormat[A](using
    m: Mirror.ProductOf[A],
    strFmt: Format[String]
  ): Format[A] =
    // Check the single element type is String
    inline erasedValue[m.MirroredElemTypes] match
      case _: Tuple1[String] =>
        // Check the single element label is "value"
        inline erasedValue[m.MirroredElemLabels] match
          case _: Tuple1["value"] =>
            val base = summon[Format[String]]
            base.inmap[A](
              s => m.fromProduct(Tuple1(s)),
              a => a.asInstanceOf[Product].productElement(0).asInstanceOf[String]
            )
          case _ =>
            error(
              "'JsonFormatsFactory.makeValueClassFormat' can only be used for case classes with exactly one field of type String,\nie. case class X(value: String)"
            )
      case _ =>
        error(
          "'JsonFormatsFactory.makeValueClassFormat' can only be used for case classes with exactly one field of type String,\nie. case class X(value: String)"
        )
