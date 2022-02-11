
import javafx.beans.binding.SetExpression

import collection.mutable
import scala.annotation.tailrec

//class SetObject(value: Any) {
//  private val setValue = value
//
//  def getValue(): Any = setValue
//}

object SetTheoryDSL {


  object WrapperObj {

    type SetType = mutable.Set[Any]

    private class Scope(name: String, parent: Scope) {
      val scopeName: String = name
      val scopeParent: Scope = parent
      val childScopes: mutable.Map[String, Scope] = mutable.Map()
      val bindingEnvironment: mutable.Map[String, Any] = mutable.Map()
      val macros: mutable.Map[String, SetExpression] = mutable.Map()
    }

    @tailrec
    private def getVariable(varName: String, scopeEnv: Scope): Any =
      if (!(scopeEnv.bindingEnvironment.get(varName).isEmpty && scopeEnv.scopeParent != null)) then
        scopeEnv.bindingEnvironment(varName)
      else
        getVariable(varName, scopeEnv.scopeParent)

    private val currentEnvironment: Array[Scope] = new Array[Scope](1)
    private val index = 0
    currentEnvironment(index) = new Scope("globalScope", null)

    enum SetExpression:
      case Value(value: Any)
      case Variable(varName: String)
      case Union(s1: SetExpression, s2: SetExpression)
      case Intersection(setA: SetExpression, setB: SetExpression)
      case SetDifference(setA: SetExpression, setB: SetExpression)
      case SymDifference(setA: SetExpression, setB: SetExpression)
      case CartesianProduct(setA: SetExpression, setB: SetExpression)
      case Macro(m: String, macroExp: SetExpression)
      case ComputeMacro(m: String)
      case NamedScope(scopeName: String, scopeExpArgs: SetExpression*)
      case UnnamedScope(scopeExpArgs: SetExpression*)
      case Check(setName: String, exp: SetExpression)
      case Assign(name: String, exp: SetExpression)
      case SetIdentifier(setExpArgs: SetExpression*)
      case InsertInto(name: String, setExpArgs: SetExpression*)
      case DeleteFrom(name: String, setExpArgs: SetExpression*)


      def eval: Any = this match {
        case Value(v) => v
        case Variable(varName) => getVariable(varName, currentEnvironment(index))
        case Assign(varName, exp) =>
          val evaluatedExp = exp.eval
          currentEnvironment(index).bindingEnvironment.put(varName, evaluatedExp)
          evaluatedExp
        case Macro(m, exp) => currentEnvironment(index).macros.put(m, exp)
        case ComputeMacro(m) => getMacro(m, currentEnvironment(index)).eval
        case NamedScope(scopeName: String, expArgs*) =>
          // opening a new scope block
          if currentEnvironment(0).childScopes.isEmpty || !currentEnvironment(0).childScopes.contains(scopeName) then
            val newScope: Scope = new Scope(scopeName, currentEnvironment(0))
            currentEnvironment(0).childScopes += (scopeName -> newScope)
            currentEnvironment(0) = newScope
            expArgs.foreach( _.eval )
            currentEnvironment(0) = newScope.scopeParent
          else
            val nestedScope: Scope = currentEnvironment(0).childScopes(scopeName)
            currentEnvironment(0) = nestedScope
            expArgs.foreach( _.eval )
            currentEnvironment(0) = nestedScope.scopeParent
        case UnnamedScope(expArgs*) =>
          val newScope: Scope = new Scope(null, currentEnvironment(0))
          currentEnvironment(0) = newScope
          expArgs.foreach( _.eval )
          currentEnvironment(0) = newScope.scopeParent
        case SetIdentifier(setExpArgs*) =>
          val newSet: SetType = mutable.Set()
          setExpArgs.foreach(newSet += _.eval)
          newSet
        case Union(s1, s2) => s1.eval.asInstanceOf[SetType] | s2.eval.asInstanceOf[SetType]
        case Intersection(s1, s2) => s1.eval.asInstanceOf[SetType] & s2.eval.asInstanceOf[SetType]
        case SetDifference(s1, s2) => s1.eval.asInstanceOf[SetType] &~ s2.eval.asInstanceOf[SetType]
        case SymDifference(s1, s2) =>
          val set1 = s1.eval.asInstanceOf[SetType]
          val set2 = s2.eval.asInstanceOf[SetType]
          (set1 | set2) &~ (set1 & set2)
        case CartesianProduct(s1, s2) =>
          for {x <- s1.eval.asInstanceOf[SetType]; y <- s2.eval.asInstanceOf[SetType]} yield (x, y)
        case InsertInto(name, setExpArgs*) =>
          val storedSet = Variable(name).eval.asInstanceOf[SetType]
          setExpArgs.foreach(storedSet += _.eval)
          storedSet
        case DeleteFrom(name, setExpArgs*) =>
          val storedSet = Variable(name).eval.asInstanceOf[SetType]
          setExpArgs.foreach( i => storedSet.remove(i.eval))
      }

    @tailrec
    private def getMacro(macroName: String, scopeEnv: Scope): SetExpression =
      if (!(scopeEnv.macros.get(macroName).isEmpty && scopeEnv.scopeParent != null)) then
        scopeEnv.macros(macroName)
      else
        getMacro(macroName, scopeEnv.scopeParent)
  }



  @main def runSetTheory(): Unit = {
    import WrapperObj.SetExpression.*
    Macro("m1", Variable("var3")).eval
    Assign("var1", Value(20)).eval
    Assign("var3", Value("sexy")).eval
    Assign("set1", SetIdentifier(Value(30), Variable("var1"), ComputeMacro("m1"))).eval
    Assign("set2", Union(SetIdentifier(Value("hello"), Value(60)), SetIdentifier(Value(5), Value(30)))).eval
    Assign("set3", SetIdentifier()).eval
    InsertInto("set2", Value(400)).eval
    NamedScope("a",
      Assign("var_a", Value("var_a_val")),
      UnnamedScope(
        InsertInto("set2", Variable("var_a"))
      )
    ).eval

    NamedScope("a",
      NamedScope("b",
        InsertInto("set1", Variable("var_a")),
        Assign("var_b", Value("val_b"))
      )
    ).eval

    NamedScope("a",
      NamedScope("b",
        InsertInto("set1", Variable("var_b")),
      )
    ).eval


    println(Variable("set2").eval)
    println(Variable("set1").eval)
//    println(Variable("set1").eval)
//    println(Variable("set2").eval)
//    DeleteFrom("set2", Value(60)).eval
//    println(Variable("set2").eval)
  }

}
