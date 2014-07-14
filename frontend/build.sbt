name := "DropboxAcq"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  "com.typesafe.play" % "play-slick_2.10" % "0.6.0.1",
  "com.dropbox.core" % "dropbox-core-sdk" % "1.7.6",
  "postgresql" % "postgresql" % "9.1-901.jdbc4"
)     

play.Project.playScalaSettings
