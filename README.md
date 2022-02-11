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
3. Switch to the `master` branch if the default branch is somthing else.
4. Open IntelliJ and go to `File > New > Project from existing Source`, or `File > New > Project from version control`. For the second option, you have directly provide git repo link and no need to clone the git repo separately.
5. Make sure you have Java JDK and scala installed on your system. Java JDK between versions 8 and 17 are required to run this project.
6. We are using Scala version `3.1.1`. Please go to its [docs](https://docs.scala-lang.org/scala3/) for better understanding. You can download Scala 3 from [here](https://www.scala-lang.org/download/).
7. The build tool we are using for this is call [SBT(Simple Build Toolkit)](https://www.scala-sbt.org/download.html). Check out this download [link](https://www.scala-sbt.org/download.html) for SBT. 
8. Alternatively, you can also install SBT for your project only from IntelliJ's external libraries.
9. SBT version we are using in this project is `1.6.2`.
10. Please also install `Scala` plugin in IntelliJ itelf.
11. For testing purposes, we are using [Scalatest](https://www.scalatest.org/).
12. If you go to the `build.sbt` in the folder structure, you can see that Scalatest version `3.2.11` is added as a dependency.
13. The `build.sbt` is responsible for building your project.
14. In IntelliJ, go to `Build > Build Project` to build the project.
15. Once build is finished, you are ready to run the project.
16. `src/test/scala/SetTheoryDSLTest.scala` is the file where all the test cases are written. This is the file you need to run to test the project. 
17. You can add your own test cases in it to test the project better.
18. The file `src/main/scala/SetTheoryDSL.scala` is the main implementation of Set Theory DSL.
19. Please explore and have fun.

---

### Implementation
The implementation of Set Theory DSL is done with the help of `Enumeration` or [enums](https://docs.scala-lang.org/scala3/reference/enums/enums.html) construct.
