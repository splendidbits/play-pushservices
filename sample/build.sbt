name := "push sample application"
version := "1.2.2"

lazy val buildSettings = Seq(
  scalaVersion := "2.12.6"
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
  "org.scala-lang" % "scala-library" % "2.12.6",
  "com.splendidbits" % "play-pushservices" % "1.2.2",
  "org.postgresql" % "postgresql" % "42.2.5",
  "org.jetbrains" % "annotations" % "16.0.2",
  "junit" % "junit" % "4.12" % Test
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// If set to True, Play will run in a different JVM than SBT
// Turning this to "true" stops debugging. Aways set it to "false" in debug
fork in run := false
