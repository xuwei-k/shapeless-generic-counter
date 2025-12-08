val common = Def.settings(
  libraryDependencies += "io.circe" %% "circe-generic" % "0.14.12",
)

val a1 = project.settings(
  common,
  scalaVersion := "2.13.18",
)

val a2 = project.settings(
  common,
  scalaVersion := "2.12.21",
)

val a3 = project.settings(
  scalaVersion := "3.3.7"
)

val a4 = project
  .settings(
    common,
    scalaVersion := "2.13.18"
  )
  .disablePlugins(SbtShapelessGenericCounter)

val root = project
  .in(file("."))
  .settings(
    TaskKey[Unit]("check") := {
      val result: String = IO.read(target.value / "shapeless-generic-count-aggregate.json")
      val expect: String = IO.read(file("expect.json"))
      assert(result == expect, s"${result} != ${expect}")
    }
  )
  .aggregate(
    a1,
    a2,
    a3,
    a4,
  )
