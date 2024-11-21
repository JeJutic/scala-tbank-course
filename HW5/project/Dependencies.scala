import sbt.*

object Dependencies {
  lazy val catsCore = "org.typelevel" %% "cats-core" % "2.12.0"
  lazy val scalaTest = "org.scalatest" %% "scalatest" % "3.2.15" % Test
  lazy val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test
}
