package ventilation

import StateUtils.sequence
import ventilation.AQueue.{dequeEnqueue, enqueue, getMax}

trait Ventilation {
  def solve(degrees: List[Int], k: Int): List[Int]
}

object Ventilation {
  object VentilationVanilla extends Ventilation {
    override def solve(degrees: List[Int], k: Int): List[Int] =
      if (degrees.isEmpty) // special case because 1 <= k (and somehow k <= n)
        List.empty
      else
        degrees
          .sliding(k)
          .map(_.max)
          .toList
  }

  object VentilationOptimized extends Ventilation {
    override def solve(degrees: List[Int], k: Int): List[Int] = {
      if (degrees.isEmpty)
        List.empty
      else {
        val firstWindow = degrees
          .slice(0, k)
          .map(enqueue)
        val startState = for {
          _ <- sequence(firstWindow)
          max <- getMax
        } yield max

        val restState = degrees
          .slice(k, degrees.size)
          .map { elem =>
            for {
              _ <- dequeEnqueue(elem)
              max <- getMax
            } yield max
          }
        val initQueue = AQueue.of()
        sequence(startState :: restState).run(initQueue).value._2.flatten
      }
    }
  }
}
