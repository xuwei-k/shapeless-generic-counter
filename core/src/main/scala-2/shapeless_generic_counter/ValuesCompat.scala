package shapeless_generic_counter

trait ValuesCompat { self: Values =>
  def asTupleOption = Values.unapply(self)
}
