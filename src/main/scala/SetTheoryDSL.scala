/** Important imports */
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

  /**
   * Creating private variables, a way to maintain pointer to current referencing environment
   */
  private val currentEnvironment: Array[Scope] = new Array[Scope](1)
  private val index = 0
  private val globalScopeName = "globalScope"
  // Creating a global scope whose parent is null
  currentEnvironment(index) = new Scope("globalScope", null)


  /** A Scope which is used to create a binding environment
   *
   * @constructor create a new Scope with a name and its parent scope.
   * @param name   the scope's name
   * @param parent a pointer to the scope's parent scope
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

  /**
   * Data Structure for storing Classes
   * @param cName - name of class
   * @param constructor - constructor for class - a map with single key "constructor"
   * @param fields - fields map with access modifiers as keys
   * @param methods - methods map with access modifiers as keys
   * @param innerClasses - inner classes declared in the body of class
   * @param parent - parent of the class, from which thr class is inherited
   */
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

  /**
   * Data Structure to store methods
   * @param args - Args SetExpression - signature of method
   * @param body - Instruction SetExpression Sequence/body of method
   */
  private class MethodStruct(args: SetExpression, body: Seq[SetExpression]) {
    val argExp: SetExpression = args
    val methodBody: Seq[SetExpression] = body
  }

  /**
   * Class ObjectStruct - class for creating objects
   * @param classRef: Reference to class for creating the object
   * @param constructorArgs - Sequence of SetExpressions which are argument constructs for DSL object's constructor
   */
  private class ObjectStruct(classRef: ClassStruct, constructorArgs: Seq[SetExpression]) {
    val objectClass: ClassStruct = classRef
    val paramValues: Seq[Any] = for p <- constructorArgs yield p.eval
    val fieldsMap: mutable.Map[String, Any] = mutable.Map()
    val inheritedFieldMap: mutable.Map[String, Any] = mutable.Map()
    val publicFields: mutable.Set[String] = mutable.Set()
    val protectedFields: mutable.Set[String] = mutable.Set()
    // call the execute constructor method
    executeConstructor(objectClass, paramValues)

    /**
     * Method to replicate mechanism for object's instantiation by calling h
     * @param classRef: reference of class whose constructor needs to be called
     * @param paramValues: param values for class's constructor
     */
    private def executeConstructor(classRef: ClassStruct, paramValues: Seq[Any]): Unit =
      // recursively go to the top most class
      if classRef.parentClass != null then executeConstructor(classRef.parentClass, paramValues)

      // clear fieldsMap map
      fieldsMap.clear()
      // initializing all fields to a value of zero - default value
      classRef.classFieldNames.foreach(fieldsMap.put(_, 0))
      // add inherited fields to fields map - from previous recursion cycle
      fieldsMap ++= inheritedFieldMap
      // store the
      publicFields ++= classRef.classFieldTypes("publicFields")
      protectedFields ++= classRef.classFieldTypes("protectedFields")

      // these names can be different for different super constructors
      val paramNames = classRef.classConstructor("constructor").argExp.eval.asInstanceOf[Seq[Any]]
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      // but their number should be same
      if paramNames.size != paramValues.size then
        throw new Exception(paramValues.size + " number of params do not match the method signature")
      else
        for i <- paramNames.indices do paramsMap += (paramNames(i).asInstanceOf[String] -> paramValues(i))

      // creating a constructor scope
      // switching the current environment to the new scope
      currentEnvironment(index) = new Scope(null, currentEnvironment(index))

      // adding this ref to scope
      currentEnvironment(index).bindingEnvironment.put("this", this)

      // adding params map to current referencing env
      currentEnvironment(index).bindingEnvironment ++= paramsMap
      // Evaluating every expression in that constructor - this might have changed value of some of the fields
      classRef.classConstructor("constructor").methodBody.foreach(_.eval)

      // updating the inherited field map and public/protected inherited set
      classRef.classFieldTypes("publicFields").foreach(key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )
      classRef.classFieldTypes("protectedFields").foreach(key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )


      // once done executing - no need to remove the params from currentEnv
      currentEnvironment(index) = currentEnvironment(index).scopeParent
      // clearing the fieldMap - only inheritedMap needs to be retained

    /**
     * Construct for invoking object's method
     * @param mName - method name to be invoked
     * @param mParams - params to be passed for method
     * @param isCalledFromOutside - represents whether invoked from outside the class's body or not
     * @return
     */
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
        for i <- methodParamNames.indices do paramsMap += (methodParamNames(i).asInstanceOf[String] -> methodParamValues(i))

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
      // remove the scope of this
      currentEnvironment(index).bindingEnvironment -= "this"
      // once done executing
      currentEnvironment(index) = currentEnvironment(index).scopeParent
      // return the value
      lastCallReturn("return")

    /**
     * Mechanism for dynamic dispatch of methods
     * @param mName - method name to be dynamically dispatched
     * @param classRef - current class in the recursion chain
     * @param isCalledFromOutside - represents if method is not called from class's body
     * @return MethodStruct
     */
    @tailrec
    private def dynamicDispatch(mName: String, classRef: ClassStruct, isCalledFromOutside: Boolean): MethodStruct =
      // if not part of the current class, go to its parent class
      if !classRef.classMethodMap.keys.toSet.contains(mName) && classRef.parentClass != null then
        dynamicDispatch(mName, classRef.parentClass, isCalledFromOutside)
      else if classRef.classMethodMap.keys.toSet.contains(mName) && isCalledFromOutside then
      // check if in default, private or protected method and is called from outside
        if !classRef.classMethodsTypes("publicMethods").contains(mName) then
          throw new Exception("cannot access private or default method from an object")
        else
          classRef.classMethodMap(mName)
      else if classRef.classMethodMap.keys.toSet.contains(mName) && !isCalledFromOutside then
        if classRef.classMethodsTypes("defaultMethods").contains(mName) || classRef.classMethodsTypes("privateMethods").contains(mName) then
          throw new Exception("default and private methods cannot be inherited")
        else
        // can access public and protected methods from inside
          classRef.classMethodMap(mName)
      else
        throw new Exception("method not found")

    /**
     * This method returns the field for an object
     * @param fName - name of field
     * @param requestFromOutside - represents if field's access is requested outside of class's body
     * @return
     */
    def getField(fName: String, requestFromOutside: Boolean): Any =
    // if not in current set of fields and also not in any inherited public and protected fields
      if !(fieldsMap.keys.toSet.contains(fName) || publicFields.contains(fName) || protectedFields.contains(fName)) then
        throw new Exception("No such field Exist")
      // it means that the field is in current set or inherited public and protected fields
      else if !(objectClass.classFieldTypes("publicFields").contains(fName) || publicFields.contains(fName)) then
      // it means the field must be protected, default or private
        if requestFromOutside then
          throw new Exception("access to default, private and public fields from outside is not permitted")
        else fieldsMap(fName)
      else fieldsMap(fName)

    /**
     * This method sets a value for field
     * @param fName - name of field
     * @param value - value to be set in the field
     * @param requestFromOutside represents if field's access is requested outside of class's body
     */
    def setField(fName: String, value: Any, requestFromOutside: Boolean): Unit =
      try {
        getField(fName, requestFromOutside)
      } catch {
        case e: Exception => throw e
      }
      fieldsMap.put(fName, value)

    /**
     * Return inner class of a particular name
     * @param cName - name of inner class requested
     * @return ClassStruct - inner class
     */
    def getInnerClass(cName: String): ClassStruct =
      if !objectClass.memberClasses.contains(cName) then
        throw new Error("nested class not found")
      else
        objectClass.memberClasses(cName)

    /**
     * Return a Boolean representing the object is an instance of a particular class
     * @param cRef - class reference for match
     * @return
     */
    def isInstanceOf(cRef: ClassStruct): Boolean =
      cRef == objectClass
  }


  /**
   * Helper method: createClassFieldsMap
   * Create map data structure for representing fields in class
   * @return Map of String -> Set, mapping fields with different access modifiers in single set
   */
  private def createClassFieldsMap(): Map[String, SetStringType] =
    Map(
      "defaultFields" -> mutable.Set(),
      "publicFields" -> mutable.Set(),
      "protectedFields" -> mutable.Set(),
      "privateFields" -> mutable.Set()
    )

  /**
   * Helper method: createClassConstructorMap
   * Create map data structure for representing constructor in class
   * @return Map of String -> MethodStruct (constructor implementation)
   */
  private def createClassConstructorMap(constructor: MethodStruct = null): methodMapType =
    mutable.Map[String, MethodStruct](
      "constructor" -> constructor
    )

  /**
   * Helper method: createClassMethodsMap
   * Create map data structure for representing methods in class
   * @return Map of String -> Set[String](method names)
   */
  private def createClassMethodsMap(): Map[String, SetStringType] =
    Map(
      "defaultMethods" -> mutable.Set(),
      "publicMethods" -> mutable.Set(),
      "protectedMethods" -> mutable.Set(),
      "privateMethods" -> mutable.Set()
    )


  /** Returns data on type Any stored in referencing environment and if not found, then looks into parent scope's environment
   *
   * @param varName  name of the variable to find
   * @param scopeEnv Scope of the environment where this function needs to find the variable
   */
  @tailrec
  private def getVariable(varName: String, scopeEnv: Scope): Any =
    if !(!scopeEnv.bindingEnvironment.contains(varName) && scopeEnv.scopeParent != null) then
      scopeEnv.bindingEnvironment(varName)
    else
      getVariable(varName, scopeEnv.scopeParent)

  /**
   * This method finds and returns a reference to class declared in a particular scope
   *
   * @param cName    : String - name of the class
   * @param scopeEnv : Scope - current referencing environment
   * @return ClassStruct
   */
  @tailrec
  private def getClassRef(cName: String, scopeEnv: Scope): ClassStruct =
    if scopeEnv.classes.contains(cName) then
    // class exist in this scope
      scopeEnv.classes(cName)
    else if scopeEnv.scopeParent == null then
      null
    else
      getClassRef(cName, scopeEnv.scopeParent)


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

    /**
     * ClassDef Expression
     * Defines a class with a class name and set of expression which are part of class's body or can be thought of as class members
     */
    case ClassDef(className: String, classExpArgs: SetExpression*)

    /**
     * ClassDefThatExtends Expression
     * Defines a class similar to above, but also Extends or inherit another class (passed as second argument)
     * Only Single class inheritance is allowed
     */
    case ClassDefThatExtends(cName: String, superClass: SetExpression, classExpArgs: SetExpression*)

    /**
     * ClassRef Expression
     * Gives reference to class whose name is provided as argument
     */
    case ClassRef(className: String)

    /**
     * ClassRefFromObject Expression
     * Refer to a class which is declared as an inner class of the class that the object (evaluating objRef) is instantiated with
     */
    case ClassRefFromObject(className: String, objRef: SetExpression)

    /**
     * ClassRefFromClass Expression
     * Refer to a class which is declared as an inner class of the classRef class reference
     */
    case ClassRefFromClass(className: String, classRef: SetExpression)

    /**
     * Param Expression
     * Represent single param for building methods and constructors
     */
    case Param(s: String)

    /**
     * ParamsExp Expression
     * Represents a set of params
     */
    case ParamsExp(paramExpArgs: SetExpression*)

    /**
     * Constructor Expression
     * Defines a constructor for class with paramExp and other arguments as expressions as part of constructor's instructions
     */
    case Constructor(ParamsExp: SetExpression, cBodyExpArgs: SetExpression*)

    /**
     * Field Expression
     * Return a class field referred with string inside class Body
     */
    case Field(fieldName: String)

    /**
     * FieldFromObject Expression
     * Returns a class field referred from object or outside the class's body
     */
    case FieldFromObject(fieldName: String, obj: SetExpression)

    /**
     * SetField Expression
     * This is used to set or change the value of a particular field, fieldName: name of the field to be set and exp evaluates to a value that needs to be put the field
     */
    case SetField(fieldName: String, exp: SetExpression)

    /**
     * SetFieldFromObject Expression
     * Similar to set fields, but is used to set a field from an object's reference / outside the class's body
     */
    case SetFieldFromObject(fieldName: String, obj: SetExpression, exp: SetExpression)

    /**
     * CreateField Expression
     * Creates a field for class - can only be accessed within class body and cannot be inherited (same as private)
     */
    case CreateField(fieldName: String)

    /**
     * CreatePublicField Expression
     * Creates a field with "public" access modifier - can be accessed anywhere and also inheritable
     */
    case CreatePublicField(fieldName: String)

    /**
     * CreateProtectedField Expression
     * Creates a field with "protected" access modifier - can be accessed within the class body and also inheritable
     */
    case CreateProtectedField(fieldName: String)

    /**
     * CreatePrivateField Expression
     * Creates a field with "private" access modifier - can only be accessed within class body and cannot be inherited
     */
    case CreatePrivateField(fieldName: String)

    /**
     * InvokeMethod Expression
     * Invokes a method with its name and params from the class's body
     */
    case InvokeMethod(methodName: String, params: SetExpression*)

    /**
     * InvokeMethodOfObject Expression
     * Invokes a method with its name and params from the object/ calls method on the object returned by evaluating objRef Expression
     */
    case InvokeMethodOfObject(mName: String, objRef: SetExpression, params: SetExpression*)

    /**
     * Method Expression
     * Defines a method with default access - same as private - cannot be referenced by any instance outside the class's body and is not a candidate for dynamic dispatch
     */
    case Method(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    /**
     * PublicMethod Expression
     * Defines a "public" access level method - can be accessed from within class's body as well as from instance object, also candidate for dynamic dispatch
     */
    case PublicMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    /**
     * ProtectedMethod Expression
     * Defines a "protected" access level method - can be accessed from within class's body only, also candidate for dynamic dispatch
     */
    case ProtectedMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    /**
     * PrivateMethod Expression
     * Defines a method with "private" access - cannot be referenced by any instance outside the class's body and is not a candidate for dynamic dispatch
     */
    case PrivateMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)

    /**
     * NewObject Expression
     * Return a new object by instantiating the class, classRef evaluates to a class's reference and constructorArgs are the set expression passed as arguments to constructor of Class
     * analogous to new ClassName(params)
     */
    case NewObject(classRef: SetExpression, constructorArgs: SetExpression*)

    /**
     * ObjectInstanceOf Expression
     * Return true if objectRef's evaluation is an instance or object created by instantiating classRef's evaluation
     */
    case ObjectInstanceOf(objectRef: SetExpression, classRef: SetExpression)

    /** This method evaluates SetExpressions
     * Description - The body of this method is the implementation of above abstract data types
     *
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
          expArgs.foreach(_.eval)
          // closing the scope, switching back to the scope's parent
          currentEnvironment(0) = newScope.scopeParent
        else
          // find the nested scope in children map if already there
          val nestedScope: Scope = currentEnvironment(0).childScopes(scopeName)
          // switching the current environment to the existing nested scope
          currentEnvironment(0) = nestedScope
          // evaluating each expression passed to the scope
          expArgs.foreach(_.eval)
          // closing the scope, switching back to the scope's parent
          currentEnvironment(0) = nestedScope.scopeParent

      // UnnamedScope Expression Implementation
      case UnnamedScope(expArgs*) =>
        // Create a new scope as anonymous scopes can't be referred again
        val newScope: Scope = new Scope(null, currentEnvironment(0))
        // switching the current environment to the new scope
        currentEnvironment(0) = newScope
        // evaluating each expression passed to the scope
        expArgs.foreach(_.eval)
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
        setExpArgs.foreach(i => storedSet.remove(i.eval))

      // Contains Expression Implementation
      case Contains(setExp, valExp) =>
        val set = setExp.eval.asInstanceOf[SetType]
        set.contains(valExp.eval)

      // Equals Expression Implementation
      case Equals(exp1, exp2) => exp1.eval.equals(exp2.eval)

      // Returns a reference to the class in current scope
      case ClassRef(cName) =>
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          throw new Exception(cName + " class does not exists.")
        else
          clsRef

      // Returns the reference to class from an instantiated object
      case ClassRefFromObject(cName, objRef) =>
        val objectToRef = objRef.eval.asInstanceOf[ObjectStruct]
        objectToRef.getInnerClass(cName)

      // Returns the reference to class from an outer/enclosing object
      case ClassRefFromClass(cName, classRef) =>
        val outerClassRef = classRef.eval.asInstanceOf[ClassStruct]
        if outerClassRef.memberClasses.get(cName).isEmpty then
          throw new Exception(cName + " : no such inner class found")
        else
          outerClassRef.memberClasses(cName)

      // Class definition - check if class not declared already
      case ClassDef(cName, clsExpArgs*) =>
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          val newClass = this.declareClass(cName, null, clsExpArgs)
          currentEnvironment(0).classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")

      // Class definition - with super class/Extends/inheriting the super class
      case ClassDefThatExtends(cName, sClass, clsExpArgs*) =>
        val clsRef = getClassRef(cName, currentEnvironment(index))
        if clsRef == null then
          val superClassRef = sClass.eval.asInstanceOf[ClassStruct]
          val newClass = this.declareClass(cName, superClassRef, clsExpArgs)
          currentEnvironment(index).classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")

      // Params expression - used to specify params to methods and constructor
      case ParamsExp(pExpArgs*) =>
        val params = for p <- pExpArgs yield p.eval
        params

      // Param Expression
      case Param(s) => s

      // NewObject Expression - returns new object
      case NewObject(classRef, cArgs*) =>
        val newObject = ObjectStruct(classRef.eval.asInstanceOf[ClassStruct], cArgs)
        newObject

      // InvokeMethod Expression - used to call method from which the Class constructor and other methods
      case InvokeMethod(mName, params*) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, false)

      // InvokeMethodOfObject Expression
      case InvokeMethodOfObject(mName, objectRef, params*) =>
        val currentObject = objectRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, true)

      // Field Expression - Returns single field - analogous to this.field
      case Field(fName) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].getField(fName, false)

      // FieldFromObject Expression - Returns field from object, analogous to object.field
      case FieldFromObject(fName, objRef) =>
        val currentObject = objRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].getField(fName, true)

      // SetField Expression - Updates the value of the field, analogous to assigning value to this.field
      case SetField(fName, exp) =>
        val currentObject = currentEnvironment(index).bindingEnvironment("this")
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, false)

      // SetFieldFromObject Expression - Updates the value of the field by referencing the object from outside the clas body, analogous to assigning value to object.field
      case SetFieldFromObject(fName, objRef, exp) =>
        val currentObject = objRef.eval
        currentEnvironment(index).bindingEnvironment.put("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, true)

      // ObjectInstanceOf Expression - returns a Boolean which represents whether the object is an instantiation of a particular class or not
      case ObjectInstanceOf(objRef, clsRef) => objRef.eval.asInstanceOf[ObjectStruct].isInstanceOf(clsRef.eval.asInstanceOf[ClassStruct])
    }

    /**
     * This method resolves the Expressions which are members of the class, includes class field declaration, definiting constructor, definiting methods, defining inner/nested classes
     *
     * @param classRef
     * @return
     */
    private def resolveClassMembers(classRef: ClassStruct): Any = this match {
      // Constructor Expression - will not be evaluated separately
      case Constructor(pExp, body*) =>
        classRef.classConstructor.put("constructor", MethodStruct(pExp, body))

      // CreateField Expression
      case CreateField(fName) =>
        classRef.classFieldTypes("defaultFields").add(fName)
        classRef.classFieldNames.add(fName)
      // CreatePublicField Expression
      case CreatePublicField(fName) =>
        classRef.classFieldTypes("publicFields").add(fName)
        classRef.classFieldNames.add(fName)

      // CreateProtectedField Expression
      case CreateProtectedField(fName) =>
        classRef.classFieldTypes("protectedFields").add(fName)
        classRef.classFieldNames.add(fName)

      // CreatePrivateField Expression
      case CreatePrivateField(fName) =>
        classRef.classFieldTypes("privateFields").add(fName)
        classRef.classFieldNames.add(fName)

      // Method Expression
      case Method(mName, args, body*) =>
        classRef.classMethodsTypes("defaultMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))

      // PublicMethod Expression
      case PublicMethod(mName, args, body*) =>
        classRef.classMethodsTypes("publicMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))

      // ProtectedMethod Expression
      case ProtectedMethod(mName, args, body*) =>
        classRef.classMethodsTypes("protectedMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))

      // PrivateMethod Expression
      case PrivateMethod(mName, args, body*) =>
        classRef.classMethodsTypes("privateMethods").add(mName)
        classRef.classMethodMap.put(mName, MethodStruct(args, body))

      // ClassDef Expression
      case ClassDef(cName, clsExpArgs*) =>
        val innerClass = this.declareClass(cName, null, clsExpArgs)
        classRef.memberClasses.put(cName, innerClass)

      // ClassDefThatExtends Expression
      case ClassDefThatExtends(cName, sClass, clsExpArgs*) =>
        val superClassRef = sClass.eval.asInstanceOf[ClassStruct]
        val innerClass = this.declareClass(cName, superClassRef, clsExpArgs)
        classRef.memberClasses.put(cName, innerClass)

      // ClassRef Expression
      case ClassRef(cName) => this.eval
    }

    /**
     *
     * This method creates or declares the class and returns a ClassStruct Object
     *
     * @param cName     : String - class name to be created
     * @param parent    : ClassStruct - parent class which this class is trying to inherit
     * @param classBody : Sequence of SetExpression which are part of class body (listed in method resolveClassMembers)
     * @return
     */
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
      classBody.foreach(_.resolveClassMembers(newClassRef))
      newClassRef


  /**
   * Main Function, entry point to the application
   */
  @main def runSetTheoryDSL(): Unit = {

    println("Program runs successfully")
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

    Assign("obj1", NewObject(ClassRef("c1"))).eval
    Assign("obj2", NewObject(ClassRef("c2"))).eval
    println(FieldFromObject("f1", Variable("obj1")).eval)
    println(FieldFromObject("f1", Variable("obj2")).eval)
    println("second object")
    println(InvokeMethodOfObject("m1", Variable("obj2")).eval)
    println("third object")
    Assign("obj3", NewObject(ClassRefFromClass("c3", ClassRef("c2")))).eval
    println(FieldFromObject("f1", Variable("obj3")).eval)
    println("4th object")
    Assign("obj4", NewObject(ClassRefFromObject("c3", Variable("obj2")))).eval
    SetFieldFromObject("f1", Variable("obj4"), Value("bye")).eval
    println(FieldFromObject("f1", Variable("obj4")).eval)
    println(ObjectInstanceOf(Variable("obj4"), ClassRef("c2")).eval)
  }

}
