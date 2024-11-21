package ventilation

import org.scalacheck.{Gen, Prop}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatestplus.scalacheck.Checkers
import ventilation.Ventilation.{VentilationOptimized, VentilationVanilla}

class VentilationSpec extends AnyFlatSpec with Matchers with Checkers {

  "solve" should "work for both implementations" in {
    forAll(Table("implementation", VentilationVanilla, VentilationOptimized)) { ventilation =>
      val table = Table(
        ("degrees", "k", "expected"),
        (List(1, 2, 3, 4), 1, List(1, 2, 3, 4)),
        (List(1, 2, 3, 4), 2, List(2, 3, 4)),
        (List(1, 2, 3, 4), 3, List(3, 4)),
        (List(1, 0, 0), 2, List(1, 0)),
        (List(1, 0), 1, List(1, 0)),
        (List(), 42, List())
      )
      forAll(table) { (degrees, k, expected) =>
        ventilation.solve(degrees, k) shouldEqual expected
      }
    }
  }

  private val inputGen: Gen[(List[Int], Int)] = Gen.sized { size =>
    for {
      degrees <- Gen.listOfN(size, arbitrary[Int])
      k <- if (size > 0) Gen.choose(1, size) else arbitrary[Int]
    } yield (degrees, k)
  }

  "solve" should "return the same result" in {
    check(Prop.forAll(inputGen) { case (degrees, k) =>
      VentilationVanilla.solve(degrees, k) == VentilationOptimized.solve(degrees, k)
    })
  }
}
