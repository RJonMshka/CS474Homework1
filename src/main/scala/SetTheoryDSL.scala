/** Important imports */
import SetTheoryDSL.SetExpression.{Constructor, CreatePrivateField, CreateProtectedField, CreatePublicField, Param, PublicMethod, SetFieldFromObject}
import SetTheoryDSL.mutMapSetExp
import com.sun.org.apache.bcel.internal.ExceptionConst.EXCS

import collection.mutable
import scala.annotation.tailrec
import scala.language.postfixOps

/** Factory for [[SetTheoryDSL]] instances. */
object SetTheoryDSL {

  /** Create a name for type mutable sets */
  type SetType = mutable.Set[Any]
  type SetStringType = mutable.Set[String]
  type mutMapAny = mutable.Map[String, Any]
  type mutMapSetExp = mutable.Map[String, SetExpression]
  private type methodMapType = mutable.Map[String, MethodStruct]

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
    val bindingEnvironment: mutMapAny = mutable.Map()
    val classes: mutable.Map[String, ClassStruct] = mutable.Map()
  }

  private def createClassFieldsMap(): Map[String, SetStringType] =
    Map(
      "defaultFields" -> mutable.Set(),
      "publicFields" -> mutable.Set(),
      "protectedFields" -> mutable.Set(),
      "privateFields" -> mutable.Set()
    )

  private def createClassConstructorMap(constructor: MethodStruct = null): methodMapType =
    mutable.Map[String, MethodStruct](
      "constructor" -> constructor
    )

  private def createClassMethodsMap(): Map[String, SetStringType] =
    Map(
      "defaultMethods" -> mutable.Set(),
      "publicMethods" -> mutable.Set(),
      "protectedMethods" -> mutable.Set(),
      "privateMethods" -> mutable.Set()
    )


  private class ClassStruct(
    cName: String,
    constructor: methodMapType,
    fields: Map[String, SetStringType],
    methods: Map[String, SetStringType],
    innerClasses: mutable.Map[String, ClassStruct],
    parent: ClassStruct
  ) {
    val className = cName
    val classConstructor: methodMapType = constructor
    val classFieldTypes: Map[String, SetStringType] = fields
    val classFieldNames = mutable.Set[String]()
    val classMethodsTypes: Map[String, SetStringType] = methods
    val classMethodMap = mutable.Map[String, MethodStruct]()
    val memberClasses: mutable.Map[String, ClassStruct] = innerClasses
    val parentClass: ClassStruct = parent
  }



  private class MethodStruct(
    args: SetExpression,
    body: Seq[SetExpression]
  ) {
    val argExp = args
    val methodBody = body
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


  private class ObjectStruct(
    classRef: ClassStruct,
    constructorArgs: Seq[SetExpression]
  ) {
    val objectClass = classRef
    val paramValues: Seq[Any] = (for p <- constructorArgs yield p.eval)
    var fieldsMap: mutable.Map[String, Any] = mutable.Map()
    executeConstructor(objectClass, paramValues)

    private def executeConstructor(classRef: ClassStruct, paramValues: Seq[Any]): Unit =
      if classRef.parentClass != null then executeConstructor(classRef.parentClass, paramValues)
      // recursively go to the top most class

      // clearing the current fieldmap - will for populated for the top most object - fields are not inherited
      fieldsMap.clear()
      // initializing all fields to a value of zero
      classRef.classFieldNames.foreach(fieldsMap.put( _, 0))
      // these names can be different for different super constructors
      val paramNames = classRef.classConstructor("constructor").argExp.eval.asInstanceOf[Seq[Any]]
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      // but their number should be same
      if paramNames.size != paramValues.size then
        throw new Exception("Number of params do not match")
      else
        for i <- 0 until paramNames.size do paramsMap += ( paramNames(i).asInstanceOf[String] -> paramValues(i) )

      // creating a constructor scope
      // switching the current environment to the new scope
      currentEnvironment(index) = new Scope(null, currentEnvironment(index))

      // adding this ref to scope
      currentEnvironment(index).bindingEnvironment.put("this", this)

      // adding params map to current referencing env
      currentEnvironment(index).bindingEnvironment ++= paramsMap
      // Evaluating every expression in that constructor
      classRef.classConstructor("constructor").methodBody.foreach( _.eval )

      // once done executing - no need to remove the params from currentEnv
      currentEnvironment(index) = currentEnvironment(index).scopeParent

    // this method is for invoking class methods from within the body of the class or object
    // this means that the method is either called from constructor or some other method from outside
    def invokeMethod(mName: String, mParams: Seq[SetExpression], isCalledFromOutside: Boolean): Any =
      // have to do Dynamic dispatch here
      val methodToCall = dynamicDispatch(mName, objectClass, isCalledFromOutside)
      // creating a scope for particular method
      val methodParamNames = methodToCall.argExp.eval.asInstanceOf[Seq[Any]]
      val methodParamValues: Seq[Any] = (for p <- mParams yield p.eval)
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      if methodParamNames.size != methodParamValues.size then
        throw new Exception("Number of params do not match")
      else
        for i <- 0 until methodParamNames.size do paramsMap += ( methodParamNames(i).asInstanceOf[String] -> methodParamValues(i) )

      currentEnvironment(index) = new Scope(null, currentEnvironment(index))

      // adding object ref to scope
      currentEnvironment(index).bindingEnvironment.put("this", this)

      // adding params map to current referencing env
      currentEnvironment(index).bindingEnvironment ++= paramsMap
      // Evaluating every expression in that constructor
      val lastCallReturn: mutable.Map[String, Any] = mutable.Map()
      for expIndex <- 0 until methodToCall.methodBody.size do
        val evaluatedExp = methodToCall.methodBody(expIndex).eval

        // handle last call case
        if expIndex == methodToCall.methodBody.size - 1 then
          lastCallReturn.put("return", evaluatedExp)

      // once done executing
      currentEnvironment(index) = currentEnvironment(index).scopeParent
      // return the value
      lastCallReturn("return")

    private def dynamicDispatch(mName: String, classRef: ClassStruct, isCalledFromOutside: Boolean): MethodStruct =
      if !classRef.classMethodMap.keys.toSet.contains(mName) && classRef.parentClass != null then
        dynamicDispatch(mName, classRef.parentClass, isCalledFromOutside)
      else if classRef.classMethodMap.keys.toSet.contains(mName) && isCalledFromOutside then
        // check if either default or private member
        if classRef.classMethodsTypes("defaultMethods").contains(mName) || classRef.classMethodsTypes("privateMethods").contains(mName) then
          throw new Exception("cannot access private or default method from an object")
        else
          classRef.classMethodMap(mName)
      else if classRef.classMethodMap.keys.toSet.contains(mName) && !isCalledFromOutside then
        classRef.classMethodMap(mName)
      else
        throw new Exception("method not found")

    def getField(fName: String, requestFromOutside: Boolean): Any =
      // fields are not going to be inherited so no need to look up the class chain
      if !objectClass.classFieldNames.contains(fName) then
        throw new Exception("no such field exist")
      else if requestFromOutside && (objectClass.classFieldTypes("defaultFields").contains(fName) || objectClass.classFieldTypes("privateFields").contains(fName)) then
        // it means that access to private and default are forbidden
        throw new Exception("access to private or default fields from outside is not permitted")
      else
        fieldsMap(fName)

    def setField(fName: String, value: Any, requestFromOutside: Boolean): Unit =
      try {
        getField(fName, requestFromOutside)
      } catch {
        case e: Exception => throw e
      }
      fieldsMap.put(fName, value)

    def getInnerClass(cName: String): ClassStruct =
      if objectClass.memberClasses.get(cName).isEmpty then
        throw new Error("nested class not found")
      else
        objectClass.memberClasses(cName)
  }

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

    case ClassDef(className: String, classExpArgs: SetExpression*)

    case ClassDefThatExtends(cName: String, superClass: SetExpression.ClassRef, classExpArgs: SetExpression*)

    case ClassRef(className: String)

    case ClassRefFromObject(className: String, objRef: SetExpression)

    case ClassRefFromClass(className: String, classRef: SetExpression)

    case Param(s: String)

    case ParamsExp(paramExpArgs: SetExpression*)

    case Constructor(ParamsExp: SetExpression, cBodyExpArgs: SetExpression*)

    case Field(fieldName: String)

    case FieldFromObject(fieldName: String, obj: SetExpression)

    case SetField(fieldName: String, exp: SetExpression)

    case SetFieldFromObject(fieldName: String, obj: SetExpression, exp: SetExpression)

    case CreateField(fieldName: String)

    case CreatePublicField(fieldName: String)

    case CreateProtectedField(fieldName: String)

    case CreatePrivateField(fieldName: String)

    case InvokeMethod(methodName: String, params: SetExpression*)

    case InvokeMethodOfObject(mName: String, objRef: SetExpression, params: SetExpression*)

    case Method(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    case PublicMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    case ProtectedMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    case PrivateMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    case NewObject(classRef: SetExpression.ClassRef, constructorArgs: SetExpression*)


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

      case ClassRef(cName) =>
        if currentEnvironment(0).classes.get(cName).isEmpty then
          throw new Exception("Class does not exists")
        else
          currentEnvironment(0).classes(cName)

      case ClassRefFromObject(cName, objRef) =>
        val objectToRef = objRef.eval.asInstanceOf[ObjectStruct]
        objectToRef.getInnerClass(cName)

      case ClassRefFromClass(cName, classRef) =>
        val outerClassRef = classRef.eval.asInstanceOf[ClassStruct]
        if outerClassRef.memberClasses.get(cName).isEmpty then
          throw new Exception("inner class not found")
        else
          outerClassRef.memberClasses(cName)

      case ClassDef(cName, clsExpArgs*) =>
        if currentEnvironment(0).classes.get(cName).isEmpty then
          val newClass = this.declareClass(cName, null, clsExpArgs)
          currentEnvironment(0).classes.put(cName, newClass)
        else
          throw new Exception("Class already exists")

      case ClassDefThatExtends(cName, sClass, clsExpArgs*) =>
        if currentEnvironment(0).classes.get(cName).isEmpty then
          val superClassRef = sClass.eval.asInstanceOf[ClassStruct]
          val newClass = this.declareClass(cName, superClassRef, clsExpArgs)
          currentEnvironment(0).classes.put(cName, newClass)
        else
          throw new Exception("Class already exists")


      case ParamsExp(pExpArgs*) =>
        val params = for p <- pExpArgs yield p.eval
        params

      case Param(s) => s

      case NewObject(classRef, cArgs*) =>
        val newObject = ObjectStruct(classRef.eval.asInstanceOf[ClassStruct], cArgs)
        newObject

      case InvokeMethod(mName, params*) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, false)

      case InvokeMethodOfObject(mName, objectRef, params*) =>
        val currentObject = objectRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, true)

      case Field(fName) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].getField(fName, false)

      case FieldFromObject(fName, objRef) =>
        val currentObject = objRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].getField(fName, true)

      case SetField(fName, exp) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, false)

      case SetFieldFromObject(fName, objRef, exp) =>
        val currentObject = objRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, true)
    }

    private def resolveClassMembers(classRef: ClassStruct): Any = this match {
      case Constructor(pExp, body*) =>
        classRef.classConstructor.put("constructor", MethodStruct(pExp, body))
      case CreateField(fName) =>
        classRef.classFieldTypes("defaultFields").add(fName)
        classRef.classFieldNames.add(fName)
      case CreatePublicField(fName) =>
        classRef.classFieldTypes("publicFields").add(fName)
        classRef.classFieldNames.add(fName)
      case CreateProtectedField(fName) =>
        classRef.classFieldTypes("protectedFields").add(fName)
        classRef.classFieldNames.add(fName)
      case CreatePrivateField(fName) =>
        classRef.classFieldTypes("privateFields").add(fName)
        classRef.classFieldNames.add(fName)
      case Method(mName, args, body*) =>
        classRef.classMethodsTypes("defaultMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))
      case PublicMethod(mName, args, body*) =>
        classRef.classMethodsTypes("publicMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))
      case ProtectedMethod(mName, args, body*) =>
        classRef.classMethodsTypes("protectedMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))
      case PrivateMethod(mName, args, body*) =>
        classRef.classMethodsTypes("privateMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))
      case ClassDef(cName, clsExpArgs*) =>
        val innerClass = this.declareClass(cName, null, clsExpArgs)
        classRef.memberClasses.put(cName, innerClass)
      case ClassDefThatExtends(cName, sClass, clsExpArgs*) =>
        val superClassRef = sClass.eval.asInstanceOf[ClassStruct]
        val innerClass = this.declareClass(cName, superClassRef, clsExpArgs)
        classRef.memberClasses.put(cName, innerClass)
      case ClassRef(cName) => this.eval
    }

    private def declareClass(cName: String, parent: ClassStruct, classBody: Seq[SetExpression]): ClassStruct =
      val constructorMap: methodMapType = createClassConstructorMap()
      val fieldsMap: Map[String, SetStringType] = createClassFieldsMap()
      val methodsMap: Map[String, SetStringType] = createClassMethodsMap()
      val nestedClasses: mutable.Map[String, ClassStruct] = mutable.Map()
      val newClassRef = ClassStruct(
        cName,
        constructorMap,
        fieldsMap,
        methodsMap,
        nestedClasses,
        parent
      )
      classBody.foreach( _.resolveClassMembers(newClassRef) )
      newClassRef


  /**
   * Main Function, entry point to the application
   */
  @main def runSetTheoryDSL(): Unit = {
    import SetExpression.*

    ClassDef(
      "TopClass",
      CreatePublicField("f1"),
      CreatePublicField("f2"),
      Constructor(
        ParamsExp(Param("x"), Param("y")),
        SetField("f1", Variable("x")),
        SetField("f2", Variable("y"))
      ),
      PublicMethod(
        "set_f_to_params",
        ParamsExp(Param("a"), Param("b")),
        SetField("f1", Variable("a")),
        SetField("f1", Variable("b"))
      ),
      PublicMethod(
        "get_f1",
        ParamsExp(),
        Field("f1")
      ),
      PublicMethod(
        "get_f2",
        ParamsExp(),
        Field("f2")
      )

    ).eval

    Assign("obj1", NewObject( ClassRef("TopClass"), Value(1), Value(2) )).eval
    println( InvokeMethodOfObject("get_f1", Variable("obj1")).eval )
    println("program runs successfully")
  }

}
