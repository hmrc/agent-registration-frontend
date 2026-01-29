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

object TupleTool:

  @implicitNotFound("Type ${T} is already present in the tuple ${Tup}")
  infix trait AbsentIn[T, Tup <: Tuple]

  object AbsentIn:
    transparent inline given [T, Tup <: Tuple]: AbsentIn[T, Tup] = ${ TupleToolMacros.absentInImpl[T, Tup] }

  extension [Data <: Tuple](data: Data)

    inline def addByType[T](value: T)(using T AbsentIn Data): T *: Data = value *: data

    inline def getByType[T]: T =
      inline if constValue[TupleToolMacros.IsMember[Data, T]] then
        find[Data, T](data)
      else
        fail[Data, T]

    inline def updateByType[T](value: T): Data =
      inline if constValue[TupleToolMacros.IsMember[Data, T]] then
        replace[Data, T](data, value)
      else
        fail[Data, T]

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    inline def replaceByType[Old, New](value: New): TupleToolMacros.Replace[Data, Old, New] =
      inline if constValue[TupleToolMacros.IsMember[Data, Old]] then
        replaceType[Data, Old, New](data, value).asInstanceOf[TupleToolMacros.Replace[Data, Old, New]]
      else
        fail[Data, Old]

    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    inline def deleteByType[T]: TupleToolMacros.Delete[Data, T] =
      inline if constValue[TupleToolMacros.IsMember[Data, T]] then
        deleteType[Data, T](data).asInstanceOf[TupleToolMacros.Delete[Data, T]]
      else
        fail[Data, T]

    inline def ensureUnique: Data =
      inline if constValue[TupleToolMacros.HasDuplicates[Data]] then
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

  private inline def fail[Data, T]: Nothing = ${ TupleToolMacros.failImpl[Data, T] }

  private inline def failDuplicateTuple[Data]: Nothing = ${ TupleToolMacros.failDuplicateTupleImpl[Data] }
