import cats.effect.kernel.Async
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import com.comcast.ip4s.{ipv4, port}
import config.ApplicationConfig
import http.secured.WelcomeRoutes
import http.{AuthMiddlewareFactory, LoginRoutes}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.{HttpRoutes, Response}
import service.{JwtCoder, Now, UserService}

object Main extends IOApp {

  private def application[F[_]: Async]: F[Unit] =
    ApplicationConfig.unsafeLoad().flatMap { _ =>
      val now         = Now.make[F]()
      val jwtCoder    = JwtCoder.make[F](now, "secretKey") // TODO cfg
      val userService = UserService.make[F](jwtCoder)
      val authMiddleware = AuthMiddlewareFactory[F](
        jwtCoder,
        userService
      ).authMiddleware
      val loginRoutes           = LoginRoutes[F](userService).routes
      val welcomeRoutes         = WelcomeRoutes[F](authMiddleware).routes
      val routes: HttpRoutes[F] = loginRoutes <+> welcomeRoutes
      val app                   = routes.orNotFound
      val server = EmberServerBuilder
        .default[F]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8081")
        .withHttpApp(app)
        .build
      server
        .use(_ => Async[F].never)
        .void
    }

  override def run(args: List[String]): cats.effect.IO[ExitCode] =
    application[IO].attempt
      .flatMap {
        case Left(th) => IO.println(s"Exit with error:${th.getMessage}").as(ExitCode.Error)
        case Right(_) => IO(ExitCode.Success)
      }

}
