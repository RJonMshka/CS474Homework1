/** Important imports */
import SetTheoryDSL.SetExpression.{Constructor, CreatePrivateField, CreateProtectedField, CreatePublicField, Param, PublicMethod, SetFieldFromObject}
import SetTheoryDSL.mutMapSetExp

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
    val className: String = cName
    val classConstructor: methodMapType = constructor
    val classFieldTypes: Map[String, SetStringType] = fields
    val classFieldNames: mutable.Set[String] = mutable.Set()
    val classMethodsTypes: Map[String, SetStringType] = methods
    val classMethodMap: mutable.Map[String, MethodStruct] = mutable.Map()
    val memberClasses: mutable.Map[String, ClassStruct] = innerClasses
    val parentClass: ClassStruct = parent
  }



  private class MethodStruct(
    args: SetExpression,
    body: Seq[SetExpression]
  ) {
    val argExp: SetExpression = args
    val methodBody: Seq[SetExpression] = body
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

  @tailrec
  private def getClassRef(cName: String, scopeEnv: Scope): Any =
    if scopeEnv.classes.contains(cName) then
      // class exist in this scope
      scopeEnv.classes(cName)
    else if scopeEnv.scopeParent == null then
      null
    else
      getClassRef(cName, scopeEnv.scopeParent)


  private class ObjectStruct(
    classRef: ClassStruct,
    constructorArgs: Seq[SetExpression]
  ) {
    val objectClass: ClassStruct = classRef
    val paramValues: Seq[Any] = for p <- constructorArgs yield p.eval
    val fieldsMap: mutable.Map[String, Any] = mutable.Map()
    val inheritedFieldMap: mutable.Map[String, Any] = mutable.Map()
    val publicFields: mutable.Set[String] = mutable.Set()
    val protectedFields: mutable.Set[String] = mutable.Set()
    executeConstructor(objectClass, paramValues)

    private def executeConstructor(classRef1: ClassStruct, paramValues: Seq[Any]): Unit =
      if classRef1.parentClass != null then executeConstructor(classRef1.parentClass, paramValues)
      // recursively go to the top most class
      // initializing all fields to a value of zero
      fieldsMap.clear()
      classRef1.classFieldNames.foreach(fieldsMap.put( _, 0))
      fieldsMap ++= inheritedFieldMap
      // inheriting logic for public and protected fields
      publicFields ++= classRef1.classFieldTypes("publicFields")
      protectedFields ++= classRef1.classFieldTypes("protectedFields")
      // these names can be different for different super constructors
      val paramNames = classRef1.classConstructor("constructor").argExp.eval.asInstanceOf[Seq[Any]]
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      // but their number should be same
      if paramNames.size != paramValues.size then
        throw new Exception("Number of params do not match")
      else
        for i <- paramNames.indices do paramsMap += ( paramNames(i).asInstanceOf[String] -> paramValues(i) )

      // creating a constructor scope
      // switching the current environment to the new scope
      currentEnvironment(index) = new Scope(null, currentEnvironment(index))

      // adding this ref to scope
      currentEnvironment(index).bindingEnvironment.put("this", this)

      // adding params map to current referencing env
      currentEnvironment(index).bindingEnvironment ++= paramsMap
      // Evaluating every expression in that constructor - this might have changed value of some of the fields
      classRef1.classConstructor("constructor").methodBody.foreach( _.eval )

      // updating the inherited field map and public/protected inherited set
      classRef1.classFieldTypes("publicFields").foreach( key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )
      classRef1.classFieldTypes("protectedFields").foreach( key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )


      // once done executing - no need to remove the params from currentEnv
      currentEnvironment(index) = currentEnvironment(index).scopeParent
      // clearing the fieldMap - only inheritedMap needs to be retained

    // this method is for invoking class methods from within the body of the class or object
    // this means that the method is either called from constructor or some other method from outside
    def invokeMethod(mName: String, mParams: Seq[SetExpression], isCalledFromOutside: Boolean): Any =
      // have to do Dynamic dispatch here
      val methodToCall = dynamicDispatch(mName, objectClass, isCalledFromOutside)
      // creating a scope for particular method
      val methodParamNames = methodToCall.argExp.eval.asInstanceOf[Seq[Any]]
      val methodParamValues: Seq[Any] = for p <- mParams yield p.eval
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      if methodParamNames.size != methodParamValues.size then
        throw new Exception("Number of params do not match")
      else
        for i <- methodParamNames.indices do paramsMap += ( methodParamNames(i).asInstanceOf[String] -> methodParamValues(i) )

      currentEnvironment(index) = new Scope(null, currentEnvironment(index))

      // adding object ref to scope
      currentEnvironment(index).bindingEnvironment.put("this", this)

      // adding params map to current referencing env
      currentEnvironment(index).bindingEnvironment ++= paramsMap
      // Evaluating every expression in that constructor
      val lastCallReturn: mutable.Map[String, Any] = mutable.Map()
      for expIndex <- methodToCall.methodBody.indices do
        val evaluatedExp = methodToCall.methodBody(expIndex).eval

        // handle last call case
        if expIndex == methodToCall.methodBody.size - 1 then
          lastCallReturn.put("return", evaluatedExp)

      // once done executing
      currentEnvironment(index) = currentEnvironment(index).scopeParent
      // remove the scope of this
      currentEnvironment(index).bindingEnvironment -= "this"
      // return the value
      lastCallReturn("return")

    @tailrec
    private def dynamicDispatch(mName: String, classRef1: ClassStruct, isCalledFromOutside: Boolean): MethodStruct =
      if !classRef1.classMethodMap.keys.toSet.contains(mName) && classRef1.parentClass != null then
        dynamicDispatch(mName, classRef1.parentClass, isCalledFromOutside)
      else if classRef1.classMethodMap.keys.toSet.contains(mName) && isCalledFromOutside then
        // check if in default, private or protected method and is called from outside
        if !classRef1.classMethodsTypes("publicMethods").contains(mName) then
          throw new Exception("cannot access private or default method from an object")
        else
          classRef1.classMethodMap(mName)
      else if classRef1.classMethodMap.keys.toSet.contains(mName) && !isCalledFromOutside then
        if classRef1.classMethodsTypes("defaultMethods").contains(mName) || classRef1.classMethodsTypes("privateMethods").contains(mName) then
          throw new Exception("default and private methods cannot be inherited")
        else
          // can access public and protected methods from inside
          classRef1.classMethodMap(mName)
      else
        throw new Exception("method not found")

    def getField(fName: String, requestFromOutside: Boolean): Any =
      // if not in current set of fields and also not in any inherited public and protected fields
      if !( fieldsMap.keys.toSet.contains(fName) || publicFields.contains(fName) || protectedFields.contains(fName) ) then
        throw new Exception("No such field Exist")
      // it means that the field is in current set or inherited public and protected fields
      else if !(objectClass.classFieldTypes("publicFields").contains(fName) || publicFields.contains(fName)) then
        // it means the field must be protected, default or private
        if requestFromOutside then
          throw new Exception("access to default, private and public fields from outside is not permitted")
        else fieldsMap(fName)
      else fieldsMap(fName)

    def setField(fName: String, value: Any, requestFromOutside: Boolean): Unit =
      try {
        getField(fName, requestFromOutside)
      } catch {
        case e: Exception => throw e
      }
      fieldsMap.put(fName, value)

    def getInnerClass(cName: String): ClassStruct =
      if !objectClass.memberClasses.contains(cName) then
        throw new Error("nested class not found")
      else
        objectClass.memberClasses(cName)

    def isInstanceOf(cRef: ClassStruct): Boolean =
      cRef == objectClass
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

    case NewObject(classRef: SetExpression, constructorArgs: SetExpression*)

    case ObjectInstanceOf(objectRef: SetExpression, classRef: SetExpression)


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
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          throw new Exception(cName + " class does not exists.")
        else
          clsRef

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
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          val newClass = this.declareClass(cName, null, clsExpArgs)
          currentEnvironment(0).classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")

      case ClassDefThatExtends(cName, sClass, clsExpArgs*) =>
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          val superClassRef = sClass.eval.asInstanceOf[ClassStruct]
          val newClass = this.declareClass(cName, superClassRef, clsExpArgs)
          currentEnvironment(index).classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")


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

      case ObjectInstanceOf(objRef, clsRef) => objRef.eval.asInstanceOf[ObjectStruct].isInstanceOf(clsRef.eval.asInstanceOf[ClassStruct])
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

//    ClassDef(
//      "TopClass",
//      CreatePublicField("f1"),
//      CreatePublicField("f2"),
//      Constructor(
//        ParamsExp(Param("x"), Param("y")),
//        SetField("f1", Variable("x")),
//        SetField("f2", Variable("y"))
//      ),
//      PublicMethod(
//        "set_f_to_params",
//        ParamsExp(Param("a"), Param("b")),
//        SetField("f1", Variable("a")),
//        SetField("f2", Variable("b"))
//      )
//
//    ).eval
//
//    Assign("obj1", NewObject( ClassRef("TopClass"), Value(1), Value(2) )).eval
//    InvokeMethodOfObject("set_f_to_params", Variable("obj1"), Value(40), Value(50)).eval
//    println( FieldFromObject("f1", Variable("obj1")).eval )
//    println( FieldFromObject("f2", Variable("obj1")).eval )
//    println("program runs successfully")

    ClassDef(
      "c1",
      CreatePublicField("f1"),
      CreateProtectedField("f2"),
      CreatePrivateField("f3"),
      CreateField("f4"),
      Constructor(
        ParamsExp(),
        SetField("f1", Value(1)),
        SetField("f2", Value(2)),
        SetField("f3", Value(3)),
        SetField("f4", Value(4)),
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

    ClassDefThatExtends(
      "c2",
      ClassRef("c1"),
      CreatePublicField("f5"),
      CreateProtectedField("f6"),
      CreatePrivateField("f7"),
      CreateField("f8"),
      Constructor(
        ParamsExp(),
        SetField("f2", Value(30)),
        SetField("f5", Value(5)),
        SetField("f6", Value(6)),
        SetField("f7", Value(7)),
        SetField("f8", Value(8)),
      ),
      ClassDef(
        "c3",
        CreatePublicField("f1"),
        CreatePublicField("f5"),
        Constructor(
          ParamsExp(),
          SetField("f1", Value("hello")),
          SetField("f5", Value(55))
        )
      )
    ).eval

//    case ClassRefFromObject(className: String, objRef: SetExpression)
//
//    case ClassRefFromClass(className: String, classRef: SetExpression)

    Assign("obj1", NewObject( ClassRef("c1") )).eval
    Assign("obj2", NewObject( ClassRef("c2") )).eval
    println( FieldFromObject( "f1", Variable("obj1") ).eval )
    println( FieldFromObject( "f1", Variable("obj2") ).eval )
    println("second object")
    println( InvokeMethodOfObject( "m1", Variable("obj2") ).eval )
    println("third object")
    Assign("obj3", NewObject( ClassRefFromClass("c3", ClassRef("c2") ) ) ).eval
    println( FieldFromObject( "f1", Variable("obj3") ).eval )
    println("4th object")
    Assign("obj4", NewObject( ClassRefFromObject("c3", Variable("obj2") ) ) ).eval
    SetFieldFromObject("f1", Variable("obj4"), Value("bye")).eval
    println( FieldFromObject( "f1", Variable("obj4") ).eval )
    println( ObjectInstanceOf(Variable("obj4"), ClassRef("c2") ).eval )
  }

}
