# Rajat Kumar (UIN: 653922910)

## CS474 Homework 2 - Submission

### Set Theory DSL - SetPlayground

---

### Introduction
Set Theory DSL is a Domain Specific Language created to create classes, objects and perform simple operations on Sets. It is build on top of Scala 3. Set operations like Union, Intersection, Set Difference, Symmetric Set Difference and Cartesian are implemented with the help of expression. Other operations like Inserting and deleting items are also implemented. Language specific operations like assigning the values to variables, fetching those variables, macros and their evaluation and scopes (both named and anonymous scopes) are also implemented. Classes, Object are also allowed to create. Classes can inherit other classes and single class inheritance is supported. There is also the support of Nested Classes. Operations on Sets can be performed with the help of this DSL's capabilities to enable object-oriented programming. Other important features like Dynamic Dispatch and access modiifers are also supported.

---

###  Instructions to run the Project

1. Install [IntelliJ](https://www.jetbrains.com/student/) (this link is for student edition, you can download other as well).
2. Use [github](https://github.com/RJonMshka/CS474Homework1.git) repo for this project and clone it into your machine using git bash on Windows and terminal on MacOS.
3. Switch to the `homework2` branch if the default branch is something else.
4. Open IntelliJ and go to `File > New > Project from existing Source`, or `File > New > Project from version control`. For the second option, you have to directly provide git repo link and no need to clone the git repo separately.
5. Make sure you have Java JDK and scala installed on your system. Java JDK between versions 8 and 17 are required to run this project.
6. We are using Scala version `3.1.1`. Please go to its [docs](https://docs.scala-lang.org/scala3/) for better understanding. You can download Scala 3 from [here](https://www.scala-lang.org/download/).
7. The build tool we are using for this is call [SBT(Simple Build Toolkit)](https://www.scala-sbt.org/download.html). Check out this download [link](https://www.scala-sbt.org/download.html) for SBT.
8. Alternatively, you can also install SBT for your project only from IntelliJ's external libraries.
9. SBT version we are using in this project is `1.6.2`.
10. Please also install `Scala` plugin in IntelliJ itself.
11. For testing purposes, we are using [Scalatest](https://www.scalatest.org/).
12. If you go to the `build.sbt` in the folder structure, you can see that Scalatest version `3.2.11` is added as a dependency.
13. The `build.sbt` is responsible for building your project.
14. In IntelliJ, go to `Build > Build Project` to build the project.
15. Once build is finished, you are ready to run the project.
16. There are two test files `src/test/scala/SetTheoryDSLTest.scala` and `src/test/scala/ClassesAndInheritanceDSLTest.scala`. The first one is concerned with testing Set Operations of DSL's SetExpressions and second one is focused towards testing the object-oriented features of this DSL.
17. You can add your own test cases in these files to test the project better.
18. To run test cases directly from terminal, enter the command `sbt compile test` or `sbt clean compile test`.
19. To run the main file `SetTheoryDSLTest.scala`, enter the command `sbt compile run` or `sbt clean compile run` in the terminal.
20. The file `src/main/scala/SetTheoryDSL.scala` is the main implementation of Set Theory DSL.
21. Please explore and have fun.

---

### Implementation
The implementation of Set Theory DSL is done with the help of `Enumeration` or [enums](https://docs.scala-lang.org/scala3/reference/enums/enums.html) construct. Enum types are used as Set Expressions (or Instructions) for the DSL. `Mutable Maps` are used to store variables. The `Scope` class is the implementation of named and anonymous scopes whose instances hold a name, a pointer to the parent scope, their bindings or reference environment and a `Mutable Map` which holds the reference to its child scopes.
The support of Object-Oriented Programming (OOP) behavior is also implemented. Various `SetExpression`s are added as compared to the homework1 (branch `master`). `ClassStruct`, `MethodStruct`, `ObjectStruct` and some other constructs are used to create support for Classes, Objects, Fields, Methods and specifically dynamic dispatch and inheritance.
A deep down implementation, syntax and semantics is covered in the next section.

---

## Set Operation Syntax and Semantics

### Common Language Syntax
All of the expression are of type `SetExpression`.
Each expression or instruction need to be evaluated for its working. Which means that you have to write `.eval` at the end of the outermost expression.

For example, the below code declares a variable with name `var1` and assign it a value of `1` (which is the evaluated value of `Value(3)` expression itself). Since, `Value(1)` is also an expression, but you do not need to call `eval` on it separately. You just have to call `eval` on the outermost expression which is `Assign` in this case.
```
Assign("var1", Value(1) ).eval
```

There are difference expressions in this DSL and their semantics and syntax are as follows:

### Value(value: Any): Any
`Value` expression takes one argument of type `Any` and evaluates to that argument itself. It returns the evaluated value.
For example:
```
Value(1).eval
```
The above expression evaluates (returns) to `1`.
It can take any value, like Strings, Int, Float, Double and other data-types as well.

For example
```
Value(1).eval              // returns 1
Value(2.0f).eval           // returns 20.0f
Value("hello").eval        // returns "hello"
```

### Assign(name: String, setExpression: SetExpression): Any
`Assign` expression is used to create a variable a particular value which is the evaluated value of its 2nd argument. Its return value is the evaluated value of 2nd argument.

To create a variable and assign it a value of 10, we have to write the following code.
```
Assign("var1", Value(10) ).eval
```
To assign a value of a variable into another variable, we can write the following code.
```
Assign("var1", Variable("var2") ).eval    // here var2 is the name of a variable which is assumed to be declared before this expression
```

```
Assign("set1", SetIdentifier() ).eval     // this creates an empty set and assigns it to variable with name "set1"
```

### Variable(varName: String): Any
The `Variable` expression is used to get the value of the variable stored in current referencing environment. It accepts the first argument as a String value which represents the name of the variable it is referring in the table.

For example:
```
Assign("var1", Value(10) ).eval
Variable("var1").eval      // This code returns 10
```

We can use Variables to store any kind of data type as well as Sets, Macros and other variables also.

### Macro(macroExpression: SetExpression): SetExpression
`Macro`s are used for their lazy evaluation. Instead of evaluating the `macroExpression`, it simply returns that `macroExpression`. This helps us to store the expression itself in a variable.

Syntax or usage:
```
Assign("macro1", Macro( Variable("var3") )).eval        // Stores "Variable("var3")" into a variable named "macro1"
```

This can be helpful in situation where we want to evaluate certain expression whose argument are not yet defined or are not the part of current referencing environment.
In the next section, we will see how to compute these macros on demand.

### ComputeMacro(macroExpression: SetExpression): Any
The `ComputeMacro` expression computes a particular macro and returns its evaluated value.

Syntax:

```
ComputeMacro( Variable( "macroName" ) ).eval          // Computes the macro expression which is stored in variable named "macroName"
```

Use Cases:
```
Assign("macro1", Macro( Variable("var1") )).eval       // even if var1 is not created yet, its expression can be stored in memory using macro
Assign("var1", Value(10) ).eval
ComputeMacro( Variable("macro1") ).eval               // returns 10
Assign("var1", Value(20) ).eval
ComputeMacro( Variable("macro1") ).eval               // return 20
```

### NamedScope(scopeName: String, scopeExpArgs: SetExpression*)
The `NamedScope` expression is used to create a named scope block. We can even nest multiple scope blocks with it. A NamedScope's bindings can be accessed again in the code if the NamedScope created is a direct children on current scope.
It takes first argument as scopeName which is a String. `scopeExpArgs` are a sequence of arguments passed to it. It can be more than one. The concept is that you can execute more than one instruction in a particular scope without re-writing its name.

For Example:

```
NamedScope("scope1", 
    Assign( "var1", Value(20) )
).eval                                // Create a scope whose binding environment will have one variable named var1 and whose value will be 20
```

Variable shadowing can also be achieved with it. Consider the following example:
```
Assign( "var1", Value(10) ).eval                       // global variable with value 10                         
NamedScope("scope1", 
    Assign( "var1", Value(20) )                            // var1 variable is another variable inside scope scope 1 which shadows the var1 varaible declared in global scope
).eval   
Variable("var1").eval                                  // returns 10 as it is the variable of outer scope 
```

We can also create nested scopes:
```
NamedScope("scope1", 
    Assign( "var1", Value(20) ),
    NamedScope("scope2, 
        Assign( "var1", Value(30) )
    )                           
).eval                                            // scope2 is a child scope of scope1 and can only be accessed from scope1 which can only be accessed from global scope

// Accessing named scopes again

NamedScope("scope1", 
    Assign( "var2", Variable("var1) ),           // var2 will be assigned value 20 because var1 is already declared in this scope earlier
    NamedScope("scope2, 
        Assign( "var3", Variable("var1") )       // var3 will be assigned value 30 because var1 is already declared (shadowed the var1 of scope1) in this scope earlier
    )                           
).eval 

```

### UnnamedScope(scopeExpArgs: SetExpression*)
The `UnnamedScope` expression is similar to `NamedScope` expression above. However, there are few differences. It does not have a name. We only specify the SetExpression(s) to execute in that scope. Also, since, it does not have a name, it will not be accessible to its parent once it is closed. If any named or Unnamed scopes are created inside it, they will also not be accessing if this scope closes.
Its similar to NamedScope in the sense that they share almost the same syntax except name as the first argument.

Example:
```
UnnamedScope( 
    Assign( "var1", Value(20) )
).eval  
```

Nested UnnamedScope:
```
UnnamedScope( 
    Assign( "var1", Value(20) ),
    UnnamedScope( 
        Assign( "var1", Value(30) )
    )                           
).eval  
```

```
UnnamedScope( 
    Assign( "var1", Value(20) ),                            // This variable won't be accessible outside of this scope once this scope closes
    UnnamedScope( 
        Assign( "var1", Value(30) ),                        // This variable won't be accessible outside of this scope once this scope closes
        Assign( "var2", Value(50) )
    )                                                           
).eval  

```

### SetIdentifier(setExpArgs: SetExpression*): collection.mutable.Set[Any]
The `SetIdentifier` returns a mutable Set of Any data-type. The syntax is pretty simple, we can pass zero or more `SetExpression`s to it and it will evaluate all of them and place them in a mutable Set.
To store this Set, we can use the `Assign` expression.

Syntax/Example:
```
Assign("set1", SetIdentifier() ).eval            // stores/references the mutable.Set() in variable named set1
Assign( "var1", Value(30) ).eval
Assign("set2". SetIdentifier( Value(1), Value("hello"), Variable("var1) ) ).eval     // Variable set2 will store reference to a new Set which has elements 1, "hello" and 30

Variable("set2").eval                 // returns Set(1, "hello", 30)
```

### Union(s1: SetExpression, s2: SetExpression): collection.mutable.Set[Any]
The `Union` expression takes exactly two arguments, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the union operation of two sets and return another set with elements containing elements of both sets.

Syntax/Example:
```
Assign("union_set", Union( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(3), Value(4)) ) ).eval
Variable("union_set").eval      // returns Set(1,2,3,4)
```

### Intersection(s1: SetExpression, s2: SetExpression): collection.mutable.Set[Any]
The `Intersection` expression takes exactly two arguments like `Union`, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the intersection operation of two sets and return another set with elements containing only the common elements of both sets.

Syntax/Example:
```
Assign("intersection_set", Intersection( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(2), Value(3)) ) ).eval
Variable("intersection_set").eval      // returns Set(2)
```

### SetDifference(s1: SetExpression, s2: SetExpression): collection.mutable.Set[Any]
The `SetDifference` expression takes exactly two arguments like `Union`, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the set difference operation of two sets and return another set with elements of the first set which are not part of the second set.

Syntax/Example:
```
Assign("set_diff_set", SetDifference( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(2), Value(3)) ) ).eval
Variable("set_diff_set").eval      // returns Set(1)
```

### SymDifference(s1: SetExpression, s2: SetExpression): collection.mutable.Set[Any]
The `SymDifference` expression takes exactly two arguments like `Union`, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the set difference operation of two sets and return another set with elements which are not common between both sets.

Syntax/Example:
```
Assign("sym_diff_set", SymDifference( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(2), Value(3)) ) ).eval
Variable("sym_diff_set").eval      // returns Set(1, 3)
```

### CartesianProduct(s1: SetExpression, s2: SetExpression): collection.mutable.Set[Any]
The `CartesianProduct` expression takes exactly two arguments like `Union`, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the Cartesian product operation of two sets and return another set with elements in the form (a, b), where a is every element of first set and b is every element of second set.

Syntax/Example:
```
Assign("cp_set", CartesianProduct( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(3), Value(4)) ) ).eval
Variable("cp_set").eval      // returns Set( (1, 3), (1, 4), (2, 3), (2, 4) )
```

### InsertInto(setExp: SetExpression, setExpArgs: SetExpression*): collection.mutable.Set[Any]
The `InsertInto` expression take the first argument as a `SetExpression` which must resolve or evaluate to a `mutable Set`. The other argument(s) are also `SetExpression` type but they can be more than one. These `SetExpression`s are evaluated to their respective values and are then inserted into the Set evaluated from the first argument.

Syntax/Example:
```
Assign("var1", Value("insertValue") ).eval                 // creating a variable
Assign("set1", SetIdentifier( Value(20) ) ).eval           // creating a set with one Int 20 in it

InsertInto( Variable("set1"), Value(30), Variable("var1") ).eval         // inserting into the set set1 30 as well as value of variable var1 which is "insertValue"
Variable("set1").eval                                                    // Would return Set(20, 30, "insertValue")
```

### DeleteFrom(setExp: SetExpression, setExpArgs: SetExpression*)
The `DeleteFrom` expression deletes one or many values from a Set. The first argument is a `SetExpression` which should resolve to a `mutable.Set[Any]` and other arguments are also of type `SetExpression` which are the values that need to be removed from the Set. Multiple values can be remove with one expression.

Syntax/Code Example:
```
Assign("set1", SetIdentifier( Value(20), Value(30), Value(40) ) ).eval  

DeleteFrom( Variable("set1"), Value(20) ).eval            // deletes 20 from Set set1
DeleteFrom( Variable("set1"), Value(30), Value(1), Value(3) ).eval            // can pass multiple SetExpression for deleting but only matched ones are deleted, here only 30 is deleted from the Set as 1 and 3 are not part of the Set  

Variable("set1").eval                // returns Set(40)
```

### Contains(setExp: SetExpression, valueExp: SetExpression): Boolean
The `Contains` expression takes two arguments. The first is of type `SetExpression` which must evaluate to a `mutable.Set[Any]`. The second argument is also a `SetExpression` and it should evaluate to any value or data-type which we desire to know whether it is part of the Set evaluated from the first argument or not.
It returns a Boolean depends upon whether the value is in the Set or not.

Syntax/Code Example:
```
Assign("set1", SetIdentifier( Value(20), Value(30), Value(40) ) ).eval  
Contains( Variable("set1"), Value(5) ).eval                // returns false
Contains( Variable("set1"), Value(30) ).eval               // returns true 
```

### Equals(exp1: SetExpression, exp2: SetExpression): Boolean
The `Equals` expression take two arguments evaluates their value and compares them. If both of the evaluated values are equal, then it returns true otherwise false.

Syntax/Code Example:
```
Assign("var1", Value(20)).eval
Assign("var2", Value(50)).eval
Equals( Variable("var1"), Variable("var2") ).eval             // returns false as 20 does not equals 50
Equals( Variable("var1), Value(20) ).eval                     //returns true as both expression evaluate to value 20
```

## OOPS Features
This DSL supports Classes, Objects and Inheritance. There are four kinds of access modifiers: `Public`, `Protected`, `Private`, and `Default` for both fields and methods. Also, there can be only constructor and only single class inheritance is supported which means that a class can be constructed with or without only one parent class. A class can have atmost one direct super class.

Only `Public` and `Protected` members (both fields and methods) are inherited. `Public` members can be accessed anywhere given the reference of the object. `Protected`, `Private` and `Default` members are accessible within the body of the class.

Feature of dynamic dispatch have also been added. Fields and Methods of access level `Public` and `Private` are the only candidates for Dynamic Dispatch. Any try to access private, protected and default member using Class's instance will result in an `Exception`.
Also, any attempt to access private and default members of superClass will also result in an `Exception`.


## (Syntax and Semantics)

### ClassDef(className: String, classExpArgs: SetExpression*)
The `ClassDef` expression is used to create or define new classes. `className` represents the name of the class which will be stored in the binding environment. In the correct referencing environment, use can reference this class again with this `className`. `classExpArgs` are a Sequence of SetExpression representing Class members like Fields, Methods, Constructor, etc.

A Class can contain direct following members: CreateField, CreatePublicField, CreatePrivateField, CreateProtectedField, Constructor, Method, PublicMethod, PrivateMethod, ProtectedMethod, ClassDef and ClassDefThatExtends.

Syntax/Code Example:
```
ClassDef(
    "ClassOne",                                         // name of the class "ClassOne"
    CreatePublicField("f1"),                             // Field creation
    CreateProtectedField("f2"),
    Constructor(                                          // defining Constructor                  
        ParamsExp(Param("a"), Param("b")),
        SetField("f1", Variable("a") ),
        SetField("f2", Variable("b") ),
    ),
    PublicMethod(                                           // defining a public method
        "m2",
        ParamsExp(),
        Field("f2")
    )
).eval
```

### ClassDefThatExtends(cName: String, superClass: SetExpression, classExpArgs: SetExpression*)
The `ClassDefThatExtends` expression is used to create or define new classes similiar to `ClassDef`. However, with this, you will have the ability to create a class by inheriting some other class. `className` represents the name of the class which will be stored in the binding environment. In the correct referencing environment, use can reference this class again with this `className`. And `superClass` will be the reference to the class which this class is going to inherit. `classExpArgs` are a Sequence of SetExpression representing Class members like Fields, Methods, Constructor, etc.

Syntax/Code Example:
```
ClassDefThatExtends(
    "ClassTwo",
    ClassRef("ClassOne"),                               // Parent class's reference
    CreatePublicField("f1"),                             // Field creation
    CreateProtectedField("f2"),
    Constructor(                                          // defining Constructor                  
        ParamsExp(Param("a"), Param("b")),                // specifying the signature and variables for arguments
        SetField("f1", Variable("a") ),                     // setting the value for fields
        SetField("f2", Variable("b") ),
    ),
    PublicMethod(                                           // defining a public method
        "m2",
        ParamsExp(),
        Field("f2")
    )
).eval                   // creates a class of name `ClassTwo`
```

### ClassRef(className: String)
The `ClassRef` expression is used to refer to an existing class with na,e `className`.

Syntax/Code Example:
```
ClassRef("ClassOne").eval 
```

Usage: For example in class creation
```
ClassDefThatExtends(
    "ClassTwo",
    ClassRef("ClassOne"),                               // ClassRef is used to pass reference of parent class to create sub class
    CreatePublicField("f1"),                             // Field creation
    CreateProtectedField("f2"),
    Constructor(                                          // defining Constructor                  
        ParamsExp(Param("a"), Param("b")),                // specifying the signature and variables for arguments
        SetField("f1", Variable("a") ),                     // setting the value for fields
        SetField("f2", Variable("b") ),
    ),
    PublicMethod(                                           // defining a public method
        "m2",
        ParamsExp(),
        Field("f2")
    )
).eval                   // creates a class of name `ClassTwo` that extends/inherits "ClassOne" Class
```

### Inner Classes:
Inner classes can be created with just passing another ClassDef or ClassDefThatExtends (as per need) in the ClassDef or ClassDefThatExtends Expression.

Example
```
ClassDef(
    "OuterClass",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
    ),
    ClassDef(                          // creating an inner class named "InnerClass"
        "InnerClass",
        CreatePublicField("f2"),
        Constructor(
            ParamsExp(),
            SetField("f2", Value("inner_field_value"))
        )
    )
).eval
```

### ClassRefFromClass(className: String, classRef: SetExpression)
`ClassRefFromClass` can be used to reference inner classes from reference of outer class. It can be used in creating objects by instantiating inner class by first referencing outer class.

Example
```
ClassRefFromClass( "ClassTwo", ClassRef("ClassOne") ).eval       // this expression returns to the inner class "ClassTwo" of enclosing class "ClassOne"
```

### Param(s: String): String
`Param` is used to create a binding for argument that a class method accepts. It is used along with `ParamsExp` Expression.
```
Param("x").eval        // return "x"
```

### ParamsExp(paramExpArgs: SetExpression*)
`ParamsExp` is used to create agrument set signature for a method or constructor of class.

Syntax:
```
ParamsExp().eval    // no arguments
ParamsExp(Param("a"), Param("b")).eval          // create a signature for method that accepts two arguments and can be referenced in the body of that method by variables "a" and "b".
```

Usage:
```
ClassDef(
    "Class1",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp( Param("v1")),              // here constructor's argument list is defined as single Param with name "v1"
        SetField("f1", Variable("v1"))        // refered to "v1" param passed from invoker as Variable("v1")
    )
).eval
```

### Constructor(ParamsExp: SetExpression, cBodyExpArgs: SetExpression*)
`Constructor` expression is used to create constructor for Class which will be invoked when an object is created from that class.

Consider this Class definition:
```
ClassDef(
    "Class1",                      
    CreatePublicField("f1"),
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1")) 
    )
).eval
```

The first argument of the Constructor are ParamsExp Expression representing what kind of signature is used to instantiate objects. Other params are expression which are part of constructor body and are though of as instructions.
When an object is instantiated, the OO mechanism goes to the top most class first and initiate's its fields and then invoke top class's constructor with the params passed. If the signature (number of params) of Constructor and the params values do not match then it would result in an `Exception`.

### Field(fieldName: String): Any
`Field` expression is used to refer to a field of an object with its name within the body of the class. Usage of this expression on object intance outside body of class is prohibited. We have another expression for that behavior.

Syntax:
```
Field("fieldName")
```

Usage (example: Class Method)
```
ClassDef(
    "Class1",                      
    CreatePublicField("f1"),
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1")) 
    ),
    PublicMethod(
        "m1",
        ParamsExp(),
        Field("f1")                       // Public method m1 will return value of Field f1 once invoked
    )
).eval
```

### SetField(fieldName: String, exp: SetExpression)
This expression is used to set the value of field. The value to set is determined by the second argument which is SetExpression which evaluates to the value needed to be set for the field.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePublicField("f1"),
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))       // sets field "f1" to the param "v1" passed to Constructor
    )
).eval
```

### CreateField(fieldName: String)
`CreateField` is used to create a field for class. This field has default access which means that it cannot be accessed outside directly and will not be inherited.

Usage:
```
ClassDef(
    "Class1",                      
    CreateField("f1"),         // Creation of default field "f1"
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    )
).eval
```

### CreatePublicField(fieldName: String)
`CreatePublicField` is similar to `CreateField` and is used to create a field for class. This field has public access which means that it can be accessed outside directly and will be inherited.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePublicField("f1"),         // Creation of public field "f1"
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    )
).eval
```

### CreateProtectedField(fieldName: String)
`CreateProtectedField` is similar to `CreateField` and is used to create a field for class. This field has protected access which means that it cannot be accessed outside directly but will be inherited.

Usage:
```
ClassDef(
    "Class1",                      
    CreateProtectedField("f1"),         // Creation of protected field "f1"
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    )
).eval
```

### CreatePrivateField(fieldName: String)
`CreatePrivateField` is similar to `CreateField` and is used to create a field for class. This field has private access which means that it cannot be accessed outside directly and will not be inherited by any sub-class.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePrivateField("f1"),         // Creation of private field "f1"
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    )
).eval
```

### Method(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)
`Method` Expression is used to create a method with name `methodName` for the class. `argExp` is the set of params that this method needs to be invoked and `mBodyExpArgs` are the expression of its body or we can say that they are instructions of the method.
`Method` creates a method with default access modiifer which means that it cannot be accessed/invoked outside class's body directly and will not be inherited by any sub-class.
The last expression of method body will specify its return type.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePrivateField("f1"),       
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    ),
    Method(
       "m1",                                                        // method name "m1"
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### PublicMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)
`PublicMethod` Expression is similar to `Method`.
`PublicMethod` creates a method with public access modiifer which means that it can be accessed/invoked outside class's body directly and can be inherited by any sub-class.
The last expression of method body will specify its return type.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePrivateField("f1"),         
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    ),
    PublicMethod(
       "m1",                                                        // method name "m1"
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### ProtectedMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)
`ProtectedMethod` Expression is similar to `Method`.
`ProtectedMethod` creates a method with protected access modiifer which means that it cannot be accessed/invoked outside class's body directly but can be inherited by any sub-class.
The last expression of method body will specify its return type.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePrivateField("f1"),         
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    ),
    ProtectedMethod(
       "m1",                                                        // method name "m1"
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### PrivateMethod(methodName: String, argExp: SetExpression, mBodyExpArgs: SetExpression*)
PrivateMethod` Expression is similar to `Method`.
`PrivateMethod` creates a method with private access modiifer which means that it cannot be accessed/invoked outside class's body directly and also cannot be inherited by any sub-class.
The last expression of method body will specify its return type.

Usage:
```
ClassDef(
    "Class1",                      
    CreatePrivateField("f1"),         
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    ),
    PrivateMethod(
       "m1",                                                        // method name "m1"
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### NewObject(classRef: SetExpression, constructorArgs: SetExpression*): ObjectStruct
`NewObject` Expression is used to create new DSL objects out of a class. `classRef` evaluates to a ClassStruct reference. `constructorArgs` is a sequence of Expressions that will get passed as argument to class's constructor. Any mismatch in size passed and size expected would result in an `Exception`.

Syntax and Example:
```
NewObject( ClassRef("ClassOne"), Value(1) ).eval    // creates a new object by instantiating class ClassOne
```

One use case:
```
ClassDef(
    "Class1",                      
    CreatePublicField("f1"),         
    Constructor(
        ParamsExp( Param("v1")), 
        SetField("f1", Variable("v1"))
    )
).eval

Assign("object1", NewObject( ClassRef("Class1"), Value(20) )).eval     // assigns the object into a variable named "object1" which can be refered again in the code
```

### ClassRefFromObject(className: String, objRef: SetExpression)
`ClassRefFromObject` is used to refer to an inner class of the outer class such that the known object is an instance of the outer class. This type of object can still refer to inner classes of its own class.

Syntax and Example:
```
ClassRefFromObject("Class2", Variable("object1")).eval      // refering to inner class "Class2" of the class whose instance is the object that Variable("object1") refers to
```

Usage - Creating object from inner class:
```
ClassDef(
    "OuterClass",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
    ),
    ClassDef(                          // creating an inner class named "InnerClass"
        "InnerClass",
        CreatePublicField("f2"),
        Constructor(
            ParamsExp(),
            SetField("f2", Value("inner_field_value"))
        )
    )
).eval

Assign("obj1", NewObject( ClassRef("OuterClass") )).eval     // instance of outer class

// we will use the above object to create an object of "InnerClass" using "ClassRefFromObject" Expression

Assign("obj2", NewObject( ClassRefFromObject( "InnerClass", Variable("obj1) ) )).eval      // object of InnerClass
```

### FieldFromObject(fieldName: String, obj: SetExpression)
`FieldFromObject` is similar to `Field`. But it enables the user to access public fields of any object from the object itself.
Any attempt to use `FieldFromObject` for accessing default, protected or private fields will result in an `Exception`.

Syntax and Example:
```
ClassDef(
    "Class1",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
    )
).eval
Assign("obj1", NewObject( ClassRef("Class1") )).eval 

FieldFromObject("f1", Variable("obj1")).eval     // returns "field_value"
```