name := "dst-core"

organization := "com.grierforensics"

version := "0.8"

scalaVersion := "2.11.4"

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcmail-jdk15on" % "1.53",
  "org.bouncycastle" % "bcpg-jdk15on" % "1.53",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.53",
  "org.bouncycastle" % "bcprov-ext-jdk15on" % "1.53",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.53",
  "javax.mail" % "mail" % "1.4.7",
  "com.typesafe" % "config" % "1.2.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "commons-io" % "commons-io" % "2.4",
  "org.slf4j" % "slf4j-api" % "1.7.10",
  "org.slf4j" % "slf4j-log4j12" % "1.7.10",
  "com.owlike" % "genson-scala" % "1.2"
)

libraryDependencies ++= Seq(// test
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

