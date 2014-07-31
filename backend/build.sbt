organization := "com.xenosync"

version := "0.1"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "spray repo" at "http://repo.spray.io"

libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.10" % "2.3.0",
  "com.typesafe.akka" % "akka-slf4j_2.10" % "2.3.0",
  "io.spray" % "spray-can" % "1.3.1",
  "io.spray" % "spray-routing" % "1.3.1",
  "io.spray" % "spray-json_2.10" % "1.2.6",
  "ch.qos.logback" % "logback-classic" % "1.1.2"
)

Revolver.settings