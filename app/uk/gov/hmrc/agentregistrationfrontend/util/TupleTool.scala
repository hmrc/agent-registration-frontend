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

import scala.annotation.implicitNotFound
import scala.compiletime.*
import uk.gov.hmrc.agentregistration.shared.util.SafeEquals.===

import scala.quoted.*

object TupleTool:

  @implicitNotFound("Type ${T} is already present in the tuple ${Tup}")
  infix trait AbsentIn[T, Tup <: Tuple]

  object AbsentIn:
    transparent inline given [T, Tup <: Tuple]: AbsentIn[T, Tup] = ${ absentInImpl[T, Tup] }

//  @implicitNotFound("Type ${T} is not present in the tuple ${Tup}")
  infix trait PresentIn[T, Tup <: Tuple]

  object PresentIn:
    transparent inline given [T, Tup <: Tuple]: PresentIn[T, Tup] = ${ presentInImpl[T, Tup] }

  private type IsMember[T, Tup <: Tuple] <: Boolean =
    Tup match
      case T *: _ => true
      case _ *: tail => IsMember[T, tail]
      case EmptyTuple => false

  private type HasDuplicates[Tup <: Tuple] <: Boolean =
    Tup match
      case EmptyTuple => false
      case h *: t =>
        IsMember[h, t] match
          case true => true
          case false => HasDuplicates[t]

  type Replace[
    Old,
    New,
    Tup <: Tuple
  ] <: Tuple =
    Tup match
      case Old *: tail => New *: tail
      case h *: tail => h *: Replace[Old, New, tail]
      case EmptyTuple => EmptyTuple

  type Delete[
    T,
    Tup <: Tuple
  ] <: Tuple =
    Tup match
      case T *: tail => tail
      case h *: tail => h *: Delete[T, tail]
      case EmptyTuple => EmptyTuple

  extension [Data <: Tuple](data: Data)

    inline def addByType[T](value: T)(using T AbsentIn Data): T *: Data = value *: data

    inline def getByType[T](using T PresentIn Data): T =
      inline if constValue[IsMember[T, Data]] then
        find[Data, T](data)
      else
        ???

    inline def updateByType[T](value: T)(using T PresentIn Data): Data =
      inline if constValue[IsMember[T, Data]] then
        replace[Data, T](data, value)
      else
        ???

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    inline def replaceByType[Old, New](value: New)(using
      Old PresentIn Data,
      New AbsentIn Data
    ): Replace[Old, New, Data] =
      inline if constValue[IsMember[Old, Data]] then
        replaceType[Data, Old, New](data, value).asInstanceOf[Replace[Old, New, Data]]
      else
        ???

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    inline def deleteByType[T](using T PresentIn Data): Delete[T, Data] =
      inline if constValue[IsMember[T, Data]] then
        deleteType[Data, T](data).asInstanceOf[Delete[T, Data]]
      else
        ???

    inline def ensureUnique: Data =
      inline if constValue[HasDuplicates[Data]] then
        failDuplicateTuple[Data]
      else
        data

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private inline def find[Tup, E](t: Any): E =
    inline erasedValue[Tup] match
      case _: (E *: tail) => t.asInstanceOf[E *: tail].head
      case _: (h *: tail) => find[tail, E](t.asInstanceOf[h *: tail].tail)
      case _ => error("Type not found in tuple")

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private inline def replace[Tup <: Tuple, E](t: Tup, v: E): Tup =
    inline erasedValue[Tup] match
      case _: (E *: tail) =>
        val cons = t.asInstanceOf[E *: tail]
        (v *: cons.tail).asInstanceOf[Tup]
      case _: (h *: tail) =>
        val cons = t.asInstanceOf[h *: tail]
        (cons.head *: replace[tail, E](cons.tail, v)).asInstanceOf[Tup]
      case _ => error("Type not found in tuple")

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private inline def replaceType[
    Tup <: Tuple,
    Old,
    New
  ](t: Tup, v: New): Tuple =
    inline erasedValue[Tup] match
      case _: (Old *: tail) =>
        val cons = t.asInstanceOf[Old *: tail]
        v *: cons.tail
      case _: (h *: tail) =>
        val cons = t.asInstanceOf[h *: tail]
        cons.head *: replaceType[tail, Old, New](cons.tail, v)
      case _ => error("Type not found in tuple")

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf", "org.wartremover.warts.Recursion"))
  private inline def deleteType[Tup <: Tuple, T](t: Tup): Tuple =
    inline erasedValue[Tup] match
      case _: (T *: tail) => t.asInstanceOf[T *: tail].tail
      case _: (h *: tail) =>
        val cons = t.asInstanceOf[h *: tail]
        cons.head *: deleteType[tail, T](cons.tail)
      case _ => error("Type not found in tuple")

  private inline def failDuplicateTuple[Data]: Nothing = ${ failDuplicateTupleImpl[Data] }

  private def cleanType(s: String): String = s
    .replaceAll("\\bscala\\.Predef\\.", "")
    .replaceAll("\\bscala\\.", "")
    .replaceAll("\\bjava\\.lang\\.", "")

  def failDuplicateImpl[
    T: Type,
    Data: Type
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

  def absentInImpl[
    T: Type,
    Tup <: Tuple: Type
  ](using Quotes): Expr[TupleTool.AbsentIn[T, Tup]] =
    import quotes.reflect.*

    val target = TypeRepr.of[T]

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def check(tup: TypeRepr): List[Expr[Any]] =
      tup.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === TypeRepr.of[*:].typeSymbol =>
          if head =:= target then
            val name = cleanType(target.show)
            val msg = s"Type '$name' is already present in the tuple."
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
              val msg = s"Type '$name' is already present in the tuple."
              List('{ scala.compiletime.error(${ Expr(msg) }) })
            else if arg.typeSymbol.isTypeParam || arg.typeSymbol.isAbstractType then
              arg.asType match
                case '[a] => List('{ scala.compiletime.summonInline[scala.util.NotGiven[a =:= T]] })
            else
              Nil
          }
        case t =>
          if t =:= TypeRepr.of[Tup] then
            val msg = s"Cannot prove absence of ${cleanType(target.show)} in generic tuple ${cleanType(t.show)}"
            List('{ scala.compiletime.error(${ Expr(msg) }) })
          else
            t.asType match
              case '[t] => List('{ scala.compiletime.summonInline[TupleTool.AbsentIn[T, t & Tuple]] })

    val checks = check(TypeRepr.of[Tup])
    Expr.block(checks, '{ new TupleTool.AbsentIn[T, Tup] {} })

  def presentInImpl[
    T: Type,
    Tup <: Tuple: Type
  ](using Quotes): Expr[TupleTool.PresentIn[T, Tup]] =
    import quotes.reflect.*

    val target = TypeRepr.of[T]
    val consSymbol = TypeRepr.of[*:].typeSymbol
    val emptyTupleSymbol = TypeRepr.of[EmptyTuple].typeSymbol

    def formatType(t: TypeRepr): String = cleanType(t.show)

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def getTupleElements(t: TypeRepr): List[String] =
      t.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === consSymbol => formatType(head) :: getTupleElements(tail)
        case t if t.typeSymbol === emptyTupleSymbol => Nil
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" => args.map(formatType)
        case other => List(formatType(other))

    def notFoundError: Expr[Nothing] =
      val dataElements = getTupleElements(TypeRepr.of[Tup])
      val targetName = formatType(target)
      val formattedList =
        if dataElements.isEmpty then "  (Empty Tuple)"
        else dataElements.map(s => s"  * $s").mkString("\n")
      val msg = s"Type '$targetName' is not present in the tuple.\nAvailable types:\n$formattedList"
      '{ scala.compiletime.error(${ Expr(msg) }) }

    @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
    def check(tup: TypeRepr): Expr[TupleTool.PresentIn[T, Tup]] =
      tup.dealias match
        case AppliedType(base, List(head, tail)) if base.typeSymbol === consSymbol =>
          if head =:= target then
            '{ new TupleTool.PresentIn[T, Tup] {} }
          else
            check(tail)
        case t if t.typeSymbol === emptyTupleSymbol => notFoundError
        case AppliedType(base, args) if base.typeSymbol.name.startsWith("Tuple") && base.typeSymbol.owner.fullName === "scala" =>
          if args.exists(_ =:= target) then '{ new TupleTool.PresentIn[T, Tup] {} } else notFoundError
        case t =>
          if t =:= target then '{ new TupleTool.PresentIn[T, Tup] {} }
          else
            t.asType match
              case '[t] => Expr.block(List('{ scala.compiletime.summonInline[TupleTool.PresentIn[T, t & Tuple]] }), '{ new TupleTool.PresentIn[T, Tup] {} })

    check(TypeRepr.of[Tup])
