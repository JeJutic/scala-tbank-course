package ventilation

import cats.data.State

object StateUtils {
  def fixed[S, T](f: S => T): State[S, T] = State { s => (s, f(s)) }
  def transform[S](f: S => S): State[S, Unit] = State { s => (f(s), ()) }

  def sequence[S, T](xs: List[State[S, T]]): State[S, List[T]] =
    xs.foldLeft(State.pure[S, List[T]](List.empty[T])) { (acc, x) =>
      for {
        xs <- acc
        xx <- x
      } yield xx :: xs
    }.map(_.reverse)
}
