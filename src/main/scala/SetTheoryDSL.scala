import collection.mutable
import scala.annotation.tailrec

object SetTheoryDSL {

  type SetType = mutable.Set[Any]

  private class Scope(name: String, parent: Scope) {
    val scopeName: String = name
    val scopeParent: Scope = parent
    val childScopes: mutable.Map[String, Scope] = mutable.Map()
    val bindingEnvironment: mutable.Map[String, Any] = mutable.Map()
  }

  @tailrec
  private def getVariable(varName: String, scopeEnv: Scope): Any =
    if !(!scopeEnv.bindingEnvironment.contains(varName) && scopeEnv.scopeParent != null) then
      scopeEnv.bindingEnvironment(varName)
    else
      getVariable(varName, scopeEnv.scopeParent)

  private val currentEnvironment: Array[Scope] = new Array[Scope](1)
  private val index = 0
  private val globalScopeName = "globalScope"
  currentEnvironment(index) = new Scope("globalScope", null)

  enum SetExpression:
    case Value(value: Any)
    case Variable(varName: String)
    case Union(s1: SetExpression, s2: SetExpression)
    case Intersection(setA: SetExpression, setB: SetExpression)
    case SetDifference(setA: SetExpression, setB: SetExpression)
    case SymDifference(setA: SetExpression, setB: SetExpression)
    case CartesianProduct(setA: SetExpression, setB: SetExpression)
    case Macro(macroExp: SetExpression)
    case ComputeMacro(macroExp: SetExpression)
    case NamedScope(scopeName: String, scopeExpArgs: SetExpression*)
    case UnnamedScope(scopeExpArgs: SetExpression*)
    case Check(setName: String, exp: SetExpression)
    case Assign(name: String, exp: SetExpression)
    case SetIdentifier(setExpArgs: SetExpression*)
    case InsertInto(name: String, setExpArgs: SetExpression*)
    case DeleteFrom(name: String, setExpArgs: SetExpression*)
    case Contains(setExp: SetExpression, valueExp: SetExpression)
    case Equals(exp1: SetExpression, exp2: SetExpression)


    def eval: Any = this match {
      case Value(v) => v
      case Variable(varName) => getVariable(varName, currentEnvironment(index))
      case Assign(varName, exp) =>
        val evaluatedExp = exp.eval
        currentEnvironment(index).bindingEnvironment.put(varName, evaluatedExp)
        evaluatedExp
      case Macro(exp) => exp
      case ComputeMacro(exp) => exp.eval.asInstanceOf[SetExpression].eval
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
      case Contains(setExp, valExp) =>
        val set = setExp.eval.asInstanceOf[SetType]
        set.contains(valExp.eval)
      case Equals(exp1, exp2) => exp1.eval.equals(exp2.eval)
    }

}
