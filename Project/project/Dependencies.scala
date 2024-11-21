import sbt.*

object Dependencies {

  val catsCore   = "org.typelevel" %% "cats-core"   % "2.12.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"

  val zioHttp = "dev.zio" %% "zio-http" % "3.0.1"

  val jwtCore = "com.github.jwt-scala" %% "jwt-core" % "10.0.1"

  val http4sVersion = "0.23.29"
  val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-ember-client" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl"          % http4sVersion,
  )

}
