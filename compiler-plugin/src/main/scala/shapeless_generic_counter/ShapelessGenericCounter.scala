package shapeless_generic_counter

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

class ShapelessGenericCounter(override val global: Global) extends Plugin {
  override val name = "shapeless-generic-counter"
  override val description = "count shapeless.Generic.instance call"

  private val counter: TrieMap[String, LongAdder] = TrieMap.empty

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

  override def writeAdditionalOutputs(writer: OutputFileWriter): Unit = {
    val result = counter.iterator.map { case (k, v) => k -> v.sum() }.toList
      .sortBy(_.swap)
      .reverseIterator
      .map { case (k, v) => s"$k $v" }
      .mkString("", "\n", "\n")
    Files.write(
      new File(outputFile).toPath,
      result.getBytes(StandardCharsets.UTF_8)
    )
  }

  override val components: List[PluginComponent] = List[PluginComponent](
    new ShapelessGenericCounterTraverser(global, counter)
  )
}

class ShapelessGenericCounterTraverser(override val global: Global, counter: TrieMap[String, LongAdder])
    extends PluginComponent {
  import global.*

  override val runsAfter: List[String] = List("typer")

  override val phaseName = "shapeless-generic-counter"

  object MyTraverser extends Traverser {
    override def traverse(tree: Tree): Unit = {
      tree match {
        case TypeApply(Select(g: Ident, TermName("instance")), List(t, _))
            if g.tpe.typeSymbol.fullName == "shapeless.Generic" =>
          counter.getOrElseUpdate(t.tpe.typeSymbol.fullName, new LongAdder()).increment()
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
