package example

import io.circe.generic.auto._
import io.circe.Encoder
import io.circe.Decoder

case class A1(x1: Int, x2: String)

object A1 {
  def f1 = Encoder[A1]
}
