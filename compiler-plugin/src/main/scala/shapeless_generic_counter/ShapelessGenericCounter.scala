package shapeless_generic_counter

import argonaut.EncodeJson
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.concurrent.atomic.LongAdder
import scala.collection.concurrent.TrieMap
import scala.tools.nsc.Global
import scala.tools.nsc.Phase
import scala.tools.nsc.plugins.OutputFileWriter
import scala.tools.nsc.plugins.Plugin
import scala.tools.nsc.plugins.PluginComponent

object ShapelessGenericCounter {
  private implicit val resultInstance: EncodeJson[Result] =
    EncodeJson.jencode2L(Result.unapply(_: Result).get)("name", "count")

  private implicit val valuesInstance: EncodeJson[Values] =
    EncodeJson.jencode2L(Values.unapply(_: Values).get)("type", "values")
}

class ShapelessGenericCounter(override val global: Global) extends Plugin {
  import ShapelessGenericCounter.*

  override val name = "shapeless-generic-counter"
  override val description = "count shapeless.Generic.instance call"

  private val counter = Counter.create()

  private var outputFile: String = ""

  override def init(options: List[String], error: String => Unit): Boolean = {
    val key = "output:"
    options.collectFirst {
      case s if s.startsWith(key) =>
        outputFile = s.drop(key.length).trim
    }

    if (outputFile.isEmpty) {
      error(s"invalid args. please set `${key}`")
      false
    } else {
      true
    }
  }

  private def asResult(counter: TrieMap[String, LongAdder]): List[Result] =
    counter.iterator.map { case (k, v) => Result(name = k, count = v.sum()) }.toList.sortBy(x => (x.count, x.name))

  override def writeAdditionalOutputs(writer: OutputFileWriter): Unit = {
    val result = implicitly[EncodeJson[List[Values]]].encode(
      List(
        Values(
          `type` = "shapeless.Generic",
          values = asResult(counter.generic)
        ),
        Values(
          `type` = "shapeless.LabelledGeneric",
          values = asResult(counter.labelled)
        ),
      )
    )
    Files.write(
      new File(outputFile).toPath,
      result.toString.getBytes(StandardCharsets.UTF_8)
    )
  }

  override val components: List[PluginComponent] = List[PluginComponent](
    new ShapelessGenericCounterTraverser(global, counter)
  )
}

class ShapelessGenericCounterTraverser(override val global: Global, counter: Counter) extends PluginComponent {
  import global.*

  override val runsAfter: List[String] = List("typer")

  override val phaseName = "shapeless-generic-counter"

  object MyTraverser extends Traverser {
    override def traverse(tree: Tree): Unit = {
      tree match {
        case TypeApply(Select(g: Ident, TermName("instance")), List(t, _))
            if g.tpe.typeSymbol.fullName == "shapeless.Generic" =>
          counter.generic.getOrElseUpdate(t.tpe.typeSymbol.fullName, new LongAdder()).increment()
        case TypeApply(Select(g, TermName("materializeProduct" | "materializeCoproduct")), List(t, _, _, _))
            if g.tpe.typeSymbol.fullName == "shapeless.LabelledGeneric" =>
          counter.labelled.getOrElseUpdate(t.tpe.typeSymbol.fullName, new LongAdder()).increment()
        case _ =>
      }
      super.traverse(tree)
    }
  }

  override def newPhase(prev: Phase): StdPhase =
    new StdPhase(prev) {
      override def apply(unit: CompilationUnit): Unit =
        MyTraverser.traverse(unit.body)
    }
}
