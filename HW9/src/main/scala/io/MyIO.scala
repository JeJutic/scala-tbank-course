package io

import cats.Eval
import cats.data.EitherT
import io.MyIO.{MyTry, MyTryT}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

/** Класс типов, позволяющий комбинировать описания вычислений, которые могут либо успешно
  * завершиться с некоторым значением, либо завершиться неуспешно, выбросив исключение Throwable.
  * @tparam F
  *   \- тип вычисления
  */
trait Computation[F[_]] {

  def map[A, B](fa: F[A])(f: A => B): F[B]
  def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  def tailRecM[A, B](a: A)(f: A => F[Either[A, B]]): F[B]
  def pure[A](a: A): F[A]
  def *>[A, B](fa: F[A])(another: F[B]): F[B]
  def as[A, B](fa: F[A])(newValue: => B): F[B]
  def void[A](fa: F[A]): F[Unit]
  def attempt[A](fa: F[A]): F[Either[Throwable, A]]
  def option[A](fa: F[A]): F[Option[A]]

  /** Если вычисление fa выбрасывает ошибку, то обрабатывает ее функцией f, без изменения типа
    * выходного значения.
    * @return
    *   результат вычисления fa или результат функции f
    */
  def handleErrorWith[A, AA >: A](fa: F[A])(f: Throwable => F[AA]): F[AA]

  /** Обрабатывает ошибку вычисления чистой функцией recover или преобразует результат вычисления
    * чистой функцией.
    * @return
    *   результат вычисления преобразованный функцией map или результат функции recover
    */
  def redeem[A, B](fa: F[A])(recover: Throwable => B, map: A => B): F[B]
  def redeemWith[A, B](fa: F[A])(recover: Throwable => F[B], bind: A => F[B]): F[B]

  /** Выполняет вычисление. "unsafe", потому что при неуспешном завершении может выбросить
    * исключение.
    * @param fa
    *   \- еще не начавшееся вычисление
    * @tparam A
    *   \- тип результата вычисления
    * @return
    *   результат вычисления, если оно завершится успешно.
    */
  def unsafeRunSync[A](fa: F[A]): A

  /** Оборачивает ошибку в контекст вычисления.
    * @param error
    *   \- ошибка
    * @tparam A
    *   \- тип результата вычисления. Т.к. вычисление сразу завершится ошибкой при выполнении, то
    *   может быть любым.
    * @return
    *   создает описание вычисления, которое сразу же завершается с поданной ошибкой.
    */
  def raiseError[A](error: Throwable): F[A]

}

object Computation {
  def apply[F[_]: Computation]: Computation[F] = implicitly[Computation[F]]
}

final class MyIO[A] private (private val body: MyTryT[A]) {
  self =>

  private def call(): MyTry[A] = body.value.value

  def map[B](f: A => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.map(self)(f)

  def flatMap[B](f: A => MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.flatMap(self)(f)

  def tailRecM[B](f: A => MyIO[Either[A, B]])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    self.flatMap(comp.tailRecM[A, B](_)(f))

  def *>[B](another: MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.*>(self)(another)

  def as[B](newValue: => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.as(self)(newValue)

  def void(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] =
    comp.void(self)

  def attempt(implicit
    comp: Computation[MyIO]
  ): MyIO[Either[Throwable, A]] =
    comp.attempt(self)

  def option(implicit
    comp: Computation[MyIO]
  ): MyIO[Option[A]] =
    comp.option(self)

  def handleErrorWith[AA >: A](f: Throwable => MyIO[AA])(implicit
    comp: Computation[MyIO]
  ): MyIO[AA] =
    comp.handleErrorWith[A, AA](self)(f)

  def redeem[B](recover: Throwable => B, map: A => B)(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.redeem(self)(recover, map)

  def redeemWith[B](recover: Throwable => MyIO[B], bind: A => MyIO[B])(implicit
    comp: Computation[MyIO]
  ): MyIO[B] =
    comp.redeemWith(self)(recover, bind)

  def unsafeRunSync(implicit
    comp: Computation[MyIO]
  ): A =
    comp.unsafeRunSync(self)

}

object MyIO {

  implicit val computationInstanceForIO: Computation[MyIO] = new Computation[MyIO] {
    override def map[A, B](fa: MyIO[A])(f: A => B): MyIO[B] =
      myIOAndThen[A, B](fa, _.map(f))

    override def flatMap[A, B](fa: MyIO[A])(f: A => MyIO[B]): MyIO[B] =
      new MyIO(fa.body.flatMap(f.andThen(_.body)))

    @tailrec
    override def tailRecM[A, B](a: A)(f: A => MyIO[Either[A, B]]): MyIO[B] = f(a).call() match {
      case Left(exception) => raiseError(exception)
      case Right(Left(a1)) => tailRecM(a1)(f)
      case Right(Right(b)) => pure(b)
    }

    override def pure[A](a: A): MyIO[A] = MyIO.pure(a)

    override def *>[A, B](fa: MyIO[A])(another: MyIO[B]): MyIO[B] =
      flatMap(fa)(_ => another)

    override def as[A, B](fa: MyIO[A])(newValue: => B): MyIO[B] =
      map(fa)(_ => newValue)

    override def void[A](fa: MyIO[A]): MyIO[Unit] = as(fa)(())

    private def transformError[A, F[_]](fa: MyIO[A], f: MyTry[A] => F[A]): MyIO[F[A]] =
      myIOAndThen[A, F[A]](fa, f.andThen(MyTry.success))

    override def attempt[A](fa: MyIO[A]): MyIO[Either[Throwable, A]] =
      transformError(fa, identity)

    override def option[A](fa: MyIO[A]): MyIO[Option[A]] =
      transformError(fa, _.toOption)

    override def handleErrorWith[A, AA >: A](fa: MyIO[A])(f: Throwable => MyIO[AA]): MyIO[AA] =
      new MyIO(
        fa.body.leftFlatMap(f.andThen(_.body))
      )

    override def redeem[A, B](fa: MyIO[A])(recover: Throwable => B, bind: A => B): MyIO[B] =
      handleErrorWith(map(fa)(bind))(recover.andThen(pure))

    override def redeemWith[A, B](
      fa: MyIO[A]
    )(recover: Throwable => MyIO[B], bind: A => MyIO[B]): MyIO[B] =
      handleErrorWith(flatMap(fa)(bind))(recover)

    override def unsafeRunSync[A](fa: MyIO[A]): A = fa.call() match {
      case Left(throwable) => throw throwable
      case Right(result)   => result
    }

    override def raiseError[A](error: Throwable): MyIO[A] =
      fromMyTry(
        Eval.now(MyTry.failure(error))
      )
  }

  private type MyTry[A] = Either[Throwable, A]

  private object MyTry {

    def success[A](value: A): MyTry[A]             = Right(value)
    def failure[A](exception: Throwable): MyTry[A] = Left(exception)

  }

  private type MyTryT[A] = EitherT[Eval, Throwable, A]

  private def fromMyTry[A](t: Eval[MyTry[A]]): MyIO[A] =
    new MyIO[A](EitherT(t))

  private def myIOAndThen[A, B](io: MyIO[A], f: MyTry[A] => MyTry[B]): MyIO[B] =
    fromMyTry(io.body.value.map(f))

  def apply[A](body: => A): MyIO[A] = delay(body)

  def suspend[A](thunk: => MyIO[A])(implicit
    comp: Computation[MyIO]
  ): MyIO[A] =
    unit.flatMap(_ => thunk)(comp)

  def delay[A](body: => A): MyIO[A] =
    fromMyTry(
      Eval.later(
        try
          MyTry.success(body)
        catch {
          case e: Throwable => Left(e)
        }
      )
    )

  def pure[A](a: A): MyIO[A] = MyIO(a)

  def fromEither[A](e: Either[Throwable, A])(implicit
    comp: Computation[MyIO]
  ): MyIO[A] =
    fromTry(e.toTry)(comp)

  def fromOption[A](option: Option[A])(orElse: => Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[A] =
    fromEither(option.toRight(orElse))(comp)

  def fromTry[A](t: Try[A])(implicit
    comp: Computation[MyIO]
  ): MyIO[A] = t match {
    case Failure(exception) => comp.raiseError(exception)
    case Success(value)     => comp.pure(value)
  }

  def none[A]: MyIO[Option[A]] = pure(None)

  def raiseUnless(cond: Boolean)(e: => Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] =
    fromEither(Either.cond(cond, (), e))(comp)

  def raiseWhen(cond: Boolean)(e: => Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[Unit] =
    raiseUnless(!cond)(e)(comp)

  def raiseError[A](error: Throwable)(implicit
    comp: Computation[MyIO]
  ): MyIO[A] =
    comp.raiseError(error)

  def unlessA(cond: Boolean)(action: => MyIO[Unit]): MyIO[Unit] =
    if (cond) MyIO.unit else action

  def whenA(cond: Boolean)(action: => MyIO[Unit]): MyIO[Unit] =
    unlessA(!cond)(action)

  val unit: MyIO[Unit] = MyIO.pure(())

}
