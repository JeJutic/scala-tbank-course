package unmarshal.decoder

import cats.data.{EitherT, State}
import unmarshal.error.DecoderError
import unmarshal.error.DecoderError.addPath
import unmarshal.model.Json
import unmarshal.model.Json.{JsonArray, JsonBool, JsonNull, JsonNum, JsonObject, JsonString}

trait Decoder[A] {
  def fromJson(json: Json): Either[DecoderError, A]
}

object Decoder {

  def apply[A](using
    decoder: Decoder[A]
  ): Decoder[A] = decoder

  private def nestedImplementation[A](
    typeName: String,
    partialMatch: PartialFunction[Json, Either[DecoderError, A]]
  ): Decoder[A] = json =>
    partialMatch
      .lift(json)
      .toRight(DecoderError.wrongJson(s"expected $typeName, got $json"))
      .flatten

  private def primitiveImplementation[A](
    typeName: String,
    partialMatch: PartialFunction[Json, A]
  ): Decoder[A] =
    nestedImplementation(typeName = typeName, partialMatch = partialMatch.andThen(Right(_)))

  given Decoder[String] = primitiveImplementation("string", { case JsonString(value) => value })

  given Decoder[Long] = primitiveImplementation("number", { case JsonNum(value) => value })

  given Decoder[Boolean] = primitiveImplementation("boolean", { case JsonBool(value) => value })

  given [T: Decoder]: Decoder[List[T]] = nestedImplementation(
    "array",
    { case JsonArray(array) =>
      val (lefts, rights) =
        array
          .map(element => Decoder[T].fromJson(element))
          .zipWithIndex
          .map {
            case (Left(error), index) => Left((error, index))
            case (Right(t), _)        => Right(t) // another type of Either
          }
          .partitionMap(identity)
      lefts.headOption
        .map { case (error, index) =>
          addPath(error, index.toString)
        }
        .toLeft(rights)
    }
  )

  private type ParsingState[A] = State[Map[String, Json], A]

  private def getOrNotPresent[A](
    field: String,
    f: Either[DecoderError, Json] => Either[DecoderError, A]
  ): EitherT[ParsingState, DecoderError, A] = EitherT(
    State { map =>
      (
        map - field,
        f(
          map
            .get(field)
            .toRight(DecoderError.wrongJson(s"field not present"))
        ).left.map(addPath(_, field))
      )
    }
  )

  def getField[A: Decoder](field: String): EitherT[ParsingState, DecoderError, A] =
    getOrNotPresent(field, _.flatMap(json => Decoder[A].fromJson(json)))

  def getOptionField[A: Decoder](field: String): EitherT[ParsingState, DecoderError, Option[A]] =
    getOrNotPresent(
      field,
      _.flatMap {
        case JsonNull => Right(None)
        case json     => Decoder[A].fromJson(json).map(Some(_))
      }
    )

  // all these complexity with EitherT and State was only to ensure no values left in the map
  def objectDecoder[A](decodeApi: EitherT[ParsingState, DecoderError, A]): Decoder[A] =
    nestedImplementation(
      "object",
      { case JsonObject(map) =>
        val eval = decodeApi.value.run(map).value
        // important that priority of lefts preserved
        eval._2.map { res =>
          eval._1.headOption
            .map(field => DecoderError.wrongJson("extra field", field._1))
            .toLeft(res)
        }.flatten
      }
    )

  // off-topic: accept me on the internship pls
}
