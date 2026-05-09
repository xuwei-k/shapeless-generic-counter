package shapeless_generic_counter

trait ResultCompat { self: Result =>
  def asTupleOption = Option(Tuple.fromProductTyped(self))
}
