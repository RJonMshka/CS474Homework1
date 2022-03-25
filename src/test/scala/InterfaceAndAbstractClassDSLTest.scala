import org.scalatest.*
import org.scalatest.funspec.AnyFunSpec
import matchers.should.Matchers.*
import SetTheoryDSL.SetExpression.*

import scala.collection.mutable

class InterfaceAndAbstractClassDSLTest extends AnyFunSpec {

  describe("Testing Interfaces and Abstract Classes in DSL SetTheory") {

    describe("Interface declaration, implementation and inheritance") {

      it("should throw an Exception if one interface tries to implement another interface") {
        InterfaceDef(
          "I1",
          CreatePublicField("f1"),
          Method(
            "m1",
            PublicAccess(),
            ParamsExp(Param("a"), Param("b"))
          )
        ).eval
        assertThrows[Exception] {
          InterfaceDef(
            "I2",
            Implements(InterfaceRef("I1")),
            CreatePublicField("f1"),
            Method(
              "m1",
              PublicAccess(),
              ParamsExp(Param("a"), Param("b"))
            )
          ).eval
        }
      }

      it("should throw an Exception if an attempt to declare an interface with the name of an existing interface in the same scope") {
        assertThrows[Exception] {
          InterfaceDef(
            "I1"
          ).eval
        }
      }

      it("should throw an Exception if one interface extends an another interface and tries to create a method with same name which is already declared in super interface") {
        assertThrows[Exception] {
          InterfaceDef(
            "I3",
            Extends(InterfaceRef("I1")),
            CreatePublicField("f1"),
            Method(
              "m1",
              PublicAccess(),
              ParamsExp(Param("a"), Param("b"))
            )
          ).eval
        }
      }

      it("should throw an Exception if 'Extends' expression is present more than once in interface body") {
        assertThrows[Exception] {
          InterfaceDef(
            "I4",
            Extends(InterfaceRef("I1")),
            Extends(InterfaceRef("I1"))
          ).eval
        }
      }

    }

  }

}
