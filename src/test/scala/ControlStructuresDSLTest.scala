import org.scalatest.*
import org.scalatest.funspec.AnyFunSpec
import matchers.should.Matchers.*
import SetTheoryDSL.SetExpression.*

import scala.collection.mutable

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
      it("should evaluate then clause and ignore else clause of IfElse Expression if condition evaluated is truthy") {
        Assign("var4", Value(20)).eval
        Assign("set4", SetIdentifier( Value(10) )).eval

        IfElse( Check( Equals(Variable("var4"), Value(20)) ),
          Then(
            InsertInto( Variable("set4"), Value(50))
          ),
          Else(
            InsertInto( Variable("set4"), Value(20))
          )
        ).eval

        assert( Variable("set4").eval == Set(10, 50) )
      }

      it("should evaluate multiple expressions in the then clause of IfElse Expression if condition evaluated is truthy") {
        Assign("var5", Value(20)).eval
        Assign("set5", SetIdentifier( Value(10) )).eval

        IfElse( Check( Equals(Variable("var5"), Value(20)) ),
          Then(
            InsertInto( Variable("set5"), Value(50), Value(true) ),
            DeleteFrom( Variable("set5"), Value(50) )
          ),
          Else(
            InsertInto( Variable("set5"), Value(20))
          )
        ).eval

        assert( Variable("set5").eval == Set(10, true) )
      }

      it("should evaluate else clause and ignore then clause of IfElse Expression if condition evaluated is false") {
        Assign("var6", Value(20)).eval
        Assign("set6", SetIdentifier( Value(10) )).eval

        IfElse( Check( Equals(Variable("var6"), Value("20")) ),
          Then(
            InsertInto( Variable("set6"), Value(50))
          ),
          Else(
            InsertInto( Variable("set6"), Value(20))
          )
        ).eval

        assert( Variable("set6").eval == Set(10, 20) )
      }

      it("should evaluate multiple expressions in the else clause of IfElse Expression if condition evaluated is false") {
        Assign("var7", Value(20)).eval
        Assign("set7", SetIdentifier( Value(10) )).eval

        IfElse( Check( Equals(Variable("var7"), Value("20")) ),
          Then(
            InsertInto( Variable("set7"), Value(20))
          ),
          Else(
            InsertInto( Variable("set7"), Value(50), Value(true) ),
            DeleteFrom( Variable("set7"), Value(50) )
          )
        ).eval

        assert( Variable("set7").eval == Set(10, true) )
      }
    }

    describe("Testing exception throwing and handling capabilities of the DSL") {
      it("should create an exception class and throw an instance of that exception class in the global scope, which will not be handled") {
        val exceptionClassName = "Exception_1"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        val thrown = the [Exception] thrownBy ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause)).eval
        thrown.getMessage should equal ("Unhandled Exception")
      }

      it("should create an exception class and throw an instance of that exception class in a nested named scope, which will not be handled") {
        val exceptionClassName = "Exception_2"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        val thrown = the [Exception] thrownBy NamedScope("scope1",
          ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause))
        ).eval
        thrown.getMessage should equal ("Unhandled Exception")
      }

      it("should create an exception class and throw an instance of that exception class in a nested unnamed scope, which will not be handled") {
        val exceptionClassName = "Exception_3"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        val thrown = the [Exception] thrownBy UnnamedScope(
          ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause))
        ).eval
        thrown.getMessage should equal ("Unhandled Exception")
      }

      it("should catch an exception and handle it in the catch block of TryCatch expression when thrown inside the try block") {
        val exceptionClassName = "Exception_4"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set8", SetIdentifier( Value(10) )).eval
        InsertInto(Variable("set8"), Value(20)).eval
        TryCatch(
          Try(
            InsertInto(Variable("set8"), Value(50)),
            ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause)),
            InsertInto(Variable("set8"), Value(100)),
          ),
          Catch("e1", ClassRef(exceptionClassName),
            InsertInto(Variable("set8"), FieldFromObject("cause", Variable("e1")))
          )
        ).eval

        assert(Variable("set8").eval == Set(10, 20, 50, exceptionCause))
      }

      it("should ignore all the expression after an exception is thrown in the try block and move on after handling the catch block expression") {
        val exceptionClassName = "Exception_5"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set9", SetIdentifier( Value(10) )).eval
        InsertInto(Variable("set9"), Value(20)).eval
        TryCatch(
          Try(
            InsertInto(Variable("set9"), Value(50)),
            ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause)),
            InsertInto(Variable("set9"), Value(100)),
            DeleteFrom(Variable("set9"), Value(100), Value(50), Value(10))
          ),
          Catch("e1", ClassRef(exceptionClassName),
            InsertInto(Variable("set9"), FieldFromObject("cause", Variable("e1")))
          )
        ).eval
        InsertInto(Variable("set9"), Value("after try catch block")).eval

        assert(Variable("set9").eval == Set(10, 20, 50, exceptionCause, "after try catch block"))
      }

      it("should throw an exception deeply nested in a try block and propagate to the matching catch block") {
        val exceptionClassName = "Exception_6"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set10", SetIdentifier( Value(10) )).eval
        InsertInto(Variable("set10"), Value(20)).eval
        TryCatch(
          Try(
            InsertInto(Variable("set10"), Value(50)),
            UnnamedScope(
              InsertInto(Variable("set10"), Value(60)),
              NamedScope("scope2",
                InsertInto(Variable("set10"), Value(70)),
                ThrowNewException(ClassRef(exceptionClassName), Value(exceptionCause))
              )
            ),
            InsertInto(Variable("set10"), Value(100)),
            DeleteFrom(Variable("set10"), Value(100), Value(50), Value(10))
          ),
          Catch("e1", ClassRef(exceptionClassName),
            InsertInto(Variable("set10"), FieldFromObject("cause", Variable("e1")))
          )
        ).eval

        assert(Variable("set10").eval == Set(10, 20, 50, 60, 70, exceptionCause))
      }

      it("should not handle an exception when the thrown exception is different from the one whose signature is mentioned in catch block") {
        val exceptionClassName1 = "Exception_7"
        val exceptionClassName2 = "Exception_8"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName1,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        ExceptionClassDef(exceptionClassName2,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set11", SetIdentifier( Value(10) )).eval

        val thrown = the [Exception] thrownBy TryCatch(
          Try(
            InsertInto(Variable("set11"), Value(50)),
            ThrowNewException(ClassRef(exceptionClassName1), Value(exceptionCause)),
            InsertInto(Variable("set11"), Value(100)),
          ),
          Catch("e1", ClassRef(exceptionClassName2),
            InsertInto(Variable("set11"), FieldFromObject("cause", Variable("e1")))
          )
        ).eval

        thrown.getMessage should equal ("Unhandled Exception")
      }

      it("should take advantage of multiple catch blocks in TryCatch expression and handle exception later in the chain") {
        val exceptionClassName1 = "Exception_9"
        val exceptionClassName2 = "Exception_10"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName1,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        ExceptionClassDef(exceptionClassName2,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set12", SetIdentifier( Value(10) )).eval

        TryCatch(
          Try(
            InsertInto(Variable("set12"), Value(50)),
            ThrowNewException(ClassRef(exceptionClassName1), Value(exceptionCause)),
            InsertInto(Variable("set12"), Value(100)),
          ),
          Catch("e1", ClassRef(exceptionClassName2),
            InsertInto(Variable("set12"), FieldFromObject("cause", Variable("e1")))
          ),
          Catch("e1", ClassRef(exceptionClassName1),
            InsertInto(Variable("set12"), Value(200))
          )
        ).eval

        assert(Variable("set12").eval == Set(10, 50, 200))
      }

      it("should take advantage of dynamic dispatch and inheritance and catch an exception of sub class type in catch block looking for exception with the parent type") {
        val exceptionClassName1 = "Exception_11"
        val exceptionClassName2 = "Exception_12"
        val exceptionCause = "Custom Exception Cause"
        ExceptionClassDef(exceptionClassName1,
          CreatePublicField("cause"),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        ExceptionClassDef(exceptionClassName2,
          Extends(ClassRef(exceptionClassName1)),
          Constructor(
            ParamsExp(Param("passedCause")),
            SetField("cause", Variable("passedCause"))
          )
        ).eval

        Assign("set13", SetIdentifier( Value(10) )).eval

        TryCatch(
          Try(
            InsertInto(Variable("set13"), Value(50)),
            ThrowNewException(ClassRef(exceptionClassName2), Value(exceptionCause)),
            InsertInto(Variable("set13"), Value(100)),
          ),
          Catch("e1", ClassRef(exceptionClassName1),
            InsertInto(Variable("set13"), Value(200))
          )
        ).eval

        assert(Variable("set13").eval == Set(10, 50, 200))
      }
    }

  }
}
