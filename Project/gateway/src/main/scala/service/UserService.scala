package service

import cats.Monad
import cats.syntax.all.*
import domain.User
import domain.User.UserName

trait UserService[F[_]] {

  def retrieve(username: UserName): F[Option[User]]

  def logIn(username: UserName, password: String): F[Either[String, String]] // FIXME: typed error

}

object UserService {

  def make[F[_]: Monad](jwtCoder: JwtCoder[F]): UserService[F] =
    new UserService[F] { // pure as long as no side effects
      override def retrieve(username: UserName): F[Option[User]] =
        Monad[F].pure(Some(User(username)))

      private def verifyLogin(username: UserName, password: String): F[Either[String, Unit]] =
        Monad[F].pure(Right(())) // TODO

      override def logIn(username: UserName, password: String): F[Either[String, String]] =
        verifyLogin(username, password).flatMap(
          _.left
            .map(identity) // TODO typed error
            .map(_ => jwtCoder.encode(username))
            .sequence
        )
    }

}
