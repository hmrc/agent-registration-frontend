/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.quoted.*

object TupleMacros:

  type IsMember[Tup <: Tuple, T] <: Boolean =
    Tup match
      case T *: _ => true
      case _ *: tail => IsMember[tail, T]
      case EmptyTuple => false

  type HasDuplicates[Tup <: Tuple] <: Boolean =
    Tup match
      case EmptyTuple => false
      case h *: t =>
        IsMember[t, h] match
          case true => true
          case false => HasDuplicates[t]

  private def cleanType(s: String): String = s.replaceAll("\\bscala\\.Predef\\.", "")
    .replaceAll("\\bscala\\.", "")

  /** Fail compilation if type T is not part of the Data tuple. Print readable compiler error message.
    */
  def failImpl[
    Data: Type,
    T: Type
  ](using Quotes): Expr[Nothing] =
    import quotes.reflect.*

    val consSymbol: Symbol = TypeRepr.of[*:].typeSymbol
    val emptyTupleSymbol: Symbol = TypeRepr.of[EmptyTuple].typeSymbol

    def formatType(t: TypeRepr): String = cleanType(t.show)

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def getTupleElements(t: TypeRepr): List[String] =
      t.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === consSymbol => formatType(head) :: getTupleElements(tail)
        case t if t.typeSymbol === emptyTupleSymbol => Nil
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" => args.map(formatType)
        case other => List(formatType(other))

    val dataElements: List[String] = getTupleElements(TypeRepr.of[Data])
    val targetName: String = formatType(TypeRepr.of[T])

    val formattedList: String =
      if dataElements.isEmpty then
        "  (Empty Tuple)"
      else
        dataElements.map(s => s"  * $s").mkString("\n")

    val msg = s"Type '$targetName' is not present in the tuple.\nAvailable types:\n$formattedList"
    '{ scala.compiletime.error(${ Expr(msg) }) }

  def failDuplicateImpl[
    Data: Type,
    T: Type
  ](using Quotes): Expr[Nothing] =
    import quotes.reflect.*
    val targetName: String = cleanType(TypeRepr.of[T].show)
    val msg = s"Type '$targetName' is already present in the tuple."
    '{ scala.compiletime.error(${ Expr(msg) }) }

  def failDuplicateTupleImpl[Data: Type](using Quotes): Expr[Nothing] =
    import quotes.reflect.*

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def getTypes(t: TypeRepr): List[TypeRepr] =
      t.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === TypeRepr.of[*:].typeSymbol => head :: getTypes(tail)
        case t if t.typeSymbol === TypeRepr.of[EmptyTuple].typeSymbol => Nil
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" => args
        case other => List(other)

    val types = getTypes(TypeRepr.of[Data])

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def findFirstDuplicate(
      ts: List[TypeRepr],
      seen: List[TypeRepr]
    ): Option[TypeRepr] =
      ts match
        case Nil => None
        case h :: tail =>
          if seen.exists(s => s =:= h) then Some(h)
          else findFirstDuplicate(tail, h :: seen)

    val duplicate = findFirstDuplicate(types, Nil)
    val targetName = duplicate.map(t => cleanType(t.show)).getOrElse("Unknown")

    val msg = s"Type '$targetName' is already present in the tuple."
    '{ scala.compiletime.error(${ Expr(msg) }) }
