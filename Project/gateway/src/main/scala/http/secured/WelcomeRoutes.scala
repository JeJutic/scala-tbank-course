package http.secured

import cats.Monad
import domain.User
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpRoutes}

class WelcomeRoutes[F[_]: Monad](authMiddleware: AuthMiddleware[F, User]) extends Http4sDsl[F] {

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of { case GET -> Root / "welcome" as user => Ok(s"Welcome, ${user.name}") }

  val routes: HttpRoutes[F] = authMiddleware(httpRoutes)

}
