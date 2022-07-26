import sbt.url

lazy val root = (project in file(".")).settings(assemblySettings)

lazy val V = new {
  val jackson = "2.13.3"
  val json = "20220320"
  val cosmos = "4.33.0"
  val test = "3.2.12"
  val reactor = "0.8.0"
  val zio = "2.0.0"
  val pureconfig = "0.17.1"
  val logger = "3.9.4"
  val slf4j = "1.7.36"
}

name := "cosmicWorkScala"
scalaVersion := "2.13.8"
version := "0.0.1-SNAPSHOT"
organization := "com.elastacloud"
description := "An example with cosmicWorks data for model cosmosbd with scala"
homepage := Some(url("https://www.elastacloud.com"))
developers ++= List(
  Developer(
    id = "macwinux",
    name = "Carlos Martinez",
    email = "carlos.martinez@elastacloud.com",
    url = url("https://github.com/macwinux")
  )
)

ThisBuild / libraryDependencies ++= Seq(
  "com.azure" % "azure-cosmos" % V.cosmos,
  "org.json" % "json" % V.json,
  "com.github.pureconfig" %% "pureconfig" % V.pureconfig,
  "dev.zio" %% "zio" % V.zio,
  "io.projectreactor" %% "reactor-scala-extensions" % V.reactor,
  "com.typesafe.scala-logging" %% "scala-logging" % V.logger,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % V.jackson,
  "com.fasterxml.jackson.core" % "jackson-databind" % V.jackson,
  "org.slf4j" % "slf4j-jdk14" % V.slf4j
)

// Test settings
ThisBuild / libraryDependencies ++= Seq(
  "org.scalactic" %% "scalactic" % V.test % Test,
  "org.scalatest" %% "scalatest" % V.test % Test
)

val assemblySettings = Seq(
  assembly / assemblyJarName := s"${name.value}_${scalaBinaryVersion.value}_${version.value}_uber.jar",
  assembly / assemblyExcludedJars := {
    val cp = (assembly / fullClasspath).value
    cp filter { f =>
      f.data.getName.contains("netty-transport-native-epoll") ||
      f.data.getName.contains("scalactic") ||
      f.data.getName.contains("scalatest")
    }
  },
  assembly / assemblyMergeStrategy := {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x                             => MergeStrategy.first
  }
)
