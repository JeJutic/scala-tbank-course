package unmarshal.error

final case class DecoderError protected (
  message: String,
  field: String
)

object DecoderError {

  private val FieldUndefined = "*"

  def wrongJson(reason: String, field: String): DecoderError =
    new DecoderError(
      message = s"Illegal json at '$field': $reason",
      field = field
    )

  def wrongJson(reason: String): DecoderError =
    wrongJson(s"Illegal json: $reason", FieldUndefined)

  def addPath(original: DecoderError, field: String): DecoderError =
    original.copy(field =
      field + (if (original.field != FieldUndefined) "." + original.field else "")
    )

}
