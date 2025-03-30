package example

import io.circe.generic.auto._
import io.circe.Encoder
import io.circe.Decoder

case class A1(x1: Int, x2: String)

case class A2(x1: A1, x2: A2)

object A2 {
  def f1 = Encoder[A2]
  def f2 = Decoder[A2]
}
