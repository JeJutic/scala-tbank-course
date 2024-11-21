package ventilation

import cats.data.State
import StateUtils.{fixed, transform}
import ventilation.AQueue.max

case class Element(value: Int, max: Int)

case class AQueue private (stackIn: List[Element], stackOut: List[Element]) {
  self =>
  private def newElement(elem: Int): Element = {
    Element(elem, AQueue.max(elem, stackIn.headOption.map(_.max)))
  }

  private lazy val getMax: Option[Int] = max(stackIn.headOption.map(_.max), stackOut.headOption.map(_.max))

  def enqueue(elem: Int): AQueue = self.copy(stackIn = stackIn.prepended(newElement(elem)))

  private lazy val rebalance: AQueue = {
    val firstElem = stackIn.headOption
      .map(elem => Element(elem.value, elem.value))
      .getOrElse(Element(0, Int.MinValue))
    val reweighted =
      stackIn.scanLeft(firstElem)((prevEl, elem) => Element(elem.value, elem.value.max(prevEl.max))) match {
        case _ :: tail => tail // getting rid of initial value
        case Nil       => List.empty // never reached
      }

    copy(
      stackIn = List.empty,
      stackOut = reweighted.reverse
    )
  }

  def dequeEnqueue(elem: Int): AQueue = stackOut match {
    case _ :: tail2 =>
      copy(stackOut = tail2)
        .enqueue(elem)
    case Nil =>
      rebalance.stackOut match {
        case _ :: tail1 =>
          rebalance
            .copy(stackOut = tail1)
            .enqueue(elem)
        case Nil => AQueue.of()
      }
  }
}

object AQueue {
  def of(): AQueue = AQueue(List.empty, List.empty)

  private def max(a: Int, b: Option[Int]): Int = a.max(b.getOrElse(Int.MinValue))
  private def max(a: Option[Int], b: Option[Int]): Option[Int] = a.map(max(_, b)).orElse(b)

  val getMax: State[AQueue, Option[Int]] = fixed { _.getMax }

  def enqueue(elem: Int): State[AQueue, Unit] = transform(_.enqueue(elem))

  def dequeEnqueue(elem: Int): State[AQueue, Unit] = transform(_.dequeEnqueue(elem))
}
