package domain

import domain.User.UserName

case class User(name: UserName)

object User {
  type UserName = String // opaque?
}
