name := """frontend"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  anorm,
  cache,
  ws,
  "com.dropbox.core" % "dropbox-core-sdk" % "1.7.6",
  "commons-io" % "commons-io" % "2.4",
  "commons-codec" % "commons-codec" % "1.9"
)
