import org.scalatest._
import org.scalatest.funspec.AnyFunSpec
import matchers.should.Matchers.*
import SetTheoryDSL.SetExpression.*

import scala.collection.mutable

class ClassesAndInheritanceDSLTest extends AnyFunSpec {

  describe("Testing Classes, Objects and Inheritance in DSL SetTheory") {

    describe("Class Definition, fields, and methods") {

      it("Should Define a class and create an object from it") {
        ClassDef(
          "ClassOne",
          CreatePublicField("f1"),
          CreateProtectedField("f2"),
          CreatePrivateField("f3"),
          CreateField("f4"),
          Constructor(
            ParamsExp(Param("a"), Param("b")),
            SetField("f1", Variable("a") ),
            SetField("f2", Variable("b") ),
            SetField("f3", Value(1)),
            SetField("f4", Value(2)),
          ),
          PublicMethod(
            "m1",
            ParamsExp(),
            InvokeMethod("m2")
          ),
          PublicMethod(
            "m2",
            ParamsExp(),
            Field("f2")
          )
        ).eval
        Assign( "object1", NewObject( ClassRef("ClassOne"), Value(3), Value(4) ) ).eval

        assert( FieldFromObject("f1", Variable("object1")).eval == 3 )
      }

      it("Should Invoke one public method of an object") {
        assert(Assign("var2", InvokeMethodOfObject("m2", Variable("object1")) ).eval == 4)
      }

      it("Should change a public of an object") {
        SetFieldFromObject("f1", Variable("object1"), Value("abc") ).eval
         assert( FieldFromObject("f1", Variable("object1")).eval == "abc" )
      }

      it("Should invoke another one method from another one") {
        assert(Assign("var4", InvokeMethodOfObject("m1", Variable("object1")) ).eval == 4)
      }

      it("Should throw an error when trying to access a protected field") {
        assertThrows[Exception] {
          FieldFromObject("f2", Variable("object1")).eval
        }
      }

      it("Should throw an error when trying to access a private field") {
        assertThrows[Exception] {
          FieldFromObject("f3", Variable("object1")).eval
        }
      }

      it("Should throw an error when trying to access a default field") {
        assertThrows[Exception] {
          FieldFromObject("f4", Variable("object1")).eval
        }
      }

      it("Should create another class by extending other - inheriting its accessible fields") {
        ClassDefThatExtends(
          "ClassTwo",
          ClassRef("ClassOne"),
          CreatePublicField("f5"),
          CreateProtectedField("f6"),
          CreatePrivateField("f7"),
          CreateField("f8"),
          Constructor(
            ParamsExp(Param("a"), Param("b")),
            SetField("f5", Field("f1") ),
            SetField("f6",  Field("f2") ),
            SetField("f7", Variable("a") ),
            SetField("f8", Variable("b") )
          ),
          PublicMethod(
            "m2",
            ParamsExp(),
            Value(100)
          ),
          PublicMethod(
            "m3",
            ParamsExp(),
            Field("f2")
          )
        ).eval

        Assign("object2", NewObject( ClassRef("ClassTwo"), Value(10), Value(11) ) ).eval
        assert( FieldFromObject("f5", Variable("object2") ).eval == 10  )
      }

      it("should prove that public fields are also inherited") {
        assert( FieldFromObject("f1", Variable("object2") ).eval == 10  )
      }

      it("should prove that protected fields are also inherited") {
        assert( InvokeMethodOfObject("m3", Variable("object2") ).eval == 11  )
      }



    }

  }
}
