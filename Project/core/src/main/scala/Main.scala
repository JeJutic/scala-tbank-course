import cats.effect.IO

// Для запуска в sbt shell используйте команду `core / run`
object Main {

  def main(args: Array[String]): Unit =
    println(IO.delay(println("Just showing that cats-effect is available")))

}
