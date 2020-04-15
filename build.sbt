import sbt.Keys._

scalaVersion := "2.12.11"
name := """keepo"""
version := "v1.0-SNAPSHOT"
maintainer := "rafael.silverio.it@gmail.com"

updateOptions := updateOptions.value.withLatestSnapshots(true)

val home = sys.env("HOME")

PlayKeys.devSettings := Seq(
  "play.server.http.port" -> "9005",
  "vault.host" -> "localhost",
  "morbid.host" -> "localhost"
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
  "com.jsuereth" %% "scala-arm"  % "2.0",
  "com.chuusai"  %% "shapeless"  % "2.3.3",
  "commons-io"   %  "commons-io" % "2.6",
  "commons-lang" % "commons-lang" % "2.6",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
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
