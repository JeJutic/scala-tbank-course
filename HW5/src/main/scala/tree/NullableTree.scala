package tree

import TraverseMethod.{BreadthFirstSearch, DepthFirstSearch}

sealed trait NullableTree

object NullableTree {
  object NullTree extends NullableTree

  case class Tree(value: Int, left: NullableTree = NullTree, right: NullableTree = NullTree) extends NullableTree {
    self =>

    def add(newValue: Int): Tree = mergeWithTree(Tree(newValue, NullTree, NullTree))(self)

    def delete(toDelete: Int): NullableTree = {
      val deleteOp = flatMapTree(_.delete(toDelete))
      val newLeft = deleteOp(left)
      val newRight = deleteOp(right)

      if (value == toDelete)
        merge(newLeft, newRight)
      else Tree(value, newLeft, newRight)
    }

    private def mapFoldLeft[A, B](method: TraverseMethod)(zero: B)(transform: Tree => A)(f: (B, A) => B): B =
      method(self).result
        .map(transform)
        .foldLeft(zero)(f)

    def foldLeft[B](method: TraverseMethod)(zero: B)(f: (B, Tree) => B): B =
      mapFoldLeft(method)(zero)(identity)(f)

    private def toSeq(method: TraverseMethod): Seq[Tree] =
      foldLeft(method)(List.empty[Tree])((list, tree) => tree :: list).reverse

    def depthFirstSearch(): Seq[Tree] = toSeq(DepthFirstSearch)
    def breadthFirstSearch(): Seq[Tree] = toSeq(BreadthFirstSearch)

    private def foldLeftOnValue(method: TraverseMethod, f: (Int, Int) => Int): Int =
      mapFoldLeft(method)(value)(_.value)(f)

    def max(method: TraverseMethod): Int = foldLeftOnValue(method, _.max(_))
    def min(method: TraverseMethod): Int = foldLeftOnValue(method, _.min(_))

    def size: Int = mapFoldLeft(DepthFirstSearch)(0)(_ => 1)(_ + _)

    def print: String =
      BreadthFirstSearch
        .applyWithDepth(self)
        .result
        .map(line => line.map(_.value).mkString(" "))
        .mkString(System.lineSeparator())
  }

  private def flatMapTree(f: Tree => NullableTree): NullableTree => NullableTree = {
    case t @ Tree(_, _, _) => f(t)
    case NullTree          => NullTree
  }

  private def merge(first: NullableTree, second: NullableTree): NullableTree = first match {
    case t @ Tree(_, _, _) => mergeWithTree(t)(second)
    case NullTree          => second
  }

  private def mergeWithTree(first: Tree): NullableTree => Tree = {
    case Tree(value, left, right) => Tree(value, mergeWithTree(first)(left), right)
    case NullTree                 => first
  }
}
