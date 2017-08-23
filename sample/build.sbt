import sbt.Keys.organizationHomepage

name := "play-pushservices-sample"
organization := "com.splendidbits"
organizationHomepage := Some(new URL("https://splendidbits.co"))
version := "1.0.2"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val sampleapp = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(buildSettings: _*)

resolvers += (
  "Splendid Bits repository" at "http://dl.bintray.com/splendidbits/play-pushservices"
)

libraryDependencies ++= Seq(
  javaCore,
  javaJdbc,
  javaJpa,
  javaWs,
  guice,
  "com.splendidbits" % "play-pushservices" % "1.0.2",
  "org.avaje" % "avaje-agentloader" % "2.1.2",
  "org.postgresql" % "postgresql" % "42.1.1"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// If set to True, Play will run in a different JVM than SBT
// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
