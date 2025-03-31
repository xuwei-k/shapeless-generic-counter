package shapeless_generic_counter

import sbt.Keys.*
import sbt.{*, given}
import scala.collection.compat.*
import sjsonnew.BasicJsonProtocol.*
import sjsonnew.JsonFormat

object SbtShapelessGenericCounter extends AutoPlugin {
  object autoImport {
    val shapelessGenericCounterOutput = settingKey[File]("")
    val shapelessGenericCounterAggregate = taskKey[ResultValue]("")
    val shapelessGenericCounterAggregateConfigurations = settingKey[Seq[Configuration]]("")
  }
  import autoImport.*

  override def trigger: PluginTrigger = allRequirements

  private type ResultValue = List[Values]

  private def defaultConfigurations = Seq(Compile, Test)

  private object AsInt {
    def unapply(s: String): Option[Int] = s.toIntOption
  }

  private object EnableScalaBinaryVersion {
    def unapply(v: String): Boolean = Set("2.12", "2.13").contains(v)
  }

  private implicit val instance: JsonFormat[Values] = {
    import sjsonnew.BasicJsonProtocol.*

    implicit val result: JsonFormat[shapeless_generic_counter.Result] =
      caseClass2(shapeless_generic_counter.Result.apply, shapeless_generic_counter.Result.unapply)(
        "name",
        "count",
      )

    caseClass2(Values.apply, Values.unapply)(
      "type",
      "values",
    )
  }

  override def buildSettings: Seq[Def.Setting[?]] = Def.settings(
    shapelessGenericCounterAggregateConfigurations := defaultConfigurations,
    LocalRootProject / shapelessGenericCounterAggregate / shapelessGenericCounterOutput := {
      (LocalRootProject / target).value / "shapeless-generic-count-aggregate.json"
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
            val json = sjsonnew.support.scalajson.unsafe.Parser.parseFromFile(f).get
            sjsonnew.support.scalajson.unsafe.Converter.fromJsonUnsafe[ResultValue](json)
          }
          .groupBy(_.`type`)
          .map { case (k, v) =>
            Values(
              k,
              v.flatMap(_.values)
                .groupBy(_.name)
                .map { case (x, y) =>
                  shapeless_generic_counter.Result(x, y.map(_.count).sum)
                }
                .toList
                .sortBy(x => (x.count, x.name))
                .reverse
            )
          }
          .toList
          .sortBy(_.`type`)
          .reverse

      val f = (LocalRootProject / shapelessGenericCounterAggregate / shapelessGenericCounterOutput).value
      val json = sjsonnew.support.scalajson.unsafe.Converter.toJsonUnsafe(result)
      val jsonString = sjsonnew.support.scalajson.unsafe.PrettyPrinter(json)
      IO.write(f, jsonString)
      result
    }
  )

  def shapelessGenericCounterSetting(c: Configuration): SettingsDefinition =
    Def.settings(
      c / shapelessGenericCounterOutput := {
        crossTarget.value / s"shapeless-generic-count-${Defaults.nameForSrc(c.name)}.json"
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
