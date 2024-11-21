package data

import unmarshal.decoder.Decoder
import unmarshal.decoder.Decoder.{getField, getOptionField, objectDecoder}
import unmarshal.encoder.Encoder

case class Employee(
  name: String,
  age: Long,
  id: Long,
  bossId: Option[Long]
)

object Employee {

  given employeeEncoder: Encoder[Employee] = Encoder.autoDerive[Employee]

  given employeeDecoder: Decoder[Employee] = objectDecoder {
    for {
      name   <- getField[String]("name")
      age    <- getField[Long]("age")
      id     <- getField[Long]("id")
      bossId <- getOptionField[Long]("bossId")
    } yield Employee(name, age, id, bossId)
  }

}
