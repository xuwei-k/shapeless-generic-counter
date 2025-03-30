import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._

val commonSettings = Def.settings(
  publishTo := sonatypePublishToBundle.value,
  Compile / unmanagedResources += (LocalRootProject / baseDirectory).value / "LICENSE.txt",
  Compile / packageSrc / mappings ++= (Compile / managedSources).value.map { f =>
    (f, f.relativeTo((Compile / sourceManaged).value).get.getPath)
  },
  Compile / doc / scalacOptions ++= {
    val hash = sys.process.Process("git rev-parse HEAD").lineStream_!.head
    if (scalaBinaryVersion.value == "3") {
      Nil // TODO
    } else {
      Seq(
        "-sourcepath",
        (LocalRootProject / baseDirectory).value.getAbsolutePath,
        "-doc-source-url",
        s"https://github.com/xuwei-k/shapeless-generic-counter/blob/${hash}€{FILE_PATH}.scala"
      )
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "3" =>
        Nil
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case "2.12" =>
        Seq("-Xsource:3")
    }
  },
  scalacOptions ++= Seq(
    "-deprecation",
  ),
  pomExtra := (
    <developers>
    <developer>
      <id>xuwei-k</id>
      <name>Kenji Yoshida</name>
      <url>https://github.com/xuwei-k</url>
    </developer>
  </developers>
  <scm>
    <url>git@github.com:xuwei-k/shapeless-generic-counter.git</url>
    <connection>scm:git:git@github.com:xuwei-k/shapeless-generic-counter.git</connection>
  </scm>
  ),
  organization := "com.github.xuwei-k",
  homepage := Some(url("https://github.com/xuwei-k/shapeless-generic-counter")),
  licenses := List(
    "MIT License" -> url("https://opensource.org/licenses/mit-license")
  ),
)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  releaseStepCommandAndRemaining("set useSuperShell := false"),
  releaseStepCommandAndRemaining("publishSigned"),
  releaseStepCommandAndRemaining("sonatypeBundleRelease"),
  releaseStepCommandAndRemaining("set useSuperShell := true"),
  setNextVersion,
  commitNextVersion,
  pushChanges
)

commonSettings

publish / skip := true

lazy val sbtPlugin = projectMatrix
  .in(file("sbt-plugin"))
  .enablePlugins(SbtPlugin)
  .defaultAxes()
  .jvmPlatform(scalaVersions = Seq("2.12.20", "3.6.4"))
  .settings(
    commonSettings,
    description := "count shapeless.Generic instance",
    sbtTestDirectory := (LocalRootProject / baseDirectory).value / "test",
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          (pluginCrossBuild / sbtVersion).value
        case _ =>
          "2.0.0-M4"
      }
    },
    Compile / sourceGenerators += task {
      val dir = (Compile / sourceManaged).value
      val className = "ShapelessGenericCounterBuildInfo"
      val f = dir / "shapeless_generic_counter" / s"${className}.scala"
      IO.write(
        f,
        Seq(
          "package shapeless_generic_counter",
          "",
          s"private[shapeless_generic_counter] object $className {",
          s"""  def version: String = "${version.value}" """,
          "}",
        ).mkString("", "\n", "\n")
      )
      Seq(f)
    },
    scriptedLaunchOpts += "-Dplugin.version=" + version.value,
    scriptedBufferLog := false,
    name := "sbt-shapeless-generic-counter",
  )

lazy val compilerPlugin = projectMatrix
  .in(file("compiler-plugin"))
  .defaultAxes()
  .jvmPlatform(scalaVersions = Seq("2.12.20", "2.13.16"))
  .settings(
    commonSettings,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    name := "shapeless-generic-counter",
    description := "count shapeless.Generic deriving count",
  )

ThisBuild / scalafixDependencies += "com.github.xuwei-k" %% "scalafix-rules" % "0.6.4"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision
