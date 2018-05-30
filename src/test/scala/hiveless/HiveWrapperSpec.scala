package hiveless

import hiveless.HiveWrapper._
import org.scalatest.{Matchers, WordSpec}

class HiveWrapperSpec extends WordSpec with Matchers {
  "HiveWrapper" should {
    "make this able to compile" in {
      // Primitive
      implicitly[HiveWrapper[Int]]
      implicitly[HiveWrapper[Double]]
      implicitly[HiveWrapper[String]]

      // Structs
      case class Test(i: Int, d: Double)
      implicitly[HiveWrapper[Test]]

      // Nested structs
      case class Nested(s: String, t: Test)
      implicitly[HiveWrapper[Nested]]
    }
  }
}
