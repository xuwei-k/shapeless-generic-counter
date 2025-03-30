package example

import shapeless.Generic
import shapeless.HNil
import shapeless.::

case class A2(x1: Int)

object A2 {
  def f1 = Generic[A2]
}

case class A3(x1: Int)

case class A4(x1: Int)

object A4 {
  def f1 = Generic[A4]
}

object A3 {
  implicit val instance: Generic.Aux[A3, Int :: HNil] =
    Generic.materialize
}

object Test1 {
  def f1 = Generic[A3]
  def f2 = Generic[A3]
  def f3 = Generic[A3]
}
