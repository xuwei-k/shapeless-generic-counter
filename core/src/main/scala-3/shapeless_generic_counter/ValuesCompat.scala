package shapeless_generic_counter

trait ValuesCompat { self: Values =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
