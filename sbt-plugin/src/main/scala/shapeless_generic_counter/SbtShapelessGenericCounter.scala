package shapeless_generic_counter

import sbt.Keys.*
import sbt.{*, given}
import scala.collection.compat.*

object SbtShapelessGenericCounter extends AutoPlugin {
  object autoImport {
    val shapelessGenericCounterOutput = settingKey[File]("")
    val shapelessGenericCounterAggregate = taskKey[ResultValue]("")
    val shapelessGenericCounterAggregateConfigurations = settingKey[Seq[Configuration]]("")
  }
  import autoImport.*

  override def trigger: PluginTrigger = allRequirements

  private type ResultValue = List[(String, Int)]

  private def defaultConfigurations = Seq(Compile, Test)

  private object AsInt {
    def unapply(s: String): Option[Int] = s.toIntOption
  }

  private object EnableScalaBinaryVersion {
    def unapply(v: String): Boolean = Set("2.12", "2.13").contains(v)
  }

  override def buildSettings: Seq[Def.Setting[?]] = Def.settings(
    shapelessGenericCounterAggregateConfigurations := defaultConfigurations,
    LocalRootProject / shapelessGenericCounterAggregate / shapelessGenericCounterOutput := {
      (LocalRootProject / target).value / "shapeless-generic-count-aggregate.txt"
    },
    LocalRootProject / shapelessGenericCounterAggregate := {
      val extracted = Project.extract(state.value)
      val currentBuildUri = extracted.currentRef.build
      val result =
        extracted.structure.units
          .apply(currentBuildUri)
          .defined
          .values
          .flatMap { p =>
            val proj = LocalProject(p.id)
            for {
              c <- extracted.getOpt(proj / shapelessGenericCounterAggregateConfigurations).toList.flatten
              out <- extracted.getOpt(proj / c / shapelessGenericCounterOutput).toList
            } yield out
          }
          .filter(_.isFile)
          .flatMap { f =>
            IO.readLines(f)
              .iterator
              .map(_.trim)
              .filter(_.nonEmpty)
              .map(_.split(' '))
              .map { case Array(a, AsInt(b)) =>
                a -> b
              }
              .toList
          }
          .groupBy(_._1)
          .map { case (k, v) => k -> v.map(_._2).sum }
          .toList
          .sortBy(_.swap)
          .reverse

      val f = (LocalRootProject / shapelessGenericCounterAggregate / shapelessGenericCounterOutput).value

      IO.writeLines(f, result.map { case (a, b) => s"$a $b" })
      result
    }
  )

  def shapelessGenericCounterSetting(c: Configuration): SettingsDefinition =
    Def.settings(
      c / shapelessGenericCounterOutput := {
        crossTarget.value / s"shapeless-generic-count-${Defaults.nameForSrc(c.name)}.txt"
      },
      c / compile / scalacOptions ++= PartialFunction
        .condOpt(scalaBinaryVersion.value) { case EnableScalaBinaryVersion() =>
          val prefix = "-P:shapeless-generic-counter:"
          val x = (c / shapelessGenericCounterOutput).value
          val out: String =
            (LocalRootProject / baseDirectory).value.relativize(x).fold(x.getAbsolutePath)(_.toString)

          s"${prefix}output:${out}"
        }
        .toSeq
    )

  override def projectSettings: Seq[Def.Setting[?]] = Def.settings(
    libraryDependencies ++= PartialFunction
      .condOpt(scalaBinaryVersion.value) { case EnableScalaBinaryVersion() =>
        compilerPlugin(
          "com.github.xuwei-k" %% "shapeless-generic-counter" % ShapelessGenericCounterBuildInfo.version
        )
      }
      .toSeq
  ) ++ defaultConfigurations.flatMap(shapelessGenericCounterSetting)
}
