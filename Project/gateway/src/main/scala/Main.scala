import cats.data.{Kleisli, OptionT}
import cats.effect.kernel.Clock
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import com.comcast.ip4s.{ipv4, port}
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes, Request, Response}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

object Main extends IOApp {

// Secret Authentication key
  private val SecretKey = "secretKey" // FIXME

  // Helper to encode the JWT token
  private def jwtEncode(username: UserName, issuedAtSeconds: Long): String = {
    val claim = JwtClaim {
      username
    }.issuedAt(issuedAtSeconds)
      .expiresAt(issuedAtSeconds + 60)
    Jwt.encode(claim, SecretKey, JwtAlgorithm.HS512)
  }

  // Helper to decode the JWT token
  private def jwtDecode(token: String): Option[JwtClaim] =
    Jwt.decode(token, SecretKey, Seq(JwtAlgorithm.HS512)).toOption

  private type UserName = String   // opaque?

  private case class User(name: UserName) // enum Student, Admin?

  // gotta figure out how to do the form
  private def verifyLogin(request: Request[IO]): IO[Either[String, UserName]] = IO(Right("admin"))  // FIXME

  private val logIn: Kleisli[IO, Request[IO], Response[IO]] = Kleisli({ request =>
    verifyLogin(request: Request[IO]).flatMap(_ match {
      case Left(error) =>
        Forbidden(error)
      case Right(username) =>
        for {
          realTime <- Clock[IO].realTime
          message = jwtEncode(username, realTime.toSeconds)
          response <- Ok(message)
        } yield response
    })
  })

  private def retrieveUser: Kleisli[IO, UserName, User] = Kleisli(username => IO(User(username))) // TODO

  private val authUserHeader: Kleisli[IO, Request[IO], Either[String, User]] = Kleisli({ request =>
    val message = for {
      header <- request.headers
        .get(Authorization.name)
        .toRight("Couldn't find an Authorization header") // FIXME typed errors?
      token <- jwtDecode(header.toString)
        .toRight("Header invalid")
    } yield token.content // UserName
    message.traverse(retrieveUser.run)
  })

  private val onFailure: AuthedRoutes[String, IO] =
    Kleisli(req => OptionT.liftF(Forbidden(req.context)))

  private val authMiddleware = AuthMiddleware(authUserHeader, onFailure)

  private val authedRoutes: AuthedRoutes[User, IO] =
    AuthedRoutes.of { case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}") }

  private val service: HttpRoutes[IO] = {
    Router(
      // **logIn** is essentially a HttpApp[IO] but I can't figure out how to convert easier
      "/login" -> HttpRoutes.of[IO] { case req => logIn.run(req) }
    )
  } <+> authMiddleware(authedRoutes)

  private val app = service.orNotFound

  private val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8081")
    .withHttpApp(app)
    .build

  override def run(args: List[String]): cats.effect.IO[ExitCode] =
    server
      .use(_ => IO.never)
      .as(ExitCode.Success)

}
