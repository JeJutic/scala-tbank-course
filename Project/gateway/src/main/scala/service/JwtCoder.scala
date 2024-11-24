package service

import cats.Monad
import cats.effect.kernel.Sync
import cats.implicits.*
import domain.User.UserName
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import scala.util.Try

trait JwtCoder[F[_]: Monad] {

  def encode(username: UserName): F[String]

  def decode(token: String): F[Try[JwtClaim]]

}

object JwtCoder {

  def make[F[_]: Sync](
    now: Now[F],
    secretKey: String
  ): JwtCoder[F] = new JwtCoder[F] {
    override def encode(username: UserName): F[String] =
      for {
        clock    <- now.getClock
        issuedAt <- Monad[F].pure(clock.millis()) // an effect
        issuedAtSeconds = issuedAt / 1000
        claim = JwtClaim()
          .about(username) // subject field
          .issuedAt(issuedAtSeconds)
          .expiresAt(issuedAtSeconds + 60)
      } yield Jwt.encode(claim, secretKey, JwtAlgorithm.HS512)

    override def decode(token: String): F[Try[JwtClaim]] =
      now.getClock.flatMap { clock =>
        Sync[F].delay( // uses clock inside => has a side effect
          Jwt(clock)
            .decode(token, secretKey, Seq(JwtAlgorithm.HS512))
        )
      }
  }

}
