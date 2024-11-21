package tree

import tree.NullableTree.{NullTree, Tree}

import scala.collection.immutable.Queue
import scala.util.control.TailCalls.{TailRec, done, tailcall}

sealed trait TraverseMethod extends (Tree => TailRec[Seq[Tree]])

object TraverseMethod {
  object BreadthFirstSearch extends TraverseMethod {
    private def inner(queue: Queue[(Tree, Int)]): TailRec[List[(Tree, Int)]] = {
      queue.dequeueOption
        .map { case (head, q) =>
          def updateQueue(tree: NullableTree, queueToUpd: Queue[(Tree, Int)]) = tree match {
            case l @ Tree(_, _, _) => queueToUpd.appended((l, head._2 + 1))
            case NullTree          => queueToUpd
          }

          val newQ = updateQueue(head._1.right, updateQueue(head._1.left, q))
          tailcall(inner(newQ)).map(head :: _)
        }
        .getOrElse(done(List.empty))
    }

    def applyWithDepth(tree: Tree): TailRec[Seq[Seq[Tree]]] =
      inner(Queue((tree, 0)))
        .map(
          _.groupBy(_._2).values
            .map(_.map(_._1))
            .toList
        )

    override def apply(tree: Tree): TailRec[Seq[Tree]] = inner(Queue((tree, 0))).map(_.map(_._1))
  }

  object DepthFirstSearch extends TraverseMethod {
    private def applyNullable: NullableTree => TailRec[List[Tree]] = {
      case t @ Tree(_, _, _) => tailcall(apply(t))
      case NullTree          => done(List.empty)
    }

    override def apply(tree: Tree): TailRec[List[Tree]] = {
      for {
        left <- tailcall(applyNullable(tree.left))
        right <- tailcall(applyNullable(tree.right))
      } yield tree :: left ::: right
    }
  }
}
