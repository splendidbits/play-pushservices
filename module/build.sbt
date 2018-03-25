name := "play-pushservices"
organization := "com.splendidbits"
organizationName := "Splendid Bits"
organizationHomepage := Some(new URL("https://splendidbits.co"))
version := "1.1.2"

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8"
)

lazy val pushservices = (project in file("."))
  .enablePlugins(PlayJava, PlayEbean, PlayEnhancer)
  .settings(buildSettings: _*)

resolvers += Resolver.jcenterRepo
libraryDependencies ++= Seq(
  javaCore,
  javaJpa,
  javaWs,
  guice,
  "commons-io" % "commons-io" % "2.5",
  "com.google.code.gson" % "gson" % "2.8.2",
  "junit" % "junit" % "4.12" % Test
)

playEbeanDebugLevel := 4
playEbeanModels in Compile := Seq(
  "models.pushservices.db.*"
)

// Bintray publishing
// run 'sbt bintrayChangeCredentials' first!
crossPaths := false
pomIncludeRepository := (_ => true)
publishArtifact in Test := false
publishMavenStyle := true

description in bintray := "A Play Framework module for idiot-proof push notification delivery."
bintrayPackageLabels := Seq("play", "play framework", "play-framework", "push notifications", "gcm", "apns")
bintrayRepository := "play-pushservices"
bintrayPackage := "pushservices"
bintrayReleaseOnPublish := false
bintrayVcsUrl := Some("http://github.com/splendidbits/play-pushservices")
licenses += ("GPL-3.0", url("http://opensource.org/licenses/GPL-3.0"))

bintrayPackageAttributes ~= (_ ++ Map(
    "website_url" -> Seq(bintry.Attr.String("https://splendidbits.co")),
    "github_repo" -> Seq(bintry.Attr.String("https://github.com/splendidbits/play-pushservices")),
    "issue_tracker_url" -> Seq(bintry.Attr.String("https://github.com/splendidbits/play-pushservices/issues"))
  )
)

fork in run := false