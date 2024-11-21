package data

import unmarshal.decoder.Decoder
import unmarshal.decoder.Decoder.{getField, objectDecoder}
import unmarshal.encoder.Encoder

case class CompanyEmployee(
  employees: List[Employee]
)

object CompanyEmployee {

  given companyEmployeeEncoder: Encoder[CompanyEmployee] = Encoder.autoDerive[CompanyEmployee]

  given companyEmployeeDecoder: Decoder[CompanyEmployee] = objectDecoder {
    for {
      employees <- getField[List[Employee]]("employees")
    } yield CompanyEmployee(employees)
  }

}
