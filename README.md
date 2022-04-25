_# Rajat Kumar (UIN: 653922910)

## CS474 Homework 5 - Submission

### Set Theory DSL - SetPlayground

---

### Introduction
Set Theory DSL is a Domain Specific Language created to create classes, objects and perform simple operations on Sets. It is build on top of Scala 3. Set operations like Union, Intersection, Set Difference, Symmetric Set Difference and Cartesian are implemented with the help of expression. Other operations like Inserting and deleting items are also implemented. Language specific operations like assigning the values to variables, fetching those variables, macros and their evaluation and scopes (both named and anonymous scopes) are also implemented. Classes, Object are also allowed to create. Classes can inherit other classes and single class inheritance is supported. There are interfaces and abstract classes also. There is also the support of Nested Classes. Operations on Sets can be performed with the help of this DSLs' capabilities to enable object-oriented programming. Other important features like Dynamic Dispatch and access modifiers are also supported.
The functionality for control structures and exception handling has been introduced by adding support for If, If-Else, and Try-Catch exception handling expressions.
As part of homework 5, it is possible to perform partial evaluation on the set expressions and an additional optimization step has also been introduced, where certain `SetExpression` can be reduced according to the rules of the optimizing transformer function. 
This can decrease the complexity for evaluating the expression.

---

###  Instructions to run the Project

1. Install [IntelliJ](https://www.jetbrains.com/student/) (this link is for student edition, you can download other as well).
2. Use [gitHub](https://github.com/RJonMshka/CS474Homework1.git) repo for this project and clone it into your machine using git bash on Windows and terminal on MacOS.
3. Switch to the `homework5` branch if the default branch is something else.
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
16. There are five test files `src/test/scala/SetTheoryDSLTest.scala`, `src/test/scala/ClassesAndInheritanceDSLTest.scala`, `src/test/scala/InterfaceAndAbstractClassDSLTest.scala`, `src/test/scala/ControlStructuresDSLTest.scala` and `src/test/scala/PartialEvaluationDSLTest.scala`. The first one is concerned with testing Set Operations of DSLs' SetExpressions, the second one is focused towards testing the object-oriented features like Classes, Objects and inheritance of this DSL, the third test file tests the features of interfaces and abstract classes with their complex compositions, the fourth one is for testing control structures and exception handling capabilities of the DSL, and lastly, the fifth test suite is designed to test partial evaluation, optimizing transformers and map function of `SetExpression`.
17. You can add your own test cases in these files to test the project better.
18. To run test cases directly from terminal, enter the command `sbt compile test` or `sbt clean compile test`. Individual test suites can also be run from IntelliJ IDE.
19. To run the main file `SetTheoryDSLTest.scala`, enter the command `sbt compile run` or `sbt clean compile run` in the terminal.
20. The file `src/main/scala/SetTheoryDSL.scala` is the main implementation of Set Theory DSL.
21. Please explore, go through code, observe how these complex constructs are implemented and more importantly have fun coding and experimenting around.

---

### Implementation
The implementation of Set Theory DSL is done with the help of `Enumeration` or [enums](https://docs.scala-lang.org/scala3/reference/enums/enums.html) construct. Enum types are used as Set Expressions (or Instructions) for the DSL. `Mutable Maps` are used to store variables. The `Scope` class is the implementation of named and anonymous scopes whose instances hold a name, a pointer to the parent scope, their bindings or reference environment and a `Mutable Map` which holds the reference to its child scopes.
The support of Object-Oriented Programming (OOP) behavior is also implemented. Various `SetExpression`s are added as compared to the homework1 (branch `master`). `ClassStruct`, `MethodStruct`, `ObjectStruct` and some other constructs are used to create support for Classes, Objects, Fields, Methods and specifically dynamic dispatch and inheritance.
Interface, Abstract Class declaration and their composition to create sophisticated hierarchical structures are also featured as part of homework 3 (branch `homework3`).
As part of homework 4(branch `homework4`), control structures can be created using `If`, `IfElse` and `TryCatch` expressions. The control of evaluation changes when these expressions are introduced in the code. More details are discussed in the further sections.
A deep down implementation, syntax and semantics is covered in the next section.
As part of homework5(branch `homework5`), partial evaluation, optimizing transformer functions and map function has been implemented.

For more details please see the documentation below or even better, open the file where the source code exists.

---


## Evaluation, Optimization and Map

### Expression Evaluations - Partial and Total
All expressions are of type `SetExpression`.
Each expression or instruction need to be evaluated for its working. There are two types of evaluations for this DSL.

1. **Total Evaluation**: This can be accomplished by adding or calling eval by `.eval` at the end of the outermost expression. As soon as the eval is called, all the expressions which are part of the expression on which eval is called, will be evaluated and the whole expression is reduced to a single value. This DSL generally works with Set Theory, so, its Set expressions and operation expression returns `Set[Any]` type value. However, this DSL is not limited to just sets. It is capable of return value of type `Any` with expressions like Value, UnitExp, etc.
2. **Partial Evaluation (and optimization)**: Partial optimization can be achieved by calling method `evaluate` with syntax as `.evaluate`. This is actually a two part process, first is the true partial evaluation which modifies the SetExpression into another one by replacing values with variable references which are available in the binding environment of the scope active during the evaluation of that expression. If the resulting expression does not have any unknown variable reference left, it can be totally evaluated by just calling `.eval`. However, if there are still unknown variable references left in the expression, the expression cannot be reduced to a value. This allows the scope of optimization where the every part of the SetExpression is scanned for optimization. The optimizer tries to reduce the expression for simpler calculations based on some concrete rules of set theory. At the end, it again checks if the optimization has resulted in removal of unknown variable references from the expression and its body. If so, it is going to evaluate and reduced to a value, otherwise expression.

For example, the below code declares a variable with name `var1` and assign it a value of `1` (which is the evaluated value of `Value(3)` expression itself). Since, `Value(1)` is also an expression, but you do not need to call `eval` on it separately. You just have to call `eval` on the outermost expression which is `Assign` in this case.
```
Assign("var1", Value(1) ).eval     // total evaluation (no optimization)

```

Here is the syntax for partial evaluation:
```
SetIdentifier( Value(1) ).evaluete   // partial evaluation + optimization
```

The return type of `evaluate` is `Set[Any] | SetExpression | Any`. This seems confusing as it could have been just `Any` for simplicity.
However, this is deliberately done to show the user of the code that the expression actually result in SetExpression or Set[Any] or any other value.
This is because, this DSL has the capability to return any value from expression such as Value(AnyValue).

List of the expression which are checked for unknown variable references:
```
Value
Variable
Assign
UnnamedScope
NamedScope
TryCatch
Try
Catch
If
IfElse
Then
Else
SetIdentifier
Union
Intersection
SetDifference
SymDifference
CartesianProduct
Contains
Equals
InsertInto
DeleteFrom
Check
Macro
ComputeMacro
```
Other expressions are partially evaluated to themselves.

### Optimization
Several optimization steps are performed on `SetExpression` which is partially evaluated first.
These step refers to a process where the optimizer keeps getting called until the input expression is same as the resulting (optimized) expression which means that there is no scope for further optimization.

There are certain rules for optimization which are written very clearly in the code and are easy to understand.
One example is `Union(SetIdentifier(), SetIdentifier( Variable("x") )).evaluate`.
Executing the above code will result in partial evaluation and will reach the optimization step since there is an unknown variable in the expression.
However, as per set rules, if one of the set for calculating the union is empty, the result of union can simply be the other set.
So, the optimization step takes this rule into consideration and evaluates the above code to `SetIdentifer( Variable("x") )`.

This is only one the rules, there are rules for `SetIdentifier`, `Intersection`, `SetDifference`, `SymDifference`, `CartesianProduct` and even some on `If` and `IfElse` expression.

Once, the optimization step is done, the expression is again checked for completeness as in absence of unknown variable references. If there are no variables left (assuming they got eliminated by optimization step), then expression can be evaluated to a value.
However, if the unknown variable references still exist, then the expression is return by the evaluator.


### Map
This is an additional feature which has been implemented as part of `homework5`.
Generally a map takes a container of certain type and converts/maps it into container of another type. It also accepts a transformer function which responsible for changing every element in that container to a different type.

In this homework, SetExpression is considered a kind of container. This whole DSL is based on evaluating a singular expression which results into evaluating its child expressions in a certain way.
So, the map actually operates directly on SetExpression.

It also accepts one transformer function whose signature is as follows:
```
transformerFunction: SetExpression => SetExpression
```

It takes a SetExpression and returns another SetExpression. It's body decides how it transforms input to output.

For example a transformer function can look like below:
```
def tf(exp: SetExpression): SetExpression = exp match {
    case Value(v) => UnitExp
    case _ => exp
}
```
The above function maps any expression that matches Value to UnitExp and leaving other expressions as it is.

Signature of map function is:
```
SetExp.map(transformerFunction: SetExpression => SetExpression): SetExpression
```

For different expressions, map works a little differently.

For example, applying map on UnnamedScope does not change the UnnamedScope expression because it is considered a container for inner expression.
The map function would instead transform the expression which are part of body of UnnamedScope.

However, for certain other expressions, it would map directly to that expression instead (Unit for monoid). For example, UnitExp, Value(v), etc.
```
SetExp.map(transformerFunction: SetExpression => SetExpression): SetExpression = SetExp match {
    case UnnamedScope(scopeExpArgs*) => UnnamedScope(scopeExpArgs.map( transformerFunction(_) )*)
        .
        .
        .
    case _ => transformerFunction(this)
}
```

Usage:
```
// custom transformer function
def unionToSetTransformer(exp: SetExpression): SetExpression = exp match {
  case Union(s1, s2) => SetIdentifier()
  case _ => exp
}

UnnamedScope(
    Union(SetIdentifier(Value(1)), SetIdentifier(Variable("x"))),
    Union(SetIdentifier(Value(2)), SetIdentifier(Variable("y"))),
).map(
    unionToSetTransformer
).evaluate
```

There are two default transformer method already defined in the code named `defaultIfOptimizer` and `defaultIfElseOptimizer` which users can use and utilize for mapping. 
However, users are free to write their own transformers like the code in above snippet cell.
___

## Set Operation Syntax and Semantics

### Common Language Syntax

There are difference expressions in this DSL and their semantics and syntax are as follows:

### Value(value: Any): Any
`Value` expression takes one argument of type `Any` and totally evaluates to that argument itself, but partially evaluates to itself. It returns the evaluated value.
For example:
```
Value(1).eval  // returns 1

Value().evaluate   // return Value(1)
```
The above expression evaluates (returns) to `1`.
It can take any value, like Strings, Int, Float, Double and other data-types as well.

For example
```
Value(1).eval              // returns 1
Value(2.0f).eval           // returns 20.0f
Value("hello").eval        // returns "hello"
Value("hello").evaluate        // returns Value("hello")
Value(Value(1)).evaluate      // Value(1)   - also removes nested values
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

The partial evaluation is almost similar to this except one exception where a variable can be assigned an expression instead. 

```
Assign("set1", SetIdentifier(Variable("x")) ).evaluate   // since there is no variable named x in scope, set1 refers to SetIdentifier(Variable("x")) in the binding environment 
```


### Variable(varName: String): Any
The `Variable` expression is used to get the value of the variable stored in current referencing environment. It accepts the first argument as a String value which represents the name of the variable it is referring in the table.

For example:
```
Assign("var1", Value(10) ).eval
Variable("var1").eval      // This code returns 10

// Partial evaluation
Variable("var1").evaluate   // evaluates to 10
Variable("unknown").evaluate   // evaluates to Variable("unknown")

// Inside a set expression
SetIdentifier( Variable("var1"), Variable("unknown") ).evaluate    // evaluates to SetIdentifier( Value(10), Variable("unknown") )
```

We can use Variables to store any kind of data type as well as Sets, Macros and other variables also.

### Macro(macroExpression: SetExpression): SetExpression
`Macro`s are used for their lazy evaluation. Instead of evaluating the `macroExpression`, it simply returns that `macroExpression`. This helps us to store the expression itself in a variable.
Macro's partial evaluation only results in partially evaluating its inner expression.

Syntax or usage:
```
Assign("macro1", Macro( Variable("var3") )).eval        // Stores "Variable("var3")" into a variable named "macro1"
```

This can be helpful in situation where we want to evaluate certain expression whose argument are not yet defined or are not the part of current referencing environment.
In the next section, we will see how to compute these macros on demand.

### ComputeMacro(macroExpression: SetExpression): Any
The `ComputeMacro` expression computes a particular macro and returns its evaluated value.
ComputeMacro's partial evaluation only results in partially evaluating its inner expression.

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

### NamedScope(scopeName: String, scopeExpArgs: SetExpression*): Any
The `NamedScope` expression is used to create a named scope block. We can even nest multiple scope blocks with it. A NamedScope's bindings can be accessed again in the code if the NamedScope created is a direct children on current scope.
It takes first argument as scopeName which is a String. `scopeExpArgs` are a sequence of arguments passed to it. It can be more than one. The concept is that you can execute more than one instruction in a particular scope without re-writing its name.

Partial Evaluation: If there are unknown variable in expression, its partial evaluation returns another NamedScope with its inner elements getting partially evaluated and optimized as well.

It returns the last expression evaluated or expression (in case of partial evaluation) in the scope body.
For Example:

```
NamedScope("scope1", 
    Assign( "var1", Value(20) )
).eval                                // Create a scope whose binding environment will have one variable named var1 and whose value will be 20

// Partial Evaluation
NamedScope("scope1", 
    SetIdentifier(Variable("x"))
).evaluate                              // evaluates to NamedScope("scope1", SetIdentifier(Variable("x")))  
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

### UnnamedScope(scopeExpArgs: SetExpression*): Any
The `UnnamedScope` expression is similar to `NamedScope` expression above. However, there are few differences. It does not have a name. We only specify the SetExpression(s) to execute in that scope. Also, since, it does not have a name, it will not be accessible to its parent once it is closed. If any named or Unnamed scopes are created inside it, they will also not be accessing if this scope closes.
It's similar to NamedScope in the sense that they share almost the same syntax except name as the first argument.
It returns the last expression evaluated or expression (in case of partial evaluation) in the scope body.
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
The `SetIdentifier` returns a mutable Set of Any data-type. The syntax is pretty simple, we can pass zero or more `SetExpression`s to it, and it will evaluate all of them and place them in a mutable Set.
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
The `CartesianProduct` expression takes exactly two arguments like `Union`, both of type `SetExpression`. They both must evaluate to a Set of type `mutable.Set[Any]`. This expression performs the Cartesian product operation of two sets and return another set with elements in the form (a, b), where `a` is every element of first set and b is every element of second set.

Syntax/Example:
```
Assign("cp_set", CartesianProduct( SetIdentifier(Value(1), Value(2)), SetIdentifier(Value(3), Value(4)) ) ).eval
Variable("cp_set").eval      // returns Set( (1, 3), (1, 4), (2, 3), (2, 4) )
```

### InsertInto(setExp: SetExpression, setExpArgs: SetExpression*): collection.mutable.Set[Any]
The `InsertInto` expression take the first argument as a `SetExpression` which must resolve or evaluate to a `mutable Set`. The other argument(s) are also `SetExpression` type, but they can be more than one. These `SetExpression`s are evaluated to their respective values and are then inserted into the Set evaluated from the first argument.

Syntax/Example:
```
Assign("var1", Value("insertValue") ).eval                 // creating a variable
Assign("set1", SetIdentifier( Value(20) ) ).eval           // creating a set with one Int 20 in it

InsertInto( Variable("set1"), Value(30), Variable("var1") ).eval         // inserting into the set set1 30 as well as value of variable var1 which is "insertValue"
Variable("set1").eval                                                    // Would return Set(20, 30, "insertValue")
```

### DeleteFrom(setExp: SetExpression, setExpArgs: SetExpression*): collection.mutable.Set[Any]
The `DeleteFrom` expression deletes one or many values from a Set. The first argument is a `SetExpression` which should resolve to a `mutable.Set[Any]` and other arguments are also of type `SetExpression` which are the values that need to be removed from the Set. Multiple values can be removed with one expression.

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

## Object-Oriented Design Features
This DSL supports Classes, Objects and Inheritance. There are four kinds of access modifiers: `Public`, `Protected`, `Private`, and `Default` for both fields and methods. Also, there can be only constructor and only single class inheritance is supported which means that a class can be constructed with or without only one parent class. A class can have to the most one direct super class.

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
    Method(                                           // defining a public method
        "m2",
        PublicAccess(),
        ParamsExp(),
        Field("f2")
    )
).eval
```

### ClassDef(className: String, classExpArgs: SetExpression*) [with `Extends` functionality]
### Extends(classRef: SetExpression)
The `ClassDef` can be used to inherit another class with just specifying one `Extends` expression with the above signature in the body of the class. This expression can only appear once and can only take one class reference as argument.
This means that a class can only inherit a single class.

Syntax/Code Example:
```
ClassDef(
    "ClassTwo",
    Extends(ClassRef("ClassOne")),                               // Parent class's reference
    CreatePublicField("f1"),                             // Field creation
    CreateProtectedField("f2"),
    Constructor(                                          // defining Constructor                  
        ParamsExp(Param("a"), Param("b")),                // specifying the signature and variables for arguments
        SetField("f1", Variable("a") ),                     // setting the value for fields
        SetField("f2", Variable("b") ),
    ),
    Method(                                           // defining a public method
        "m2",
        PublicAccess(),
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
ClassDef(
    "ClassTwo",
    Extends(ClassRef("ClassOne")),                               // ClassRef is used to pass reference of extend expression to create sub class
    CreatePublicField("f1"),                             // Field creation
    CreateProtectedField("f2"),
    Constructor(                                          // defining Constructor                  
        ParamsExp(Param("a"), Param("b")),                // specifying the signature and variables for arguments
        SetField("f1", Variable("a") ),                     // setting the value for fields
        SetField("f2", Variable("b") ),
    ),
    Method(                                           // defining a public method
        "m2",
        PublicAccess(),
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
`ParamsExp` is used to create argument set signature for a method or constructor of class.

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
`Field` expression is used to refer to a field of an object with its name within the body of the class. Usage of this expression on object instance outside body of class is prohibited. We have another expression for that behavior.

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
    Method(
        "m1",
        PublicAccess(),
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
`CreatePrivateField` is similar to `CreateField` and is used to create a field for class. This field has private access which means that it cannot be accessed outside directly and will not be inherited by any subclass.

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

### Method(methodName: String, accessProp: AccessProperties, argExp: SetExpression, mBodyExpArgs: SetExpression*)
`Method` Expression is used to create a method with name `methodName` for the class. `argExp` is the set of params that this method needs to be invoked and `mBodyExpArgs` are the expression of its body, or we can say that they are instructions of the method.
`Method` creates a method with default access modifier which means that it cannot be accessed/invoked outside class's body directly and will not be inherited by any subclass.
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
    "m1",                                                            // method name "m1"
       DefaultAccess(),                                              // default access modifier   
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### PublicMethod Syntax
### Method(methodName: String, accessProp: AccessProperties, argExp: SetExpression, mBodyExpArgs: SetExpression*)
A Public access level can be created with `Method` expression by adding a `PublicAccess()` expression as its second argument.
This creates a method with public access modifier which means that it can be accessed/invoked outside class's body directly and can be inherited by any subclass.
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
       PublicAccess(),                                              // Public Access Modifier
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### ProtectedMethod Syntax
### Method(methodName: String, accessProp: AccessProperties, argExp: SetExpression, mBodyExpArgs: SetExpression*)
A Protected access level can be created with `Method` expression by adding a `ProtectedAccess()` expression as its second argument.
This creates a method with protected access modifier which means that it cannot be accessed/invoked outside class's body directly but can be inherited by any subclass.
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
       ProtectedAccess(),                                              // Protected Access Modifier
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### PrivateMethod Syntax
### Method(methodName: String, accessProp: AccessProperties, argExp: SetExpression, mBodyExpArgs: SetExpression*)
A Private access level can be created with `Method` expression by adding a `PrivateAccess()` expression as its second argument.
This creates a method with private access modifier which means that it cannot be accessed/invoked outside class's body directly and also cannot be inherited by any subclass.
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
       PrivateAccess(),                                              // Private Access Modifier
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### Default Access Method Syntax
### Method(methodName: String, accessProp: AccessProperties, argExp: SetExpression, mBodyExpArgs: SetExpression*)
A Default access level can be created with `Method` expression by adding a `DefaultAccess()` expression as its second argument.
This creates a method with private access modifier which means that it cannot be accessed/invoked outside class's body directly and also cannot be inherited by any subclass.
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
       DefaultAccess(),                                              // Default Access Modifier
       ParamsExp( Param("x")),                                      // param signature for method
       SetField("f1", Variable("x")),                               // start of method body
       Field("f1")                                              // end of method body - last expression, so this method return value of field "f1"
    )
).eval
```

### NewObject(classRef: SetExpression, constructorArgs: SetExpression*): ObjectStruct
`NewObject` Expression is used to create new DSL objects out of a class. `classRef` evaluates to a ClassStruct reference. `constructorArgs` is a sequence of Expressions that will be passed as argument to class's constructor. Any mismatch in size passed and size expected would result in an `Exception`.

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

### FieldFromObject(fieldName: String, obj: SetExpression): Any
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

### SetFieldFromObject(fieldName: String, obj: SetExpression, exp: SetExpression)
`SetFieldFromObject` is similar to `SetField`. It is used to set public field of an object from outside the body of class by directly referencing object to a particular value determined by evaluating `exp` SetExpression.
Any attempt to use `SetFieldFromObject` for changing default, protected or private fields will result in an `Exception`.

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

SetFieldFromObject("f1", Variable("obj1"), Value("hello")).eval     // changes the value of "f1" for "obj1" to "hello"

FieldFromObject("f1", Variable("obj1")).eval     // returns "hello"
```

### InvokeMethod(methodName: String, params: SetExpression*): Any
`InvokeMethod` is used to invoke methods of class within the class body. For example, a class method will use InvokeMethod in one of its functions body to execute any other method from its own body.
`methodName` specifies the name of the method and `params` specifies the signature of param the method receives. Signature in this DSL only refers to name of method and number of params for that method.


Syntax and Example:
```
ClassDef(
    "Class1",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
        InvokeMethod("m1", Value(20))                   // when called will return Set(10, 20, 30)
    ),
    Method(
        "m1",
        PublicAccess(), 
        ParamsExp(Param("a")),
        Union( SetIdentifier(Value(10), Value(30)), SetIdentifier(Variable("a")) )
    )
).eval
```

### InvokeMethodOfObject(mName: String, objRef: SetExpression, params: SetExpression*)
Similar to `InvokeMethod` but is used to invoke method from outside the class body or from referencing object.
This expression can only invoke public methods. Any attempt to invoke default, private and protected methods will lead to an `Exception`.

Syntax and Example:
```
ClassDef(
    "Class1",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
    ),
    Method(
        "m1",
        PublicAccess(), 
        ParamsExp(Param("a")),
        Union( SetIdentifier(Value(10), Value(30)), SetIdentifier(Variable("a")) )
    )
).eval

Assign("obj1", NewObject( ClassRef("Class1") )).eval 

InvokeMethodOfObject("m1", Variable("obj1"), Value("abc") ).eval    // returns Set(10, 30, "abc")
```

### ObjectInstanceOf(objectRef: SetExpression, classRef: SetExpression): Boolean
This expression returns `true` if object returned by evaluating `objectRef` is an instance of class returned by evaluating `classRef`.

Usage:
```
ClassDef(
    "Class1",                      // normal class creation
    CreatePublicField("f1"),
    Constructor(
        ParamsExp(),
        SetField("f1", Value("field_value"))
    ),
    Method(
        "m1",
        PublicAccess(), 
        ParamsExp(Param("a")),
        Union( SetIdentifier(Value(10), Value(30)), SetIdentifier(Variable("a")) )
    )
).eval

Assign("obj1", NewObject( ClassRef("Class1") )).eval 

ObjectInstanceOf( Variable("obj1"), ClassRef("Class1") ).eval      // return true

ObjectInstanceOf( Variable("obj1"), ClassRef("ClassX") ).eval       // return false - given ClassX class was already declared
```

### ClassRefFromInterface(className: String, intRef: SetExpression)
`ClassRefFromInterface` is used to refer to an inner class of the outer interface.
Usage:
Usage - Creating object from inner class of an interface:
```
InterfaceDef(
    "OuterInterface",                      // normal interface creation
    CreatePublicField("f1"),
    ClassDef(                          // creating an inner class named "InnerClass"
        "InnerClass",
        CreatePublicField("f2"),
        Constructor(
            ParamsExp(),
            SetField("f2", Value("inner_field_value"))
        )
    )
).eval
// we will use the above object to create an object of "InnerClass" using "ClassRefFromInterface" Expression

Assign("obj1", NewObject( ClassRefFromInterface( "InnerClass", InterfaceRef("OuterInterface") ) )).eval      // object of InnerClass
```

### InterfaceDef(intName: String, interfaceExpArgs: SetExpression*)
`InterfaceDef` expression is used to create or define an interface. The syntax is such that the first argument is the name of the interface, other arguments will be the expression(s) representing body of the interface.
An interface can have all access level fields same as classes. It can extend a single interface and cannot implement other interface.
It can have all access level method with addition of `abstract` and default level implementation methods.
An `abstract` does not have any implementation. There is no specific keyword to represent an abstract method.
It can be viewed as a method expression with `no` body expression arguments.

For an interface, the method that contains a body, is a `default` implementation level method. That method has some default implementation and is not considered a fully concrete method.

Only methods with `public` and `protected` access can be inherited by sub-interfaces and subclasses.

An interface can be implemented by many classes and a single class can implement many interfaces.

Usage and Example:
```
InterfaceDef(
    "OuterInterface",                      // normal interface creation
    CreatePublicField("f1")
).eval
```

With default method:
```
InterfaceDef(
    "OuterInterface",                      // normal interface creation
    CreatePublicField("f1"),
    Method(
        "m1",                   // a default method
        PublicAccess(), 
        ParamsExp(Param("a")),
        Variable("a")
    )
).eval
```

With abstract method:
```
InterfaceDef(
    "OuterInterface",                      // normal interface creation
    CreatePublicField("f1"),
    Method(
        "m1",                   // an abstract method
        PublicAccess(), 
        ParamsExp(Param("a"))
).eval
```

### Extends(superInterfaceRef: SetExpression)
One interface can extend or inherit other interface by adding single `Extends` expression in its body and passing a reference to another Interface object which will be the parent of current interface being declared.

It will inherit Public, Protected fields and methods from superinterfaces (go up the parent hierarchy).

Usage:
```
InterfaceDef(
    "I1",                      // normal interface creation
    Extends(InterfaceRef("I2")),   // extends another interface with name I2
    CreatePublicField("f1"),
    Method(
        "m1",                   // an abstract method
        PublicAccess(), 
        ParamsExp(Param("a"))
).eval
```

### AbstractClassDef(className: String, classExpArgs: SetExpression*)
An `AbstractClassDef` expression is used to create an abstract class which will have abstract (without implementation) methods.

There should be at least one abstract method belonging to the abstract class, otherwise an Exception will be thrown.

An abstract class can extend other concrete or abstract classes and can even implement interfaces. However, after inheriting and implementing, there should be at least one abstract method left that belong to the abstract class.

Usage and Example:
```
AbstractClassDef(                                   // abstract class definition
    classOneName,
    Implements(InterfaceRef(interfaceTwoName)),    // can implement interface
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method with no implementation
        "m1",
        PublicAccess(),
        ParamsExp()
    )
).eval
```

### Implements(interfaceRefs: SetExpression*)
The `Implements` expression is used by a class (concrete or abstract, applies to both) to implement multiple interfaces.
These interfaces will serve as superinterfaces for the class.

If the class implementing interfaces is abstract then it need not provide implementation for all the abstract methods of interfaces.

However, if the class implementing interfaces is concrete then it must or its super classes must implement methods of interfaces.

The way `Extends` and `Implements` work is that first the control goes to the top most parent class. There it determines `default`, `abstract` and `implemented` inheritable (public and protected) methods.

Then is finds all the inherited superinterfaces. If an interface appears again in the chain which has already been inherited, it is discarded as all its methods are already captured.

Then abstract methods of class and interfaces are merged to form total abstract class.

Similar approach is used for `Default` method but without any intervention from classes as classes cannot create default methods. That is solely interface's feature.

All these methods are backed down till the current class which is formed by implementing other interfaces and extending other class.

Usage:
```
ClassDef(                             
    classOneName,
    Implements(InterfaceRef(interfaceTwoName)),    // implementing an interface
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval

ClassDef(                             
    classOneName,
    Implements(InterfaceRef("I1"), InterfaceRef("I2")),    // implementing multiple interfaces interface
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval
```


### InterfaceRef(intName: String)
`InterfaceRef` is used to refer to an interface object with the passed name. If the interface does not exist, it searches the scope's hierarchy until it finds one or throws an error of not found type.

Usage:
```
ClassDef(                             
    classOneName,
    Implements(InterfaceRef(interfaceTwoName)),    // InterfaceRef used to refer to another interface for implementation
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval
```


### InterfaceRefFromClass(intName: String, classRef: SetExpression)
`InterfaceRefFromClass` expression is used to refer to an interface from a class reference whose member interface it is.

Usage:
```

ClassDef(
    "c1",
    InterfaceDef(                           // interface declaration as a member of class
        "I1",
        CreatePublicField("f2")
    )

).eval
ClassDef(                             
    classOneName,
    Implements( InterfaceRefFromClass("I1", ClassRef("c1") )),    // InterfaceRefFromClass used to refer to another interface which is a member of a known class
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval
```

### InterfaceRefFromInterface(intName: String, intRef: SetExpression)
`InterfaceRefFromInterface` expression is used to refer to an interface from another interface as a member interface.

Usage:
```

InterfaceDef(
    "I1",
    InterfaceDef(                           // interface declaration as a member of another interface
        "I2",
        CreatePublicField("f2")
    )

).eval
ClassDef(                             
    classOneName,
    Implements( InterfaceRefFromInterface("I2", InterfaceRef("I1") )),    // InterfaceRefFromInterface used to refer to another interface which is a member of a known interface
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval
```


### InterfaceRefFromObject(intName: String, objRef: SetExpression)
`InterfaceRefFromObject` expression is used to refer to an interface from an object whose class's member interfaces have that interface in them.

Usage:
```

ClassDef(                                   // class declaration
    "c1",
    Constructor(
        ParamsExp()
    ),
    InterfaceDef(                           // interface declaration as a member of another class
        "I2",
        CreatePublicField("f2")
    )

).eval

Assign("obj1", NewObject( ClassRef("c1") ))
ClassDef(                             
    classOneName,
    Implements( InterfaceRefFromObject("I2", Variable("obj1") )),    // InterfaceRefFromObject used to refer to another interface from an object
    Constructor(
        ParamsExp()
    ),
    Method(                             // a public method (concrete)
        "m1",
        PublicAccess(),
        ParamsExp(),
        Value(20)
    )
).eval
```

## Questions to Answer as part of Homework 3:

### Can a class/interface inherit from itself?
`
Answer: No, it cannot. The syntax for Implementation and Inheritance is such that the call for inheriting or implementing is done at the time when the class is not yet available in binding environment.
Since, there is no binding of class or interface available, it cannot finds itself for inheriting or implementing.
`

### Can an interface inherit from an abstract class with all pure methods?
`
Answer: No, it cannot. It is because Class and Interface are totally different constructs in this DSL and cannot be substituted easily.
`

### Can an interface implement another interface?
`
Answer: No. An interface can only `Extend` another interface (single one).
Only classes can implment interfaces.
`

### Can a class implement two or more different interfaces that declare methods with exactly the same signatures?
`
Answer: Yes, it can. It starts by selecting the left most super interface. Extract all its methods, and then moves to another and if same signature is detected or found, it is ignored.
`

### Can an abstract class inherit from another abstract class and implement interfaces where all interfaces and the abstract class have methods with the same signatures?
`
Answer: Yes. An abstract class can inherit from another abstract class. The logic of above question applies, there will not be any ambiguous reference, methods with same signature is considered as a single method.
`

### Can an abstract class implement interfaces?
`
Answer: Yes it can. However, after implementing (and many be extending some class), it should have at least one abstract method in its hierarchy which is not implemented by any subclass.
`

### Can a class implement two or more interfaces that have methods whose signatures differ only in return types?
`
Answer: The return type is not part of signature in this DSL, so those methods will be indistinguishable in terms of signature and can be treated as one.
`

### Can an abstract class inherit from a concrete class?
`
Answer: Yes. But it have to have at least one abstract class in its hierarchy or an Exception is thrown.
`

### Can an abstract class/interface be instantiated as anonymous concrete classes?
`
Answer: No, they cannot be. There is no way to instantiate an interface in this DSL. And abstract classes will throw an Exception upon instantitation.
`

## Control Structures Syntax and Semantics
___

### If(ConditionExp: SetExpression, thenClause: SetExpression): Any
`If` expression is used as a control structure which acts as a branching structure and evaluates its `thenClause` when the evaluation of `condition` expression is a true value.
The `If` expression return the evaluation of last expression of then branch if condition is true otherwise it evaluates to Unit (partially evaluate to UnitExp).
Usage / Example:
```
If( Check( Equals(Variable("var1"), Value(20)) ),     // condition expression as first argument
  Then(                                               
    InsertInto( Variable("set1"), Value(50))            // body of then clause, evaluated when condition evaluates to a true value
  )                                                  // Then expression as second argument
).eval
```

### IfElse(ConditionExp: SetExpression, thenClause: SetExpression, elseClause: SetExpression): Any
`IfElse` expression is used as a control structure which acts as a branching structure and evaluates its `thenClause` when the evaluation of `condition` expression is a true value, otherwise evaluates its `elseClause` expression.
The usage is very similar to `If` expression above.
The `IfElse` expression return the evaluation of last expression of whichever branch is evaluated.

Usage / Example:
```
IfElse( Check( Equals(Variable("var4"), Value(20)) ),  // condition expression
  Then(
    InsertInto( Variable("set4"), Value(50))            // then body expressions
  ),                                                    // thenClause expression                     
  Else(
    InsertInto( Variable("set4"), Value(20))            // else body expressions
  )                                                     // elseClause expression
).eval
```

### Check(exp: SetExpression)
`Check` expression is a complementary addition which helps to build `condition` expression for `If` and `IfElse` expressions.
This expression take another expression and evaluates it to either a true value or a false value based on a specific DSL logic.

Usage / Example:
```
If( Check( Equals(Variable("var1"), Value(20)) ),     // here Check expression is used for evaluating the condition for If expression
  Then(                                               
    InsertInto( Variable("set1"), Value(50))            
  )                                                
).eval
```

### Then(expSeq: SetExpression*)
`Then` expression is used to act as a wrapper for multiple expression to executed in the `thenClause` part of `If` and `IfElse` expressions.

Usage / Example:
```
If( Check( Equals(Variable("var1"), Value(20)) ),     
  Then(                                               
    InsertInto( Variable("set1"), Value(50))  
    InsertInto( Variable("set1"), Value(20))        
  )                                                // Then expression is used to wrap expressions for then clause
).eval


IfElse( Check( Equals(Variable("var4"), Value(20)) ),  
  Then(
    InsertInto( Variable("set4"), Value(50))            // then body expressions
  ),                                                    // Then expression - here used with IfElse                  
  Else(
    InsertInto( Variable("set4"), Value(20))            
  )                                                     
).eval
```

### Else(expSeq: SetExpression*)
`Else` expression is used to act as a wrapper for multiple expression to executed in the `elseClause` part of `IfElse` expression.

Usage / Example:
```
IfElse( Check( Equals(Variable("var4"), Value(20)) ),  
  Then(
    InsertInto( Variable("set4"), Value(50))        
  ),                                                                     
  Else(
    InsertInto( Variable("set4"), Value(20))            
  )                                                  // Else Expression wraps elseClause expression(s)   
).eval
```

### TryCatch(tryExp: SetExpression, catchExpSeq: SetExpression*)
`TryCatch` expression is used for writing code that can throw exception in the DSL. The code/expressions that needs to be protected are supposed to be added in this expression.
It has two parts. the first is `Try` expression and the second is `Catch`, both of them are mentioned in detail later in the documentation 
`TryCatch` can only have one `Try` expression (as its first argument) but it does support multiple `Catch` expressions.
First, the `Try` expression is evaluated and its inner expressions are also evaluated until an exception is thrown.
Once, the exception is thrown, the further evaluation stops until `Catch` expressions are encountered in the code. 
Among multiple `Catch` expressions, the first that matches the thrown exception is evaluated and the exception is passed to it and handled accordingly.

Usage/Example:
```
TryCatch(                                           // TryCatch Expression
  Try(                                          
    InsertInto(Variable("set8"), Value(50)),
    ThrowNewException(ClassRef("c1"), Value(exceptionCause)),       // An exception is thrown
    InsertInto(Variable("set8"), Value(100)),                       // will not be evaluated
  ),                                                // Try Expression
  Catch("e1", ClassRef("c1"),
    InsertInto(Variable("set8"), FieldFromObject("cause", Variable("e1")))          // Exception handling
  )                                                     // catch matches the exception from ClassRef
).eval
```

### Try(expSeq: SetExpression*)
The `Try` expression acts as a wrapper to hold the expression that need to evaluated in the try block of `TryCatch` expression.
`expSeq` is a sequence of `SetExpression` which are evaluated until an exception is encounter like in the code snippet shown above.

Usage / Example:
```
TryCatch(                                           // TryCatch Expression
  Try(                                          
    InsertInto(Variable("set8"), Value(50)),
    ThrowNewException(ClassRef("c1"), Value(exceptionCause)),       // An exception is thrown
    InsertInto(Variable("set8"), Value(100)),                       // will not be evaluated
  ),                                                // Try Expression - can add or nest more expression in it
  Catch("e1", ClassRef("c1"),
    InsertInto(Variable("set8"), FieldFromObject("cause", Variable("e1")))          // Exception handling
  )                                                     // catch matches the exception from ClassRef
).eval
```

### Catch(eName: String, eType: SetExpression, catchExpSeq: SetExpression*)
`Catch` expression is used for handling exception that are thrown anywhere inside it corresponding `Try` expression.
If an exception is thrown in `Try` expression, like in snippet above, no further expression is evaluated until catch expressions are encountered.
If a matching `Catch` expression is found, then it body is evaluated. 
Before the evaluation of its body, exception is passed to its binding environment (scope) and stored with a `Variable` named `eName`.
`eType` is for matching the type of exception with it.
`catchExpSeq` are the sequence of `SetExpression` that constitute the body of this `Catch` expression.

Usage/Example:
```
TryCatch(                                           // TryCatch Expression
  Try(                                          
    InsertInto(Variable("set8"), Value(50)),
    ThrowNewException(ClassRef("c1"), Value(exceptionCause)),       // An exception is thrown
    InsertInto(Variable("set8"), Value(100)),                       // will not be evaluated
  ),                                                // Try Expression 
  Catch("e1", ClassRef("c1"),                   // e1 is the name of variable that stores exception and ClassRef("c1") is the matching type
    InsertInto(Variable("set8"), FieldFromObject("cause", Variable("e1")))          // Exception handling - body of Catch
  )                                                     // Catch expression
).eval


// Pairing/chaining multiple catch together
TryCatch(
  Try(
    InsertInto(Variable("set12"), Value(50)),
    ThrowNewException(ClassRef(exceptionClassName1), Value(exceptionCause)),
    InsertInto(Variable("set12"), Value(100)),
  ),
  Catch("e1", ClassRef(exceptionClassName2),
    InsertInto(Variable("set12"), FieldFromObject("cause", Variable("e1")))
),                                                                           // if this Catch is not matched, then exception is sent to be matched with next one
  Catch("e1", ClassRef(exceptionClassName1),
    InsertInto(Variable("set12"), Value(200))
  )                                                                          // if this matches, the exception is handled, if not the exception further propagates to outer scopes, stopping further evalutions
).eval
```

### ExceptionClassDef(className: String, classExpArgs: SetExpression*)
The `ExceptionClassDef` expression is used to create an Exception class.
The class must be a `concrete` one.
There is a strict template that needs to be followed for it to work properly.
It must have a public field named `cause`.
Its value must be passed to the field by passing argument to its constructor. That is abstracted with `ThrowNewException` expression mentioned in the next section.

The minimal template example is:
```
ExceptionClassDef("DSLException",       // Name of Exception class will be "DSLException"
  CreatePublicField("cause"),          // This must be created
  Constructor(                   
    ParamsExp(Param("passedCause")),              // value for field need to be passed in constructor
    SetField("cause", Variable("passedCause"))    // set the value of cause field
  )
).eval
```
You can add other methods, fields and event construct exception classes using class Inheritance that we have already implemented and demonstrated.
But the above template is minimal one. The users of this language are free to create their own hierarchy of the Exceptions.


### ThrowNewException(exceptionClassRef: SetExpression, exceptionCause: SetExpression)
The `ThrowNewException` expression is used to throw an exception which is an instance of the class object that `exceptionClassRef` evaluates to.
Also, there is one restriction here, the `exceptionCause` parameter must evaluate to a string. 
This represents the Exception cause/ message which can be accessed inside the `Catch` expression by referring to the `cause` field of exception object passed to it.

The propagation of the exception is already discussed above.
If the exception is not handled and global scope is reached, a Java `Exception` is thrown to the compiler will message `Unahndled Exception`.

Usage/Example:
```
TryCatch(
  Try(
    InsertInto(Variable("set12"), Value(50)),
    ThrowNewException(ClassRef("c1"), Value("my exception cause")),     // An exception which is an instance of class object named "c1" is thrown or propagated through the scope chain with the cause of "my exception cause"
    InsertInto(Variable("set12"), Value(100)),
  ),
  Catch("e1", ClassRef(exceptionClassName2),
    InsertInto(Variable("set12"), FieldFromObject("cause", Variable("e1")))  // evaluating this expression will lead to inserting the "my exception cause" string to Set "set12"
  )
).eval

// Partial evaluation
TryCatch(
  Try(
    InsertInto(Variable("set12"), Value(50)),
    ThrowNewException(ClassRef("c1"), Value("my exception cause")),     
    InsertInto(Variable("set12"), Value(100)),
  ),
  Catch("e1", ClassRef(exceptionClassName2),
    InsertInto(Variable("set12"), FieldFromObject("cause", Variable("e1")))  
  )
).evaluate
```

## Additional Features:

### GarbageCollector
This expression is used to reset the bindings manually. This helps in writing unit test cases so that test cases won't interfere with each other's functionalities and scopes.
Using it often may hamper with the functionality of the DSL. 
It is not advised to use it as a regular Set Expression. But the choice is to the user to utilize it wisely.

Usage:
```
GarbageCollector.eval

// Partial evaluation
GarbageCollector.evaluate
```





___
Those are all the Data-types and expressions of DSL Set Theory as of now.
More exciting stuff coming ahead.