import Dependencies.*
import sbt.Keys.fork

ThisBuild / scalaVersion := "3.3.4"
ThisBuild / version      := "0.1.0-SNAPSHOT"

Compile / compile / scalacOptions ++= Seq(
  "-Werror",
  "-Wunused:all",
  "-Wvalue-discard",
  "-unchecked",
  "-deprecation"
)

lazy val root: Project = project
  .in(file("."))
  .settings(
    name := "schedule-generator"
  )
  .aggregate(
    core,
    gateway
  )

lazy val core = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      catsEffect
    )
  )

lazy val gateway = project
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      catsCore,
      catsEffect,
      jwtCore
    ) ++ http4s
  )

lazy val commonSettings = Seq(
  Test / fork := true
)
