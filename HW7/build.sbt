import Dependencies.*

ThisBuild / scalaVersion := "3.4.3"
ThisBuild / version      := "0.1.0-SNAPSHOT"

scalacOptions ++= List(
  "-deprecation",
  "-encoding",
  "utf-8",
  "-explaintypes",
  "-feature",
  "-language:implicitConversions",
  "-unchecked",
  "-Ysafe-init",
  "-Werror"
)

lazy val root = (project in file("."))
  .settings(
    name := "hw7",
    libraryDependencies ++= List(
      catsCore,
      catsEffect,
      scalaTest  % Test,
      catsTest   % Test,
      scalaCheck % Test
    ),
    coverageEnabled                 := true,
    coverageFailOnMinimum           := true,
    coverageMinimumStmtTotal        := 70,
    coverageMinimumBranchTotal      := 70,
    coverageMinimumStmtPerPackage   := 70,
    coverageMinimumBranchPerPackage := 65,
    coverageMinimumStmtPerFile      := 65,
    coverageMinimumBranchPerFile    := 60
  )
