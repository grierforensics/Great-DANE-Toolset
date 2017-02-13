// Copyright (C) 2017 Grier Forensics. All Rights Reserved.

name := "dst-core"

organization := "com.grierforensics"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.bouncycastle" % "bcprov-jdk15on" % "1.56",
  "org.bouncycastle" % "bcmail-jdk15on" % "1.56",
  "javax.mail" % "mail" % "1.4.7",
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.5.0",
  "commons-io" % "commons-io" % "2.5",
  "org.slf4j" % "slf4j-api" % "1.7.22",
  "org.slf4j" % "slf4j-log4j12" % "1.7.22",
  "com.owlike" % "genson-scala" % "1.2"
)

libraryDependencies ++= Seq(// test
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

