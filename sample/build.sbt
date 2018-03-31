name := "push sample application"
version := "1.2.1"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val push_sampleapp = (project in file("."))
  .enablePlugins(PlayJava)
  .settings(buildSettings: _*)

resolvers ++= Seq(
  "Splendid Bits" at "http://dl.bintray.com/splendidbits/play-pushservices",
  "Bintray jCenter" at "http://jcenter.bintray.com"
)

libraryDependencies ++= Seq(
  javaCore,
  javaJpa,
  javaWs,
  guice,
  "com.splendidbits" % "play-pushservices" % "1.2.1",
  "org.postgresql" % "postgresql" % "42.1.1",
  "org.jetbrains" % "annotations" % "13.0",
  "junit" % "junit" % "4.12" % Test
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// If set to True, Play will run in a different JVM than SBT
// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
