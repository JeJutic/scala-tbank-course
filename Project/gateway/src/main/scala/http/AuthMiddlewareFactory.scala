package http

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT}
import cats.syntax.all.*
import domain.User
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, Request}
import service.{JwtCoder, UserService}

class AuthMiddlewareFactory[F[_]: Monad](
  jwtCoder: JwtCoder[F],
  userService: UserService[F]
) extends Http4sDsl[F] {

  val authMiddleware: AuthMiddleware[F, User] = {
    val authUserHeader: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
      (for {
        headers <- EitherT.fromEither(
          request.headers
            .get(Authorization.name)
            .toRight("Couldn't find an Authorization header")
        ) // FIXME typed errors?
        token <- EitherT(
          jwtCoder
            .decode(headers.head.value)
            .map(_.toEither.left.map(_.getMessage))
        )
        username <- EitherT.fromEither(
          token.subject
            .toRight("JWT token format is invalid")
        )
        user <- EitherT(
          userService
            .retrieve(username)
            .map(_.toRight("User not found"))
        )
      } yield user).value
    }

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    AuthMiddleware(authUserHeader, onFailure)
  }

}
