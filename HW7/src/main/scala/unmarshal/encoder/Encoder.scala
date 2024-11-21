package unmarshal.encoder

import cats.syntax.semigroup.catsSyntaxSemigroup
import unmarshal.model.Json
import unmarshal.model.Json.{
  JsonArray,
  JsonBool,
  JsonDouble,
  JsonNull,
  JsonNum,
  JsonObject,
  JsonString
}

import scala.compiletime.constValueTuple
import scala.Tuple.Zip
import scala.deriving.Mirror

trait Encoder[A] {
  def toJson(value: A): Json
}

object Encoder {

  def apply[A](using
    encoder: Encoder[A]
  ): Encoder[A] = encoder

  given Encoder[String]                = JsonString(_)
  given Encoder[Long]                  = JsonNum(_)
  given Encoder[Double]                = JsonDouble(_)
  given Encoder[Boolean]               = JsonBool(_)
  given [A: Encoder]: Encoder[List[A]] = list => JsonArray(list.map(Encoder[A].toJson))

  given [A: Encoder]: Encoder[Option[A]] = {
    case Some(value) => Encoder[A].toJson(value)
    case None        => JsonNull
  }

  opaque type EncoderToObject[A] = A => JsonObject

  object EncoderToObject {

    def apply[A](using
      encoder: EncoderToObject[A]
    ): EncoderToObject[A] = encoder

    given EncoderToObject[EmptyTuple] = _ => JsonObject(Map.empty)

    given [A: Encoder, S <: String]: EncoderToObject[(S, A)] = (fieldName, value) =>
      JsonObject(Map((fieldName, Encoder[A].toJson(value))))

    given [H: EncoderToObject, T <: Tuple: EncoderToObject]: EncoderToObject[H *: T] =
      tuple =>
        EncoderToObject[H](tuple.head) |+|
          EncoderToObject[T](tuple.tail)

  }

  inline def autoDerive[A <: Product](using
    m: Mirror.ProductOf[A],
    encoder: EncoderToObject[Zip[m.MirroredElemLabels, m.MirroredElemTypes]]
  ): Encoder[A] = t => encoder(constValueTuple[m.MirroredElemLabels].zip(Tuple.fromProductTyped(t)))

}
