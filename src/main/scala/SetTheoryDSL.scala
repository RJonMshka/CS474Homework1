/** Important imports */
import SetTheoryDSL.ImplProperties.Implemented
import SetTheoryDSL.{InterfaceStruct, SetExpression}
import SetTheoryDSL.SetExpression.{AbstractClassDef, ClassDef, ClassRefFromInterface, Constructor, CreatePublicField, InterfaceRef, NewObject, ParamsExp, SetField, Value}
import com.sun.corba.se.impl.naming.cosnaming.InternalBindingValue

import collection.mutable
import scala.annotation.tailrec

/** Factory for [[SetTheoryDSL]] instances. */
object SetTheoryDSL {

  /** Create a name for type mutable sets */
  type SetType = mutable.Set[Any]
  type SetStringType = mutable.Set[String]
  type mutMapAny = mutable.Map[String, Any]
  type mutMapSetExp = mutable.Map[String, SetExpression]
  private type methodMapType = mutable.Map[String, MethodStruct]

  /**
   * Defining types of access modifier
   */
  enum AccessProperties:
    case Public
    case Private
    case Protected
    case DefaultAccess

  /**
   * Implementation properties of methods
   */
  enum ImplProperties:
    case Default
    case Abstract
    case Implemented

  /**
   * Creating private variables, a way to maintain pointer to current referencing environment
   */
  private val currentEnvironment: mutable.Map[String, Any] = mutable.Map()
  private val index = 0
  private val globalScopeName = "globalScope"
  // Creating a global scope whose parent is null
  currentEnvironment.put("scope", new Scope("globalScope", null))
  // add the mechanism to add exception which is going to propagate through the scope chain
  currentEnvironment.put("propagatingException", null)


  /** A Scope which is used to create a binding environment
   *
   * @constructor create a new Scope with a name and its parent scope.
   * @param name   the scope's name
   * @param parent a pointer to the scope's parent scope
   */
  private class Scope(name: String, parent: Scope, val tryObject: TryStruct = null) {
    val scopeName: String = name
    val scopeParent: Scope = parent
    // This field holds the child Scope in a HashMap of particular Scope Object
    val childScopes: mutable.Map[String, Scope] = mutable.Map()
    // This field holds the binding environment (binding variables) in a HashMap referring to the referencing environment of that particular Scope object
    val bindingEnvironment: mutMapAny = mutable.Map()
    val classes: mutable.Map[String, ClassStruct] = mutable.Map()
    val interfaces: mutable.Map[String, InterfaceStruct] = mutable.Map()
  }

  private class TryStruct(val catchExpSeq: Seq[SetExpression.Catch])

  private def createNewScope(scope: Scope): Unit =
    currentEnvironment.put("scope", scope)

  private def getCurrentScope(): Scope =
    currentEnvironment("scope").asInstanceOf[Scope]

  private def addBindingToScope(bindingName: String, bindingValue: Any): Unit =
    currentEnvironment("scope").asInstanceOf[Scope].bindingEnvironment.put(bindingName, bindingValue)

  private def getBindingFromScope(bindingName: String): Any =
    currentEnvironment("scope").asInstanceOf[Scope].bindingEnvironment(bindingName)

  private def addMapToScope(map: mutMapAny): Unit =
    currentEnvironment("scope").asInstanceOf[Scope].bindingEnvironment ++= map

  private def switchToParentScope(): Unit =
    currentEnvironment.put("scope", currentEnvironment("scope").asInstanceOf[Scope].scopeParent)

  private def getParentScope(): Scope =
    currentEnvironment("scope").asInstanceOf[Scope].scopeParent

  private def removeBindingByKey(keyName: String): Unit =
    currentEnvironment("scope").asInstanceOf[Scope].bindingEnvironment -= keyName

  private def addExceptionToScope(exception: ObjectStruct): Unit =
    currentEnvironment.put("propagatingException", exception)

  private def removeExceptionFromScope(): Unit =
    currentEnvironment.put("propagatingException", null)

  private def getPropagatingException(): ObjectStruct =
    currentEnvironment("propagatingException").asInstanceOf[ObjectStruct]

  /**
   * Data Structure for storing interfaces
   * @param intName - Name of the interface
   * @param fields - a map of access modifier category string to field names
   */
  private class InterfaceStruct(intName: String, fields: Map[String, SetStringType]) {
    val interfaceName: String = intName
    val interfaceFieldTypes: Map[String, SetStringType] = fields
    val interfaceFieldNames: mutable.Set[String] = mutable.Set()
    val interfaceMethods: mutable.Set[MethodStruct] = mutable.Set()
    val interfaceRelations: mutable.Map[String, Any] = mutable.Map(
      "memberClasses" -> mutable.Map[String, ClassStruct](),
      "memberInterfaces" -> mutable.Map[String, InterfaceStruct](),
      "superInterface" -> null
    )
  }

  /**
   * Data Structure for storing Classes
   * @param cName - name of class
   * @param constructor - constructor for class - a map with single key "constructor"
   * @param fields - fields map with access modifiers as keys
   * @param isAbstract - a boolean value to represent whether the class is abstract or not
   */
  private class ClassStruct(
     cName: String,
     constructor: methodMapType,
     fields: Map[String, SetStringType],
     val isAbstract: Boolean
   ) {
    val className: String = cName
    val classConstructor: methodMapType = constructor
    val classFieldTypes: Map[String, SetStringType] = fields
    val classFieldNames: mutable.Set[String] = mutable.Set()
    val classMethods: mutable.Set[MethodStruct] = mutable.Set()
    val classRelations: mutable.Map[String, Any] = mutable.Map(
      "memberClasses" -> mutable.Map[String, ClassStruct](),
      "superClass" -> null,
      "superInterfaces" -> mutable.Set[InterfaceStruct](),
      "memberInterfaces" -> mutable.Map[String, InterfaceStruct](),
      "inheritedInterfaces" -> mutable.Set[InterfaceStruct]()
    )
    val abstractMethods: mutable.Set[MethodStruct] = mutable.Set()
    val defaultImplMethods: mutable.Set[MethodStruct] = mutable.Set()
    val implementedMethods: mutable.Set[MethodStruct] = mutable.Set()
    val localImplementedMethods: mutable.Set[MethodStruct] = mutable.Set()

    /**
     * This method add the superinterface to class relations
     * @param sInterface - superInterface reference to be added
     */
    def addSuperInterface(sInterface: InterfaceStruct): Unit =
      if classRelations("superInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].contains(sInterface) then throw new Exception("Same Interface cannot be implemented more than once in a single declaration")
      classRelations("superInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].add(sInterface)

    /**
     * This method adds method to class methods
     * @param m - method to be added
     */
    def addClassMethod(m: MethodStruct): Unit =
      // Exception handling while adding methods
      if getSetOfMethodNames(classMethods).contains(m.methodName) then throw new Exception(m.methodName + " already exists in the class")
      m.methodImplementation match {
        case ImplProperties.Default => throw new Exception("Cannot create default method in class")
        case ImplProperties.Abstract =>
          if !isAbstract then throw new Exception("A concrete class cannot have abstract methods")
          m.methodAccess match {
            case AccessProperties.Private => throw new Exception("An abstract method can only be declared public or protected")
            case AccessProperties.DefaultAccess => throw new Exception("An abstract method can only be declared public or protected")
            case _ =>
          }
        case _ =>
      }
      classMethods.add(m)

    /**
     * This method processes superinterfaces of a class and populates abstract and default methods of parent interfaces
     * @param sInterface - superinterface to undergo processing
     */
    private def processSuperInterface(sInterface: InterfaceStruct): Unit =
      // go to the top most interface
      if sInterface.interfaceRelations("superInterface") != null &&
        !classRelations("inheritedInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].contains(sInterface) then
        processSuperInterface(sInterface.interfaceRelations("superInterface").asInstanceOf[InterfaceStruct])

      // if inherited interfaces do not include the interface already, add that interface to inherited interface set and extract its abstract and default methods
      // Inherited interfaces are used so that an interface whose methods are already inherited or extracted will not be traversed again as it and its parents are already traversed
      if !classRelations("inheritedInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].contains(sInterface) then
        classRelations("inheritedInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].add(sInterface)
        val absMethodNames = getSetOfMethodNames(abstractMethods)
        val filteredAbsMethods = getInheritableAbstractMethods(sInterface.interfaceMethods).filter(m => !absMethodNames.contains(m.methodName))
        abstractMethods.addAll(filteredAbsMethods)

        // add/override default methods - should
        getInheritableDefaultMethods(sInterface.interfaceMethods).foreach(m => this.addDefaultMethod(m))

    /**
     * This method adds a default method to the default methods set
     * @param m - method which needs to be added
     */
    private def addDefaultMethod(m: MethodStruct): Unit =
      val filteredDefMethods = defaultImplMethods.filter(method => method.methodName == m.methodName)
      if filteredDefMethods.nonEmpty then
        val methodToRemove = filteredDefMethods.head
        defaultImplMethods.remove(methodToRemove)
      defaultImplMethods.add(m)

    /**
     * This method removes a method from abstract methods set
     * @param m - method to be removed
     */
    private def removeFromAbstract(m: MethodStruct): Unit =
      val filteredAbsMethods = abstractMethods.filter(method => method.methodName == m.methodName)

      if filteredAbsMethods.nonEmpty && filteredAbsMethods.head.methodAccess == m.methodAccess then
        // signature equivalent, can be removed
        abstractMethods.remove(filteredAbsMethods.head)

    /**
     * This method processes or captures the methods of all superinterfaces are a class
     */
    private def processInterfaceMethods(): Unit =
      classRelations("superInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].foreach(
        superInterface => processSuperInterface(superInterface)
      )

    /**
     * This method separates out Implemented Methods from abstract methods
     * @param classRef - Reference of class for which implemented methods need to be processed
     */
    private def processImplementedMethods(classRef: ClassStruct): Unit =
      // first go to the top
      if classRef.classRelations("superClass").asInstanceOf[ClassStruct] != null then this.processImplementedMethods(classRef.classRelations("superClass").asInstanceOf[ClassStruct])

      classRef.implementedMethods.foreach(m => this.removeFromAbstract(m))

    /**
     * This method process all methods of class, whether its own, its parent class's or the ones belonging to its superinterfaces
     */
    def processClassMethods(): Unit =
      // first get default and abstract methods of parent/ superClass
      if(classRelations("superClass") != null) {
        abstractMethods.addAll( classRelations("superClass").asInstanceOf[ClassStruct].abstractMethods )
        defaultImplMethods.addAll( classRelations("superClass").asInstanceOf[ClassStruct].defaultImplMethods )
      }

      // then process abstract and default methods from superInterfaces
      this.processInterfaceMethods()

      // and lastly add the abstract methods that are added in the class itself
      val absMethodNames = getSetOfMethodNames(abstractMethods)
      val filteredAbsMethods = getInheritableAbstractMethods(classMethods).filter(m => !absMethodNames.contains(m.methodName))
      abstractMethods.addAll(filteredAbsMethods)

      // now every abstract method is available
      implementedMethods.addAll(getInheritableImplementedMethods(classMethods))
      localImplementedMethods.addAll(getLocalImplementedMethods(classMethods))

      this.processImplementedMethods(this)

    /**
     * This method checks whether the class is actually concrete (with all concrete methods) or not
     * @return Boolean - represents whether the class is concrete or not
     */
    def isConcrete: Boolean =
      if abstractMethods.isEmpty then true else false

    def isParentOf(child: ClassStruct): Boolean =
      if this != child then
        if child.classRelations("superClass").asInstanceOf[ClassStruct] != null then this.isParentOf(child.classRelations("superClass").asInstanceOf[ClassStruct]) else false
      else true
  }

  /**
   * Data Structure to store methods
   * @param args - Args SetExpression - signature of method
   * @param body - Instruction SetExpression Sequence/body of method
   */
  private class MethodStruct(val methodName: String, accessProp: AccessProperties, impProp: ImplProperties, args: SetExpression, body: Seq[SetExpression]) {
    val argExp: SetExpression = args
    val methodAccess: AccessProperties = accessProp
    val methodImplementation: ImplProperties = impProp
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

    private def addSuperInterfaceFields(sInterface: InterfaceStruct): Unit =
      // go to the top most interface of superinterface
      if sInterface.interfaceRelations("superInterface").asInstanceOf[InterfaceStruct] != null then this.addSuperInterfaceFields(sInterface.interfaceRelations("superInterface").asInstanceOf[InterfaceStruct])
      val interfaceFieldMap: mutable.Map[String, Any] = mutable.Map()
      sInterface.interfaceFieldNames.foreach(interfaceFieldMap.put(_, 0))

      publicFields ++= sInterface.interfaceFieldTypes("publicFields")
      protectedFields ++= sInterface.interfaceFieldTypes("protectedFields")

      sInterface.interfaceFieldNames.foreach(
        fName =>
          if publicFields.contains(fName) then inheritedFieldMap.put(fName, 0)
          if protectedFields.contains(fName) then inheritedFieldMap.put(fName, 0)
      )

    /**
     * Method to replicate mechanism for object's instantiation by calling h
     * @param classRef: reference of class whose constructor needs to be called
     * @param paramValues: param values for class's constructor
     */
    private def executeConstructor(classRef: ClassStruct, paramValues: Seq[Any]): Unit =
      // recursively go to the top most class
      if classRef.classRelations("superClass") != null then executeConstructor(classRef.classRelations("superClass").asInstanceOf[ClassStruct], paramValues)

      // clear fieldsMap map
      fieldsMap.clear()
      // initializing all fields to a value of zero - default value
      classRef.classFieldNames.foreach(fieldsMap.put(_, 0))
      // add inherited fields to fields map - from previous recursion cycle
      fieldsMap ++= inheritedFieldMap
      // store the
      publicFields ++= classRef.classFieldTypes("publicFields")
      protectedFields ++= classRef.classFieldTypes("protectedFields")

      // add superinterface fields
      classRef.classRelations("superInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].foreach(sInterface => this.addSuperInterfaceFields(sInterface))

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
      createNewScope(new Scope(null, getCurrentScope()))

      // adding this ref to scope
      addBindingToScope("this", this)

      // adding params map to current referencing env
      addMapToScope(paramsMap)
      // Evaluating every expression in that constructor - this might have changed value of some of the fields
      classRef.classConstructor("constructor").methodBody.foreach(methodExp =>
        if getPropagatingException() == null then methodExp.eval else ()
      )
      // updating the inherited field map and public/protected inherited set
      publicFields.foreach(key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )
      protectedFields.foreach(key =>
        if fieldsMap.contains(key) then inheritedFieldMap.put(key, fieldsMap(key))
      )
      // once done executing - no need to remove the params from currentEnv
      // handle exception propagation behavior
      exceptionPropagation

    /**
     * Construct for invoking object's method
     * @param mName - method name to be invoked
     * @param mParams - params to be passed for method
     * @param isCalledFromOutside - represents whether invoked from outside the class's body or not
     * @return
     */
    def invokeMethod(mName: String, mParams: Seq[SetExpression], isCalledFromOutside: Boolean): Any =
      // have to do Dynamic dispatch here
      val methodToCall = if objectClass.localImplementedMethods.map(m => m.methodName).contains(mName) then
        if isCalledFromOutside then throw new Exception("Cannot access private and default access level methods from the object instance directly")
        objectClass.localImplementedMethods.filter(m => m.methodName == mName).head
      else
        try {
          dynamicDispatch(mName, objectClass, isCalledFromOutside)
        }
        catch {
          case e: Exception =>
            if objectClass.defaultImplMethods.map(m => m.methodName).contains(mName) then
              objectClass.defaultImplMethods.filter(m => m.methodName == mName).head
            else
              throw new Exception("Method not found")
        }
      // creating a scope for particular method
      val methodParamNames = methodToCall.argExp.eval.asInstanceOf[Seq[Any]]
      val methodParamValues: Seq[Any] = for p <- mParams yield p.eval
      val paramsMap: mutable.Map[String, Any] = mutable.Map()
      if methodParamNames.size != methodParamValues.size then
        throw new Exception("Number of params do not match")
      else
        for i <- methodParamNames.indices do paramsMap += (methodParamNames(i).asInstanceOf[String] -> methodParamValues(i))

      createNewScope(new Scope(null, getCurrentScope()))

      // adding object ref to scope
      addBindingToScope("this", this)

      // adding params map to current referencing env
      addMapToScope(paramsMap)
      // Evaluating every expression in that constructor
      val lastCallReturn: mutable.Map[String, Any] = mutable.Map()
      for expIndex <- methodToCall.methodBody.indices do
        if getPropagatingException() == null then
          val evaluatedExp = methodToCall.methodBody(expIndex).eval
          // handle last call case
          if expIndex == methodToCall.methodBody.size - 1 then
            lastCallReturn.put("return", evaluatedExp)
          else ()
        else lastCallReturn.put("return", ())

      // remove the scope of this
      removeBindingByKey("this")
      // once done executing
      exceptionPropagation
      // return the value
      lastCallReturn("return")

    /**
     * Mechanism for dynamic dispatch of methods
     * @param mName - method name to be dynamically dispatched
     * @param classRef - current class in the recursion chain
     * @param isCalledFromOutside - represents if method is not called from class's body
     * @return MethodStruct
     */
    private def dynamicDispatch(mName: String, classRef: ClassStruct, isCalledFromOutside: Boolean): MethodStruct =
      // if not part of the current class, go to its parent class
      val implementedMethodNames = classRef.implementedMethods.map(m => m.methodName)
      // not in implemented methods, then do dynamic dispatch again to superClass
      if !implementedMethodNames.contains(mName) && classRef.classRelations("superClass") != null then
        return dynamicDispatch(mName, classRef.classRelations("superClass").asInstanceOf[ClassStruct], isCalledFromOutside)

      // if found the method
      if implementedMethodNames.contains(mName) then
        val methodToCall = classRef.implementedMethods.filter(m => m.methodName == mName).head
        if isCalledFromOutside && methodToCall.methodAccess == AccessProperties.Protected then throw new Exception("Cannot call protected method from object instance directly")
        return methodToCall

      throw new Exception("Method not found with dynamic dispatch")


    /**
     * This method returns the field for an object
     * @param fName - name of field
     * @param requestFromOutside - represents if field's access is requested outside of class's body
     * @return
     */
    def getField(fName: String, requestFromOutside: Boolean): Any =
    // if not in current set of fields and also not in any inherited public and protected fields
      if !(fieldsMap.contains(fName) || publicFields.contains(fName) || protectedFields.contains(fName)) then
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
      if !objectClass.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception("nested class not found")
      else
        objectClass.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]](cName)

    def getInnerInterface(intName: String): InterfaceStruct =
      if !objectClass.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].contains(intName) then
        throw new Exception("nested interface not found")
      else
        objectClass.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]](intName)

    /**
     * Return a Boolean representing the object is an instance of a particular class
     * @param cRef - class reference for match
     * @return
     */
    def isInstanceOf(cRef: ClassStruct): Boolean =
      cRef == objectClass || cRef.isParentOf(objectClass)
  }


  /**
   * Helper method: createFieldsMap
   * Create map data structure for representing fields in class
   * @return Map of String -> Set, mapping fields with different access modifiers in single set
   */
  private def createFieldsMap(): Map[String, SetStringType] =
    Map(
      "defaultFields" -> mutable.Set(),
      "publicFields" -> mutable.Set(),
      "protectedFields" -> mutable.Set(),
      "privateFields" -> mutable.Set()
    )

  /**
   * Helper method: SignatureEquivalent
   * Checks if two methods have same signature or not
   * @return Boolean - represent two methods have same signature or not
   */
  private def SignatureEquivalent(methodOne: MethodStruct, methodTwo: MethodStruct): Boolean =
      methodOne.methodName == methodTwo.methodName

  /**
   * Helper method: MethodInInterfaceHierarchy
   * Checks if method is already in hierarchy of an interface
   * @param method - method to check
   * @param interface - interface to check
   * @return Boolean - result of the check
   */
  private def MethodInInterfaceHierarchy(method: MethodStruct, interface: InterfaceStruct): Boolean =
    if interface != null then
      val matchingMethods = interface.interfaceMethods.filter(m => SignatureEquivalent(m, method))
      if matchingMethods.size == 1 then
        return true
      else if matchingMethods.size > 1 then throw new Exception(method.methodName + ": Access to this method is ambiguous")
      else if matchingMethods.isEmpty then
        return MethodInInterfaceHierarchy(method, interface.interfaceRelations("superInterface").asInstanceOf[InterfaceStruct])
    false

  /**
   * Helper method: getInheritableAbstractMethods
   * Filters out inheritable (public and protected) abstract methods
   * @param methodsSet - set of methods
   * @return - return filtered set
   */
  private def getInheritableAbstractMethods(methodsSet: mutable.Set[MethodStruct]): mutable.Set[MethodStruct] =
    val abstractMethods = methodsSet
      .filter(method => (method.methodAccess == AccessProperties.Public || method.methodAccess == AccessProperties.Protected) && method.methodImplementation == ImplProperties.Abstract )
    abstractMethods

  /**
   * Helper method: getInheritableDefaultMethods
   * Filters out inheritable (public and protected) default methods
   * @param methodsSet - set of methods
   * @return - return filtered set
   */
  private def getInheritableDefaultMethods(methodsSet: mutable.Set[MethodStruct]): mutable.Set[MethodStruct] =
    val defaultMethods = methodsSet
      .filter(method => (method.methodAccess == AccessProperties.Public || method.methodAccess == AccessProperties.Protected) && method.methodImplementation == ImplProperties.Default)
    defaultMethods

  /**
   * Helper method: getInheritableImplementedMethods
   * Filters out inheritable (public and protected) methods with implementation (non-default)
   * @param methodsSet - set of methods
   * @return - return filtered set
   */
  private def getInheritableImplementedMethods(methodsSet: mutable.Set[MethodStruct]): mutable.Set[MethodStruct] =
    val implMethods = methodsSet
      .filter(method => (method.methodAccess == AccessProperties.Public || method.methodAccess == AccessProperties.Protected) && method.methodImplementation == ImplProperties.Implemented)
    implMethods

  /**
   * Helper method: getLocalImplementedMethods
   * Filters out non-inheritable (private and default access) methods with implementation
   * @param methodsSet - set of methods
   * @return - return filtered set
   */
  private def getLocalImplementedMethods(methodsSet: mutable.Set[MethodStruct]): mutable.Set[MethodStruct] =
    val implMethods = methodsSet
      .filter(method => (method.methodAccess == AccessProperties.Private || method.methodAccess == AccessProperties.DefaultAccess) && method.methodImplementation == ImplProperties.Implemented)
    implMethods

  /**
   * Helper method: getSetOfMethodNames
   * map set of methods to map of method names
   * @param methodsSet - set of method
   * @return - set of method names
   */
  private def getSetOfMethodNames(methodsSet: mutable.Set[MethodStruct]): mutable.Set[String] =
    methodsSet.map(method => method.methodName)

  /**
   * Helper method: createClassConstructorMap
   * Create map data structure for representing constructor in class
   * @return Map of String -> MethodStruct (constructor implementation)
   */
  private def createClassConstructorMap(constructor: MethodStruct = null): methodMapType =
    mutable.Map[String, MethodStruct](
      "constructor" -> constructor
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
   * @return ClassStruct - the class reference
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

  /**
   * This method finds and returns a reference to interface declared in a particular scope
   * @param intName: String - name of the interface
   * @param scopeEnv : Scope - current referencing environment
   * @return InterfaceStruct - the interface reference
   */
  @tailrec
  private def getInterfaceRef(intName: String, scopeEnv: Scope): InterfaceStruct =
    if scopeEnv.interfaces.contains(intName) then
    // class exist in this scope
      scopeEnv.interfaces(intName)
    else if scopeEnv.scopeParent == null then
      null
    else
      getInterfaceRef(intName, scopeEnv.scopeParent)

  /**
   *
   * This method creates or declares the class and returns a ClassStruct Object
   * @param cName     : String - class name to be created
   * @param classBody : Sequence of SetExpression which are part of class body (listed in method resolveClassMembers)
   * @param isAbstract : Boolean to tell that class needs to an abstract one
   * @return ClassStruct - a class object
   */
  private def declareClass(cName: String, classBody: Seq[SetExpression], isAbstract: Boolean): ClassStruct =
    val constructorMap: methodMapType = createClassConstructorMap()
    val fieldsMap: Map[String, SetStringType] = createFieldsMap()
    val newClassRef = ClassStruct(
      cName,
      constructorMap,
      fieldsMap,
      isAbstract
    )
    classBody.foreach( resolveClassMembers(_, newClassRef))
    // process all methods
    newClassRef.processClassMethods()
    // exception checking
    if newClassRef.isConcrete && isAbstract then throw new Exception("Concrete classes cannot be declared abstract")
    else if !newClassRef.isConcrete && !isAbstract then throw new Exception("Non-concrete classes (classes with abstract method) should be declared abstract")
    newClassRef

  /**
   * This method creates or declares the interface and returns a InterfaceStruct Object
   * @param intName     : String - interface name to be created
   * @param intBody : Sequence of SetExpression which are part of interface body (listed in method resolveInterfaceMembers)
   * @return InterfaceStruct - a interface object
   */
  private def declareInterface(intName: String, intBody: Seq[SetExpression]): InterfaceStruct =
    val fieldsMap: Map[String, SetStringType] = createFieldsMap()
    val newInterfaceRef = InterfaceStruct(
      intName,
      fieldsMap
    )
    intBody.foreach( resolveInterfaceMembers(_, newInterfaceRef) )
    // todo: check if there are only abstract and default methods
    newInterfaceRef

  /**
   * This method resolves the Expressions which are members of the class, includes class field declaration, defining constructor, defining methods, defining inner/nested classes
   * @param setExp: SetExpression type of member expression
   * @param classRef - class reference on which these members are called
   * @return Any
   */
  private def resolveClassMembers(setExp: SetExpression, classRef: ClassStruct): Any = setExp match {
    // Constructor Expression - will not be evaluated separately
    case Constructor(pExp, body*) =>
      if classRef.classConstructor("constructor") != null then throw new Exception("Only single constructor can be defined for a Class")
      classRef.classConstructor.put("constructor", new MethodStruct("constructor", AccessProperties.DefaultAccess, ImplProperties.Implemented, pExp, body))

    // Extending other classes
    case SetExpression.Extends(sClassRef) =>
      val superClassRef = sClassRef.eval.asInstanceOf[ClassStruct]
      if classRef.classRelations("superClass") != null then throw new Exception("A Class can extend only a single class")
      classRef.classRelations.put("superClass", superClassRef)

      // Implementing multiple interface
    case SetExpression.Implements(sInterfaceRefs*) =>
      if classRef.classRelations("superInterfaces").asInstanceOf[mutable.Set[InterfaceStruct]].nonEmpty then throw new Exception("Implement expression can only be used once")
      sInterfaceRefs.foreach( sRef => classRef.addSuperInterface( sRef.eval.asInstanceOf[InterfaceStruct] ) )

    // CreateField Expression
    case SetExpression.CreateField(fName) =>
      classRef.classFieldTypes("defaultFields").add(fName)
      classRef.classFieldNames.add(fName)
    // CreatePublicField Expression
    case SetExpression.CreatePublicField(fName) =>
      classRef.classFieldTypes("publicFields").add(fName)
      classRef.classFieldNames.add(fName)

    // CreateProtectedField Expression
    case SetExpression.CreateProtectedField(fName) =>
      classRef.classFieldTypes("protectedFields").add(fName)
      classRef.classFieldNames.add(fName)

    // CreatePrivateField Expression
    case SetExpression.CreatePrivateField(fName) =>
      classRef.classFieldTypes("privateFields").add(fName)
      classRef.classFieldNames.add(fName)

    // Method Expression
    case SetExpression.Method(mName, accessProp, args, body*) =>
      val implProp: ImplProperties = if body.isEmpty then ImplProperties.Abstract else ImplProperties.Implemented
      val method: MethodStruct = new MethodStruct(mName, accessProp.eval.asInstanceOf[AccessProperties], implProp, args, body)
      classRef.addClassMethod(method)

    // ClassDef Expression
    case SetExpression.ClassDef(cName, clsExpArgs*) =>
      if classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception(cName + ": class already exists")
      val innerClass = declareClass(cName, clsExpArgs, false)
      classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].put(cName, innerClass)

    // ClassDef Expression
    case SetExpression.ExceptionClassDef(cName, clsExpArgs*) =>
      if classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception(cName + ": exception class already exists")
      val innerClass = declareClass(cName, clsExpArgs, false)
      classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].put(cName, innerClass)

    case SetExpression.AbstractClassDef(cName, clsExpArgs*) =>
      if classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception(cName + ": class already exists")
      val innerClass = declareClass(cName, clsExpArgs, true)
      classRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].put(cName, innerClass)

    case SetExpression.InterfaceDef(intName, expArgs*) =>
      if classRef.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].contains(intName) then
        throw new Exception(intName + ": interface already exists")
      val innerInterface = declareInterface(intName, expArgs)
      classRef.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].put(intName, innerInterface)

    case _ =>
  }

  /**
   * This method resolves the interface members
   * @param setExp - SetExpression type for member expression
   * @param interfaceRef - Interface Object
   * @return - resolved value of expression as per matching
   */
  private def resolveInterfaceMembers(setExp: SetExpression, interfaceRef: InterfaceStruct): Any = setExp match {

    case SetExpression.Implements(sIntExp*) =>
      throw new Exception("An interface cannot implement another interface")

    case SetExpression.Extends(sIntRef) =>
      val superInterfaceRef = sIntRef.eval.asInstanceOf[InterfaceStruct]
      if interfaceRef.interfaceRelations("superInterface") != null then throw new Exception("An Interface can extend only a single Interface")
      interfaceRef.interfaceRelations.put("superInterface", superInterfaceRef)

    // CreateField Expression
    case SetExpression.CreateField(fName) =>
      interfaceRef.interfaceFieldTypes("defaultFields").add(fName)
      interfaceRef.interfaceFieldNames.add(fName)
    // CreatePublicField Expression
    case SetExpression.CreatePublicField(fName) =>
      interfaceRef.interfaceFieldTypes("publicFields").add(fName)
      interfaceRef.interfaceFieldNames.add(fName)

    // CreateProtectedField Expression
    case SetExpression.CreateProtectedField(fName) =>
      interfaceRef.interfaceFieldTypes("protectedFields").add(fName)
      interfaceRef.interfaceFieldNames.add(fName)

    // CreatePrivateField Expression
    case SetExpression.CreatePrivateField(fName) =>
      interfaceRef.interfaceFieldTypes("privateFields").add(fName)
      interfaceRef.interfaceFieldNames.add(fName)

    // Method Expression
    case SetExpression.Method(mName, accessProp, args, body*) =>
      val implProp: ImplProperties = if body.isEmpty then ImplProperties.Abstract else ImplProperties.Default
      val method: MethodStruct = new MethodStruct(mName, accessProp.eval.asInstanceOf[AccessProperties], implProp, args, body)
      if MethodInInterfaceHierarchy(method, interfaceRef) then throw new Exception("Method with same name (signature) already exists in the hierarchy")
      interfaceRef.interfaceMethods.add(method)

    // ClassDef Expression
    case SetExpression.ClassDef(cName, clsExpArgs*) =>
      if interfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception(cName + ": class already exists")
      val innerClass = declareClass(cName, clsExpArgs, false)
      interfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].put(cName, innerClass)

    case SetExpression.AbstractClassDef(cName, clsExpArgs*) =>
      if interfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
        throw new Exception(cName + ": class already exists")
      val innerClass = declareClass(cName, clsExpArgs, true)
      interfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].put(cName, innerClass)

    case SetExpression.InterfaceDef(intName, expArgs*) =>
      if interfaceRef.interfaceRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].contains(intName) then
        throw new Exception(intName + ": interface already exists")
      val innerInterface = declareInterface(intName, expArgs)
      interfaceRef.interfaceRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].put(intName, innerInterface)

    case _ =>
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

    /**
     * ClassDef Expression
     * Defines a class with a class name and set of expression which are part of class's body or can be thought of as class members
     */
    case ClassDef(className: String, classExpArgs: SetExpression*)

    /**
     * AbstractClassDef Expression
     * Defines an abstract class with a class name and set of expression which are part of class's body or can be thought of as class members
     */
    case AbstractClassDef(className: String, classExpArgs: SetExpression*)

    case ExceptionClassDef(className: String, classExpArgs: SetExpression*)

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
     * ClassRefFromInterface Expression
     * Refer to a class which is declared as an inner class of the InterfaceRef interface reference
     */
    case ClassRefFromInterface(className: String, intRef: SetExpression)

    /**
     * InterfaceRef Expression
     * Refer to a interface from name
     */
    case InterfaceRef(intName: String)

    /**
     * InterfaceRefFromClass Expression
     * Refer to a interface from a class whose member interface it is
     */
    case InterfaceRefFromClass(intName: String, classRef: SetExpression)

    /**
     * InterfaceRefFromInterface Expression
     * Refer to a interface from another interface whose member interface it is
     */
    case InterfaceRefFromInterface(intName: String, intRef: SetExpression)

    /**
     * InterfaceRefFromObject Expression
     * Refer to a interface from object whose parent class's member interface it is
     */
    case InterfaceRefFromObject(intName: String, objRef: SetExpression)

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
    case Method(methodName: String, accessProp: SetExpression, argExp: SetExpression, mBodyExpArgs: SetExpression*)

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

    /**
     * Extends Expression
     * extends a super class
     */
    case Extends(superClassRef: SetExpression)

    /**
     * Implements Expression
     * Implements one or more interfaces
     */
    case Implements(interfaceRefs: SetExpression*)

    /**
     * InterfaceDef Expression
     * Defining an interface
     */
    case InterfaceDef(intName: String, interfaceExpArgs: SetExpression*)

    /**
     * PublicAccess Expression
     * Represents Public Access Modifier
     */
    case PublicAccess()

    /**
     * ProtectedAccess Expression
     * Represents Protected Access Modifier
     */
    case ProtectedAccess()

    /**
     * PrivateAccess Expression
     * Represents Private Access Modifier
     */
    case PrivateAccess()

    /**
     * DefAccess Expression
     * Represents Default Access Modifier
     */
    case DefAccess()

    // control structures
    case If(ConditionExp: SetExpression, thenClause: SetExpression)

    case Check(exp: SetExpression)

    case IfElse(ConditionExp: SetExpression, thenClause: SetExpression.Then, elseClause: SetExpression.Else)

    case Then(expSeq: SetExpression*)

    case Else(expSeq: SetExpression*)

    case Try(expSeq: SetExpression*)

    case Catch(eName: String, eType: SetExpression, catchExpSeq: SetExpression*)

    case TryCatch(tryExp: SetExpression.Try, catchExpSeq: SetExpression.Catch*)

    case ThrowNewException(exceptionRef: SetExpression, exceptionCause: SetExpression*)

    /** This method evaluates SetExpressions
     * Description - The body of this method is the implementation of above abstract data types
     *
     * @return Any
     */
    def eval: Any = (this: @unchecked) match {
      // Value Expression Implementation
      case Value(v) => v

      // Variable Expression Implementation
      case Variable(varName) => getVariable(varName, getCurrentScope())

      // Assign Expression Implementation
      case Assign(varName, exp) =>
        val evaluatedExp = exp.eval
        addBindingToScope(varName, evaluatedExp)
        evaluatedExp

      // Macro Expression Implementation
      case Macro(exp) => exp

      // ComputeMacro Expression Implementation
      case ComputeMacro(exp) => exp.eval.asInstanceOf[SetExpression].eval

      // NamedScope Expression Implementation
      case NamedScope(scopeName: String, expArgs*) =>
        //check whether there are no child scope or if there are child scopes then check if the specified scope name is not a part of child scope
        if getCurrentScope().childScopes.isEmpty || !getCurrentScope().childScopes.contains(scopeName) then
          // opening a new scope block
          val newScope: Scope = new Scope(scopeName, getCurrentScope())
          // adding the new scope's reference to current scope's children
          getCurrentScope().childScopes += (scopeName -> newScope)

          processScope(newScope,{}, expArgs)
        else
          // find the nested scope in children map if already there
          val nestedScope: Scope = getCurrentScope().childScopes(scopeName)
          processScope(nestedScope, {}, expArgs)

      // UnnamedScope Expression Implementation
      case UnnamedScope(expArgs*) =>
        // Create a new scope as anonymous scopes can't be referred again
        val newScope: Scope = new Scope(null, getCurrentScope())
        processScope(newScope, {}, expArgs)

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
        val clsRef = getClassRef(cName, getCurrentScope())
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
        if outerClassRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].get(cName).isEmpty then
          throw new Exception(cName + " : no such inner class found")
        else
          outerClassRef.classRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]](cName)

      // Returns a class reference from an interface
      case ClassRefFromInterface(cName, intRef) =>
        val outerInterfaceRef = intRef.eval.asInstanceOf[InterfaceStruct]
        if !outerInterfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]].contains(cName) then
          throw new Exception(cName + " : no such inner class found")
        else
          outerInterfaceRef.interfaceRelations("memberClasses").asInstanceOf[mutable.Map[String, ClassStruct]](cName)

      // Interface reference using name
      case InterfaceRef(intName) =>
        val intRef = getInterfaceRef(intName, getCurrentScope())
        if intRef == null then
          throw new Exception(intName + " interface does not exists.")
        else
          intRef

      // Interface reference from class as member interfaces
      case InterfaceRefFromClass(intName, classRef) =>
        val outerClassRef = classRef.eval.asInstanceOf[ClassStruct]
        if outerClassRef.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].get(intName).isEmpty then
          throw new Exception(intName + " : no such inner interface found")
        else
          outerClassRef.classRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]](intName)

      // Interface reference from interface as member interfaces
      case InterfaceRefFromInterface(intName, intRef) =>
        val outerInterfaceRef = intRef.eval.asInstanceOf[InterfaceStruct]
        if outerInterfaceRef.interfaceRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]].get(intName).isEmpty then
          throw new Exception(intName + " : no such inner interface found")
        else
          outerInterfaceRef.interfaceRelations("memberInterfaces").asInstanceOf[mutable.Map[String, InterfaceStruct]](intName)

      // Interface reference from object as member interfaces of instantiating class
      case InterfaceRefFromObject(intName, objRef) =>
        val objectToRef = objRef.eval.asInstanceOf[ObjectStruct]
        objectToRef.getInnerInterface(intName)

      // Class definition - check if class not declared already
      case ClassDef(cName, clsExpArgs*) =>
        val clsRef = getClassRef(cName, getCurrentScope())
        if clsRef == null then
          val newClass = declareClass(cName, clsExpArgs, false)
          getCurrentScope().classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")

      // Abstract method definition
      case AbstractClassDef(cName, clsExpArgs*) =>
        val clsRef = getClassRef(cName, getCurrentScope())
        if clsRef == null then
          val newClass = declareClass(cName, clsExpArgs, true)
          getCurrentScope().classes.put(cName, newClass)
        else
          throw new Exception(cName + " class already exists.")

      // Interface Definition
      case InterfaceDef(intName, expArgs*) =>
        val intRef = getInterfaceRef(intName, getCurrentScope())
        if intRef == null then
          val newInterface = declareInterface(intName, expArgs)
          getCurrentScope().interfaces.put(intName, newInterface)
        else
          throw new Exception(intName + " interface already exists.")

      case ExceptionClassDef(cName, classExpArgs*) =>
        val clsRef = getClassRef(cName, getCurrentScope())
        if clsRef == null then
          val newClass = declareClass(cName, classExpArgs, false)
          getCurrentScope().classes.put(cName, newClass)
        else
          throw new Exception(cName + " exception class already exists.")

      // Params expression - used to specify params to methods and constructor
      case ParamsExp(pExpArgs*) =>
        val params = for p <- pExpArgs yield p.eval
        params

      // Param Expression
      case Param(s) => s

      // NewObject Expression - returns new object
      case NewObject(classRef, cArgs*) =>
        val instanceClassRef = classRef.eval.asInstanceOf[ClassStruct]
        if instanceClassRef.isAbstract || !instanceClassRef.isConcrete then throw new Exception("An Abstract class cannot be instantiated")
        val newObject = ObjectStruct(classRef.eval.asInstanceOf[ClassStruct], cArgs)
        newObject

      // InvokeMethod Expression - used to call method from which the Class constructor and other methods
      case InvokeMethod(mName, params*) =>
        val currentObject = getBindingFromScope("this")
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, false)

      // InvokeMethodOfObject Expression
      case InvokeMethodOfObject(mName, objectRef, params*) =>
        val currentObject = objectRef.eval
        addBindingToScope("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].invokeMethod(mName, params, true)

      // Field Expression - Returns single field - analogous to this.field
      case Field(fName) =>
        val currentObject = getBindingFromScope("this")
        currentObject.asInstanceOf[ObjectStruct].getField(fName, false)

      // FieldFromObject Expression - Returns field from object, analogous to object.field
      case FieldFromObject(fName, objRef) =>
        val currentObject = objRef.eval
        addBindingToScope("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].getField(fName, true)

      // SetField Expression - Updates the value of the field, analogous to assigning value to this.field
      case SetField(fName, exp) =>
        val currentObject = getBindingFromScope("this")
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, false)

      // SetFieldFromObject Expression - Updates the value of the field by referencing the object from outside the clas body, analogous to assigning value to object.field
      case SetFieldFromObject(fName, objRef, exp) =>
        val currentObject = objRef.eval
        addBindingToScope("this", currentObject)
        currentObject.asInstanceOf[ObjectStruct].setField(fName, exp.eval, true)

      // ObjectInstanceOf Expression - returns a Boolean which represents whether the object is an instantiation of a particular class or not
      case ObjectInstanceOf(objRef, clsRef) => objRef.eval.asInstanceOf[ObjectStruct].isInstanceOf(clsRef.eval.asInstanceOf[ClassStruct])

      // Public Access Modifier
      case PublicAccess() => AccessProperties.Public

      // Private Access Modifier
      case PrivateAccess() => AccessProperties.Private

      // Protected Access Modifier
      case ProtectedAccess() => AccessProperties.Protected

      // Default Access Modifier
      case DefAccess() => AccessProperties.DefaultAccess

      case Check(exp) => toBoolean(exp.eval)

      case If(condition, thenClause) =>
        if(condition.eval.asInstanceOf[Boolean]) then thenClause.eval else ()

      case IfElse(condition, thenClause, elseClause) =>
        if(condition.eval.asInstanceOf[Boolean]) then thenClause.eval else elseClause.eval

      case Then(thenExpSeq*) =>
        val newScope: Scope = new Scope(null, getCurrentScope())
        processScope(newScope, {}, thenExpSeq)

      case Else(elseExpSeq*) =>
        val newScope: Scope = new Scope(null, getCurrentScope())
        processScope(newScope, {}, elseExpSeq)

      case TryCatch(tryExp, catchExpSeq*) => {
        val tryObject = TryStruct(catchExpSeq)
        val newTryScope = Scope(null, getCurrentScope(), tryObject)
        val evalTryExpSeq = tryExp.eval
        processScope(newTryScope, {}, evalTryExpSeq.asInstanceOf[Seq[SetExpression]])
      }

      case Try(tryExpSeq*) => tryExpSeq

      case Catch(eName, eType, catchExpSeq*) => Map[String, Any]("exceptionName" -> eName, "exceptionType" -> eType.eval.asInstanceOf[ClassStruct], "catchSeq" -> catchExpSeq)

      case ThrowNewException(classRef, cause*) =>
        val instanceClassRef = classRef.eval.asInstanceOf[ClassStruct]
        if instanceClassRef.isAbstract || !instanceClassRef.isConcrete then throw new Exception("An Abstract class cannot be instantiated")
        val newException = ObjectStruct(classRef.eval.asInstanceOf[ClassStruct], cause)
        if(newException.getField("cause", true) != null) then
          addExceptionToScope(newException)
          if getParentScope() == null then
            removeExceptionFromScope()
            globalScopeExceptionHandling
        else throw new Exception("DSL Exception must have a 'cause' public field")
    }

  private def toBoolean(value: Any): Boolean = value match {
    case "" => false
    case null => false
    case false => false
    case 0 => false
    case _ => true
  }

  private def matchExceptionWithCatch(exception: ObjectStruct, catchExpMap: Map[String, Any]): Boolean = exception.isInstanceOf( catchExpMap("exceptionType").asInstanceOf[ClassStruct] )

  private def globalScopeExceptionHandling: Unit = throw new Exception("Unhandled Exception")

  private def exceptionPropagation: Unit =
    // check for propagating exception
    val exception = getPropagatingException()
    if exception != null then
      // check for a try block
      val tryObject = getCurrentScope().tryObject
      if tryObject != null then
        // switch to parent scope first so that catch scopes won't come in between
        switchToParentScope()
        val catchExpSeq = tryObject.catchExpSeq
        // filter with matched expressions
        val matchedCatchExpList = catchExpSeq.filter(catchExp => {
          val catchMap =  catchExp.eval.asInstanceOf[Map[String, Any]]
          matchExceptionWithCatch(exception, catchMap)
        })
        // go with the first one
        if matchedCatchExpList.isEmpty then
          addExceptionToScope(exception)
          if getParentScope() == null then
            removeExceptionFromScope()
            globalScopeExceptionHandling
          else ()
        else
          val catchExp = matchedCatchExpList.head
          val catchMap = catchExp.eval.asInstanceOf[Map[String, Any]]
          // current exception is handled, stop propagating
          removeExceptionFromScope()
          // create catch expression scope
          val catchScope = new Scope(null, getCurrentScope())
          // add exception to the scope for it to be handled
          processScope(catchScope, {
            addBindingToScope(catchMap("exceptionName").asInstanceOf[String], exception)
          }, catchMap("catchSeq").asInstanceOf[Seq[SetExpression]])
      else
        switchToParentScope()
        addExceptionToScope(exception)
        if getParentScope() == null then
          removeExceptionFromScope()
          globalScopeExceptionHandling
        else ()
    else switchToParentScope()

  private def processScope(scope: Scope, beforeEvalBlock: => Unit, expSeq: Seq[SetExpression]): Unit =
    // switching the current environment to the new scope
    createNewScope(scope)
    beforeEvalBlock
    // evaluating each expression passed to the scope
    expSeq.foreach(exp => {
      // only evaluate the expression if there is no exception propagating
      if getPropagatingException() == null then exp.eval else ()
    })
    exceptionPropagation

  /**
   * Main Function, entry point to the application
   */
  @main def runSetTheoryDSL(): Unit = {
    println("Program runs successfully")

    import SetExpression.*

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


  }

}
