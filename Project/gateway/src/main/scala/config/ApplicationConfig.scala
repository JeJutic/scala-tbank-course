package config

import cats.effect.kernel.Sync
import pureconfig.generic.ProductHint
import pureconfig.{ConfigReader, ConfigSource}

import scala.annotation.unused

final case class ApplicationConfig(
  host: String,
  port: Int
) derives ConfigReader

object ApplicationConfig {

  @unused private implicit def hint[T]: ProductHint[T] = ProductHint[T](allowUnknownKeys = false)

  def unsafeLoad[F[_]: Sync](
    config: ConfigSource = ConfigSource.default
  ): F[ApplicationConfig] =
    Sync[F].delay(config.loadOrThrow[ApplicationConfig]())

}
