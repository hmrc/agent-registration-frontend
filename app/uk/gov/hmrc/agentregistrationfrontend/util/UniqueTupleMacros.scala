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

object UniqueTupleMacros:

  private def cleanType(s: String): String = s
    .replaceAll("\\bscala\\.Predef\\.", "")
    .replaceAll("\\bscala\\.", "")
    .replaceAll("\\bjava\\.lang\\.", "")

  def failDuplicateTupleImpl[Data: Type](using Quotes): Expr[Nothing] =
    import quotes.reflect.*

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

    val duplicate: Option[TypeRepr] = findFirstDuplicate(types, Nil)
    val targetName: String = duplicate.map(t => cleanType(t.show)).getOrElse("Unknown")

    val formattedList = formatTupleContent(TypeRepr.of[Data])

    val msg = s"Tuple isn't unique. Type '$targetName' occurs more than once:\n$formattedList"
    '{ scala.compiletime.error(${ Expr(msg) }) }

  def makeAbsentInOrFailImpl[
    T: Type,
    Tup <: Tuple: Type
  ](using Quotes): Expr[UniqueTuple.AbsentIn[T, Tup]] =
    import quotes.reflect.*

    val target = TypeRepr.of[T]
    // val uniqueTupleSymbol = TypeRepr.of[UniqueTuple.type].typeSymbol.typeMember("UniqueTuple")
    val uniqueTupleSymbol = Symbol.requiredPackage("uk.gov.hmrc.agentregistrationfrontend.util").typeMember("UniqueTuple")

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def check(tup: TypeRepr): List[Expr[Any]] =
      tup.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === TypeRepr.of[*:].typeSymbol =>
          if head =:= target then
            val name = cleanType(target.show)
            val formattedList = formatTupleContent(TypeRepr.of[Tup])
            val msg = s"Type '$name' is already present in the tuple.\nAvailable types:\n$formattedList"
            List('{ scala.compiletime.error(${ Expr(msg) }) })
          else
            val headCheck =
              if head.typeSymbol.isTypeParam || head.typeSymbol.isAbstractType then
                head.asType match
                  case '[h] => List('{ scala.compiletime.summonInline[scala.util.NotGiven[h =:= T]] })
              else
                Nil
            headCheck ++ check(tail)
        case t if t.typeSymbol === TypeRepr.of[EmptyTuple].typeSymbol => Nil
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" =>
          args.flatMap { arg =>
            if arg =:= target then
              val name = cleanType(target.show)
              val formattedList = formatTupleContent(TypeRepr.of[Tup])
              val msg = s"Type '$name' is already present in the tuple.\nAvailable types:\n$formattedList"
              List('{ scala.compiletime.error(${ Expr(msg) }) })
            else if arg.typeSymbol.isTypeParam || arg.typeSymbol.isAbstractType then
              arg.asType match
                case '[a] => List('{ scala.compiletime.summonInline[scala.util.NotGiven[a =:= T]] })
            else
              Nil
          }
        case AppliedType(base, List(inner)) if base.typeSymbol === uniqueTupleSymbol => check(inner)
        case t =>
          if t =:= TypeRepr.of[Tup] then
            val msg = s"Cannot prove absence of ${cleanType(target.show)} in generic tuple ${cleanType(t.show)}"
            List('{ scala.compiletime.error(${ Expr(msg) }) })
          else
            t.asType match
              case '[t] => List('{ scala.compiletime.summonInline[UniqueTuple.AbsentIn[T, t & Tuple]] })

    val checks = check(TypeRepr.of[Tup])
    Expr.block(checks, '{ new UniqueTuple.AbsentIn[T, Tup] {} })

  def makePresentInOrFailImpl[
    T: Type,
    Tup <: Tuple: Type
  ](using Quotes): Expr[UniqueTuple.PresentIn[T, Tup]] =
    import quotes.reflect.*

    val target = TypeRepr.of[T]
    val consSymbol = TypeRepr.of[*:].typeSymbol
    val emptyTupleSymbol = TypeRepr.of[EmptyTuple].typeSymbol
//    val uniqueTupleSymbol = TypeRepr.of[UniqueTuple.type].typeSymbol.typeMember("UniqueTuple")

    val uniqueTupleSymbol = Symbol.requiredPackage("uk.gov.hmrc.agentregistrationfrontend.util").typeMember("UniqueTuple")

    def formatType(t: TypeRepr): String = cleanType(t.show)

    def notFoundError: Expr[Nothing] =
      val targetName = formatType(target)
      val formattedList = formatTupleContent(TypeRepr.of[Tup])
      val msg = s"Type '$targetName' is not present in the tuple.\nAvailable types:\n$formattedList"
      '{ scala.compiletime.error(${ Expr(msg) }) }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def check(tup: TypeRepr): Expr[UniqueTuple.PresentIn[T, Tup]] =
      tup.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === consSymbol =>
          if head =:= target then
            '{ new UniqueTuple.PresentIn[T, Tup] {} }
          else
            check(tail)
        case t if t.typeSymbol === emptyTupleSymbol => notFoundError
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" =>
          if args.exists(_ =:= target) then '{ new UniqueTuple.PresentIn[T, Tup] {} } else notFoundError
        case AppliedType(base, List(inner)) if base.typeSymbol === uniqueTupleSymbol => check(inner).asExprOf[UniqueTuple.PresentIn[T, Tup]]
        case t =>
          if t =:= target then '{ new UniqueTuple.PresentIn[T, Tup] {} }
          else
            t.asType match
              case '[t] => Expr.block(List('{ scala.compiletime.summonInline[UniqueTuple.PresentIn[T, t & Tuple]] }), '{ new UniqueTuple.PresentIn[T, Tup] {} })

    check(TypeRepr.of[Tup])

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def getTypes(using q: Quotes)(t: q.reflect.TypeRepr): List[q.reflect.TypeRepr] =
    import q.reflect.*
//    val uniqueTupleSymbol = TypeRepr.of[UniqueTuple.type].typeSymbol.typeMember("UniqueTuple")
    val uniqueTupleSymbol = Symbol.requiredPackage("uk.gov.hmrc.agentregistrationfrontend.util").typeMember("UniqueTuple")
    t.dealias match
      case AppliedType(base, List(head, tail)) if base.typeSymbol === TypeRepr.of[*:].typeSymbol => head :: getTypes(tail)
      case t if t.typeSymbol === TypeRepr.of[EmptyTuple].typeSymbol => Nil
      case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" => args
      case AppliedType(base, List(inner)) if base.typeSymbol === uniqueTupleSymbol => getTypes(inner)
      case other => List(other)

  private def formatTupleContent(using q: Quotes)(t: q.reflect.TypeRepr): String =
    val types = getTypes(t)
    if types.isEmpty then "  (Empty Tuple)"
    else types.map(t => s"  * ${cleanType(t.show)}").mkString("\n")
