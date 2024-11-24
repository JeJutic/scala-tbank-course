package http

import cats.Monad
import cats.syntax.all.*
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import service.UserService

class LoginRoutes[F[_]: Monad](userService: UserService[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "login" =>
    userService
      .logIn("admin", "admin")
      .flatMap(
        _.fold(
          Forbidden(_),
          Ok(_)
        )
      )
  }

}
