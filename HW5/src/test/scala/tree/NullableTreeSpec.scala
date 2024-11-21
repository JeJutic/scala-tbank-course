package tree

import org.scalatest.Inside.inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import tree.NullableTree.{NullTree, Tree}
import tree.TraverseMethod.{BreadthFirstSearch, DepthFirstSearch}

class NullableTreeSpec extends AnyFlatSpec with Matchers {

  private def seqToValue(trees: Seq[Tree]): Seq[Int] = trees.map[Int](_.value)
  private def treeToSet(tree: Tree): Set[Int] = tree.depthFirstSearch().map[Int](_.value).toSet

  "add" should "add a node" in {
    val table = Table(
      ("original tree", "value to add", "expected set of vertices"),
      (Tree(1), 2, Set(1, 2)),
      (Tree(1, Tree(3)), 2, Set(1, 2, 3)),
      (Tree(1, Tree(3), Tree(4)), 2, Set(1, 2, 3, 4))
    )
    forAll(table) { (tree, toAdd, expected) =>
      treeToSet(tree.add(toAdd)) shouldEqual expected
    }
  }

  it should "add a node even if a node with this value already exists" in {
    val tree = Tree(1, Tree(3), Tree(4))
      .add(3)

    tree.size shouldEqual 4
    treeToSet(tree) shouldEqual Set(1, 3, 4)
  }

  "delete" should "delete nodes with specified value" in {
    val table = Table(
      ("original tree", "value to delete", "expected set of vertices"),
      (Tree(1, Tree(3)), 3, Set(1)),
      (Tree(1, Tree(3), Tree(4)), 1, Set(3, 4))
    )
    forAll(table) { (tree, toDelete, expected) =>
      inside(tree.delete(toDelete)) { case t @ Tree(_, _, _) =>
        treeToSet(t) shouldEqual expected
      }
    }
  }

  "delete" should "return NullTree" in {
    Tree(1).delete(1) should matchPattern { case NullTree => }
  }

  it should "delete all occurrences" in {
    val tree = Tree(1, Tree(2, Tree(3), Tree(2)), Tree(2))
      .delete(2)

    inside(tree) { case t @ Tree(_, _, _) =>
      t.size shouldEqual 2
      treeToSet(t) shouldEqual Set(1, 3)
    }
  }

  "foldLeft" should "use method specified" in {
    val table = Table(
      ("tree", "expected for dfs", "expected for bfs"),
      (Tree(1), "1", "1"),
      (Tree(1, Tree(3)), "13", "13"),
      (Tree(1, Tree(3, Tree(5)), Tree(4)), "1354", "1345")
    )
    forAll(table) { (tree, dfsExpected, bfsExpected) =>
      tree.foldLeft(DepthFirstSearch)("") { (str, t) =>
        str + t.value.toString
      } shouldEqual dfsExpected
      tree.foldLeft(DepthFirstSearch)("start") { (str, t) =>
        str + t.value.toString
      } shouldEqual "start" + dfsExpected
      tree.foldLeft(BreadthFirstSearch)("") { (str, t) =>
        str + t.value.toString
      } shouldEqual bfsExpected
      tree.foldLeft(BreadthFirstSearch)("start") { (str, t) =>
        str + t.value.toString
      } shouldEqual "start" + bfsExpected
    }
  }

  "depthFirstSearch" should "have implied ordering" in {
    val table = Table(
      ("tree", "expected"),
      (Tree(1, Tree(3, Tree(5)), Tree(4)), Seq(1, 3, 5, 4)),
      (
        Tree(
          1,
          Tree(3, Tree(5, Tree(6))),
          Tree(4, Tree(7))
        ),
        Seq(1, 3, 5, 6, 4, 7)
      )
    )
    forAll(table) { (tree, expected) =>
      seqToValue(tree.depthFirstSearch()) shouldEqual expected
    }
  }

  "breadthFirstSearch" should "have implied ordering" in {
    val table = Table(
      ("tree", "expected"),
      (Tree(1, Tree(3, Tree(5)), Tree(4)), Seq(1, 3, 4, 5)),
      (
        Tree(
          1,
          Tree(3, Tree(5, Tree(6))),
          Tree(4, Tree(7))
        ),
        Seq(1, 3, 4, 5, 7, 6)
      )
    )
    forAll(table) { (tree, expected) =>
      seqToValue(tree.breadthFirstSearch()) shouldEqual expected
    }
  }

  "max" should "return max element in the tree" in {
    val table = Table(
      ("tree", "expected"),
      (Tree(1), 1),
      (Tree(2, Tree(0)), 2),
      (Tree(4, Tree(3, Tree(5)), Tree(2)), 5)
    )
    forAll(table) { (tree, expected) =>
      tree.max(DepthFirstSearch) shouldEqual expected
      tree.max(BreadthFirstSearch) shouldEqual expected
    }
  }

  "min" should "return min element in the tree" in {
    val table = Table(
      ("tree", "expected"),
      (Tree(1), 1),
      (Tree(2, Tree(0)), 0),
      (Tree(4, Tree(3, Tree(5)), Tree(2)), 2)
    )
    forAll(table) { (tree, expected) =>
      tree.min(DepthFirstSearch) shouldEqual expected
      tree.min(BreadthFirstSearch) shouldEqual expected
    }
  }

  "size" should "return work" in {
    val table = Table(
      ("tree", "expected"),
      (Tree(1), 1),
      (Tree(2, Tree(0)), 2),
      (Tree(4, Tree(3, Tree(5)), Tree(2)), 4)
    )
    forAll(table) { (tree, expected) =>
      tree.size shouldEqual expected
    }
  }

  "print" should "work" in {
    Tree(4, Tree(3, Tree(1), Tree(2)), Tree(5, Tree(6), Tree(7))).print shouldEqual "4" + System.lineSeparator() +
      "3 5" + System.lineSeparator() +
      "1 2 6 7"
  }

}
