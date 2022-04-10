import org.scalatest.*
import org.scalatest.funspec.AnyFunSpec
import matchers.should.Matchers.*
import SetTheoryDSL.SetExpression.*

class ControlStructuresDSLTest extends AnyFunSpec {
  describe("Testing Control Structures") {

    describe("Testing If Control Structure") {
      it("should evaluate the then clause if condition is truthy") {
        Assign("var1", Value(20)).eval
        Assign("set1", SetIdentifier( Value(10) )).eval

        If( Check( Equals(Variable("var1"), Value(20)) ),
          Then(
            InsertInto( Variable("set1"), Value(50))
          )
        ).eval

        assert( Variable("set1").eval == Set(10, 50) )
      }

      it("should evaluate multiple expressions passed to the then clause if condition is truthy") {
        Assign("var2", Value(20)).eval
        Assign("set2", SetIdentifier( Value(10) )).eval

        If( Check( Equals(Variable("var2"), Value(20)) ),
          Then(
            InsertInto( Variable("set2"), Value(50), Value(true)),
            DeleteFrom( Variable("set2"), Value(true) )
          )
        ).eval

        assert( Variable("set2").eval == Set(10, 50) )
      }

      it("should not evaluate the then clause if condition is false") {
        Assign("var3", Value(20)).eval
        Assign("set3", SetIdentifier( Value(10) )).eval

        If( Check( Equals(Variable("var3"), Value("20")) ),
          Then(
            InsertInto( Variable("set3"), Value(50))
          )
        ).eval

        assert( Variable("set3").eval == Set(10) )
      }
    }

    describe("Testing IfElse Control Structure") {

    }

  }
}
