import sbt.Keys._

scalaVersion := "2.12.4"
name := """oystr-vault"""
version := "v1.0-SNAPSHOT"
updateOptions := updateOptions.value.withLatestSnapshots(true)

val home = sys.env("HOME")

PlayKeys.devSettings := Seq(
  "play.server.http.port" -> "9005",
  "hashicorp.vault.address" -> "127.0.0.1:8200"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases")
)

val AkkaVersion = "2.5.16"
val AkkaPersistenceCassandra = "0.54"

libraryDependencies ++= Seq(
  guice,
  ws,
  filters,
  "oystr" %% "oystr-scala-commons" % "v1.0-SNAPSHOT",
  "com.jsuereth" %% "scala-arm"  % "2.0",
  "com.chuusai"  %% "shapeless"  % "2.3.3",
  "commons-io"   %  "commons-io" % "2.6",
  "com.softwaremill.sttp" %% "core" % "1.3.0",
  "com.softwaremill.sttp" %% "akka-http-backend" % "1.3.0"
)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(DockerPlugin)
  .disablePlugins(PlayLayoutPlugin)

topLevelDirectory    := None
executableScriptName := "run"
packageName in Universal := "package"
