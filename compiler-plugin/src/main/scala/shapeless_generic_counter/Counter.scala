package shapeless_generic_counter

import java.util.concurrent.atomic.LongAdder
import scala.collection.concurrent.TrieMap

case class Counter(
  generic: TrieMap[String, LongAdder],
  labelled: TrieMap[String, LongAdder]
)

private object Counter {
  def create(): Counter = Counter(
    TrieMap.empty,
    TrieMap.empty,
  )
}
