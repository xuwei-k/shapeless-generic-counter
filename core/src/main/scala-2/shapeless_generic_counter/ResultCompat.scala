package shapeless_generic_counter

trait ResultCompat { self: Result =>
  def asTupleOption = Result.unapply(self)
}
