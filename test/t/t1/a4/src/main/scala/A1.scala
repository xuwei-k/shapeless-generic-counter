package example

import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.auto._

case class A1(x1: Int, x2: String)

object A1 {
  def f1 = Encoder[A1]
}
