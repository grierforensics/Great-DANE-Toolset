name := "dst-core"

organization := "com.grierforensics"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "com.grierforensics" %% "dst-bc" % "0.1.0-SNAPSHOT",
  "javax.mail" % "mail" % "1.4.7",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0"
)

libraryDependencies ++= Seq(// test
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

