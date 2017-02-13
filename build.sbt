// Copyright (C) 2017 Grier Forensics. All Rights Reserved.

name := "dst"

organization := "com.grierforensics"

version := "1.0"

scalaVersion := "2.11.8"

resolvers += Resolver.sonatypeRepo("releases")


lazy val root = (project in file(".")) aggregate(core, web)

lazy val core = (project in file("dst-core"))

lazy val web = (project in file("dst-web"))
  .dependsOn(core % "compile->compile;test->test")

