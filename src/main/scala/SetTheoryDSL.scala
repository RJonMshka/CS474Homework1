/** Important imports */
import collection.mutable
import scala.annotation.tailrec

/** Factory for [[SetTheoryDSL]] instances. */
object SetTheoryDSL {

  /** Create a name for type mutable sets */
  type SetType = mutable.Set[Any]

  /** A Scope which is used to create a binding environment
   *
   *  @constructor create a new Scope with a name and its parent scope.
   *  @param name the scope's name
   *  @param parent a pointer to the scope's parent scope
   */
  private class Scope(name: String, parent: Scope) {
    val scopeName: String = name
    val scopeParent: Scope = parent
    // This field holds the child Scope in a HashMap of particular Scope Object
    val childScopes: mutable.Map[String, Scope] = mutable.Map()
    // This field holds the binding environment (binding variables) in a HashMap referring to the referencing environment of that particular Scope object
    val bindingEnvironment: mutable.Map[String, Any] = mutable.Map()
  }


  /** Returns data on type Any stored in referencing environment and if not found, then looks into parent scope's environment
   *
   * @param varName name of the variable to find
   * @param scopeEnv Scope of the environment where this function needs to find the variable
   */
  @tailrec
  private def getVariable(varName: String, scopeEnv: Scope): Any =
    if !(!scopeEnv.bindingEnvironment.contains(varName) && scopeEnv.scopeParent != null) then
      scopeEnv.bindingEnvironment(varName)
    else
      getVariable(varName, scopeEnv.scopeParent)

  // Creating private variables, a way to maintain pointer to current referencing environment
  private val currentEnvironment: Array[Scope] = new Array[Scope](1)
  private val index = 0
  private val globalScopeName = "globalScope"
  // Creating a global scope whose parent is null
  currentEnvironment(index) = new Scope("globalScope", null)


  /** Enumeration for different types of Set Expressions
   *
   */
  enum SetExpression:
    // Defining signature or abstraction of different Set Expressions

    /** Value Expression signature
     * used to represent a value that takes one param
     */
    case Value(value: Any)

    /** Variable Expression signature
     * used to refer to a variable
     */
    case Variable(varName: String)

    /** Assign Expression signature
     * used to assign variables to other expressions, which can be a Value, another Variable, SetIdentifier, Union, Intersection, and others as well
     */
    case Assign(name: String, exp: SetExpression)

    /** Macro Expression signature
     * used to create a Macro which can be lazily evaluated after it is declared and stored in a variable
     */
    case Macro(macroExp: SetExpression)

    /** ComputeMacro Expression signature
     * used to compute/evaluate a Macro which is already stored in a variable
     */
    case ComputeMacro(macroExp: SetExpression)

    /** NamedScope Expression signature
     * used to create a named scope
     */
    case NamedScope(scopeName: String, scopeExpArgs: SetExpression*)

    /** UnnamedScope Expression signature
     * used to create an anonymous scope
     */
    case UnnamedScope(scopeExpArgs: SetExpression*)

    /** SetIdentifier Expression signature
     * used to create a mutable Set
     */
    case SetIdentifier(setExpArgs: SetExpression*)

    /** Union Expression signature
     * used to evaluate Union of two sets
     */
    case Union(s1: SetExpression, s2: SetExpression)

    /** Intersection Expression signature
     * used to evaluate Intersection of two sets
     */
    case Intersection(setA: SetExpression, setB: SetExpression)

    /** SetDifference Expression signature
     * used to evaluate Set Difference of two sets
     */
    case SetDifference(setA: SetExpression, setB: SetExpression)

    /** SymDifference Expression signature
     * used to evaluate Symmetric Difference of two sets
     */
    case SymDifference(setA: SetExpression, setB: SetExpression)

    /** CartesianProduct Expression signature
     * used to evaluate Cartesian Product of two sets
     */
    case CartesianProduct(setA: SetExpression, setB: SetExpression)

    /** InsertInto Expression signature
     * inserts value(s) into a set
     */
    case InsertInto(setExp: SetExpression, setExpArgs: SetExpression*)

    /** DeleteFrom Expression signature
     * deletes value(s) from a set
     */
    case DeleteFrom(setExp: SetExpression, setExpArgs: SetExpression*)

    /** Contains Expression signature
     * checks if a set contains a particular value
     */
    case Contains(setExp: SetExpression, valueExp: SetExpression)

    /** Equals Expression signature
     * equates evaluated value of exp1 with evaluated value of exp2
     */
    case Equals(exp1: SetExpression, exp2: SetExpression)

    /** This method evaluates SetExpressions
     *  Description - The body of this method is the implementation of above abstract data types
     * @return Any
     */
    def eval: Any = this match {
      // Value Expression Implementation
      case Value(v) => v

      // Variable Expression Implementation
      case Variable(varName) => getVariable(varName, currentEnvironment(index))

      // Assign Expression Implementation
      case Assign(varName, exp) =>
        val evaluatedExp = exp.eval
        currentEnvironment(index).bindingEnvironment.put(varName, evaluatedExp)
        evaluatedExp

      // Macro Expression Implementation
      case Macro(exp) => exp

      // ComputeMacro Expression Implementation
      case ComputeMacro(exp) => exp.eval.asInstanceOf[SetExpression].eval

      // NamedScope Expression Implementation
      case NamedScope(scopeName: String, expArgs*) =>
        //check whether there are no child scope or if there are child scopes then check if the specified scope name is not a part of child scope
        if currentEnvironment(0).childScopes.isEmpty || !currentEnvironment(0).childScopes.contains(scopeName) then
          // opening a new scope block
          val newScope: Scope = new Scope(scopeName, currentEnvironment(0))
          // adding the new scope's reference to current scope's children
          currentEnvironment(0).childScopes += (scopeName -> newScope)
          // switching the current environment to the new scope
          currentEnvironment(0) = newScope
          // evaluating each expression passed to the scope
          expArgs.foreach( _.eval )
          // closing the scope, switching back to the scope's parent
          currentEnvironment(0) = newScope.scopeParent
        else
          // find the nested scope in children map if already there
          val nestedScope: Scope = currentEnvironment(0).childScopes(scopeName)
          // switching the current environment to the existing nested scope
          currentEnvironment(0) = nestedScope
          // evaluating each expression passed to the scope
          expArgs.foreach( _.eval )
          // closing the scope, switching back to the scope's parent
          currentEnvironment(0) = nestedScope.scopeParent

      // UnnamedScope Expression Implementation
      case UnnamedScope(expArgs*) =>
        // Create a new scope as anonymous scopes can't be referred again
        val newScope: Scope = new Scope(null, currentEnvironment(0))
        // switching the current environment to the new scope
        currentEnvironment(0) = newScope
        // evaluating each expression passed to the scope
        expArgs.foreach( _.eval )
        // closing the scope, switching back to the scope's parent
        currentEnvironment(0) = newScope.scopeParent

      // SetIdentifier Expression Implementation
      case SetIdentifier(setExpArgs*) =>
        // create a new set and add evaluation of each setExpression into it
        val newSet: SetType = mutable.Set()
        setExpArgs.foreach(newSet += _.eval)
        newSet

      // Union Expression Implementation
      case Union(s1, s2) => s1.eval.asInstanceOf[SetType] | s2.eval.asInstanceOf[SetType]

      // Intersection Expression Implementation
      case Intersection(s1, s2) => s1.eval.asInstanceOf[SetType] & s2.eval.asInstanceOf[SetType]

      // SetDifference Expression Implementation
      case SetDifference(s1, s2) => s1.eval.asInstanceOf[SetType] &~ s2.eval.asInstanceOf[SetType]

      // SymDifference Expression Implementation
      case SymDifference(s1, s2) =>
        val set1 = s1.eval.asInstanceOf[SetType]
        val set2 = s2.eval.asInstanceOf[SetType]
        (set1 | set2) &~ (set1 & set2)

      // CartesianProduct Expression Implementation
      case CartesianProduct(s1, s2) =>
        for {x <- s1.eval.asInstanceOf[SetType]; y <- s2.eval.asInstanceOf[SetType]} yield (x, y)

      // InsertInto Expression Implementation
      case InsertInto(setExp, setExpArgs*) =>
        // find the set stored in the variable and add evaluated values of each expression into it
        val storedSet = setExp.eval.asInstanceOf[SetType]
        setExpArgs.foreach(storedSet += _.eval)
        storedSet

      // DeleteFrom Expression Implementation
      case DeleteFrom(setExp, setExpArgs*) =>
        // find the set stored in the variable and remove evaluated values of each expression from it if they are already present in the set
        val storedSet = setExp.eval.asInstanceOf[SetType]
        setExpArgs.foreach( i => storedSet.remove(i.eval))

      // Contains Expression Implementation
      case Contains(setExp, valExp) =>
        val set = setExp.eval.asInstanceOf[SetType]
        set.contains(valExp.eval)

      // Equals Expression Implementation
      case Equals(exp1, exp2) => exp1.eval.equals(exp2.eval)
    }

  /**
   * Main Function, entry point to the application
   */
  @main def runSetTheoryDSL(): Unit = {
    println("program runs successfully")
  }

}
