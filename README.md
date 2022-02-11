# Rajat Kumar (UIN: 653922910)

## CS474 Homework 1 - Submission

### Set Theory DSL - SetPlayground

---

### Introduction
Set Theory DSL is a Domain Specific Language created to perform simple operations on Sets. It is build on top of Scala 3. Set operations like Union, Intersection, Set Difference, Symmetric Set Difference and Cartesian are implemented with the help of expression. Other operations like Inserting and deleting items are also implemented. Language specific operations like assigning the values to variables, fetching those variables, macros and their evaluation and scopes (both named and anonymous scopes) are also implemented. Other than that you can also, equate expression and check whether a particular value is in the set or not.

---

###  Instructions to run the Project

1. Install [IntelliJ](https://www.jetbrains.com/student/) (this link is for student edition, you can download other as well).
2. Use [github](https://github.com/RJonMshka/CS474Homework1.git) repo for this project and clone it into your machine using git bash on Windows and terminal on MacOS.
3. Switch to the `master` branch if the default branch is something else.
4. Open IntelliJ and go to `File > New > Project from existing Source`, or `File > New > Project from version control`. For the second option, you have directly provide git repo link and no need to clone the git repo separately.
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
16. `src/test/scala/SetTheoryDSLTest.scala` is the file where all the test cases are written. This is the file you need to run to test the project. 
17. You can add your own test cases in it to test the project better.
18. To run test cases directly from terminal, enter the command `sbt compile test` or `sbt clean compile test`.
19. To run the main file `SetTheoryDSLTest.scala`, enter the command `sbt compile run` or `sbt clean compile run` in the terminal.
20. The file `src/main/scala/SetTheoryDSL.scala` is the main implementation of Set Theory DSL.
21. Please explore and have fun.

---

### Implementation
The implementation of Set Theory DSL is done with the help of `Enumeration` or [enums](https://docs.scala-lang.org/scala3/reference/enums/enums.html) construct. Enum types are used as Set Expressions (or Instructions) for the DSL. `Mutable Maps` are used to store variables. The `Scope` class is the implementation of named and anonymous scopes whose instances hold a name, a pointer to the parent scope, their bindings or reference environment and a `Mutable Map` which holds the reference to its child scopes.
A deep down implementation, syntax and semantics is covered in the next section.

---

## Syntax and Semantics

### Common Language Syntax
All of the expression are of type `SetExpression`.
Each expression or instruction need to be evaluated for its working. Which means that you have to write `.eval` at the end of the outer most expression.

For example, the below code declares a variable with name `var1` and assign it a value of `1` (which is the evaluated value of `Value(3)` expression itself). Since, `Value(1)` is also an expression, but you do not need to call `eval` on it separately. You just have to call `eval` on the outer-most expression which is `Assign` in this case.
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
        Assign( "var1", Value(30) )                         // This variable won't be accessible outside of this scope once this scope closes
    )                                                           
).eval  

```