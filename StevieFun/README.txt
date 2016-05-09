This folder contains a version of the Stevie compiler that handles input
programs that are written as a sequence of global variable and function
definitions.  This differs from the syntax used by the previous version
where the input was just a sequence of statements (effectively the body
of a single function).  For this reason, we use the suffix ".stv" for
the source files for this version of the compiler to distinguish them
from the ".stevie" files used by the previous version.  You can read
details of the grammar from the supplied Parser.jj file, or you can
look at the supplied .stv files for some concrete examples.

This version of the compiler generates code in x86_64 assembly language
and can be used on either LINUX or MACOSX platforms.  To select the
desired platform, find the following line in the Assembly class at the
top of the Asm.java source file:

    static final int platform = PLEASE_CHECK_THE_README_FILE;

and replace the string PLEASE_CHECK_THE_README_FILE with either LINUX or
MACOSX as appropriate.  After that, the program may be compiled using
the supplied Makefile by typing the command:

  make

(You can also use "make clean" to remove the compiled files.)

To use the resulting compiler, use a command of the following form:

  java StevieFun < sourcefile.stv

where sourcefile.stv contains the source code that you want to
compile.  This will produce an assembly code output in the file "demo.s"
and an executable file called "demo" that you can run using the command:

  ./demo

In comparison to the previous StevieAsm compiler, this version of Stevie:

- Includes syntax for defining functions and global variables.

- Includes syntax for function call and assignment expressions, both of
  which can either be used as subexpressions of a larger expression, or
  as a statement.

- Supports void functions (and hence allows a "return;" statement without
  a return expression).

From an implementation perspective, the primary ways in which StevieFun
differs from StevieAsm are as follows:

- StevieFun uses values of a class called "Context" to capture the full
  set of function and global variables in the complete program.  The
  Context for the current program is passed as a parameter to every
  static analysis function so that it can be used, for example, to look
  up references to global variables or to defined functions.

- A new FunctionEnv type provides an environment mapping names to the
  associated Function objects.  (You probably guessed as much from the
  name!)

- All of the code for generating Target instructions has been removed:
  the original Target language is not suited for an implementation of
  functions.  (Implementing support for functions in the target language
  is probably technically feasible, but it would be very difficult!  At
  this stage in the process, we need to move on to more realistic
  targets!)

- There is a new abstract class called StmtExpr that is used as the
  superclass for all forms of expression that can be used as a
  statement/have the potential for a side effect.  The subclasses of
  StmtExpr in the current implementation are Assign and Call for the
  assignment and function call operations, respectively.  StmtExpr
  object support an additional static analysis method called check()
  that can be used to validate expressions that are used as statements.
  In particular, it allows a call to a void function without raising an
  error.  A call to a void function that is used as an expression will
  result in a static error because all expressions are expected to
  produce a value.  There is a new ExprStmt class that allows a StmtExpr
  to be used as a statement.  (Note the important but subtle difference
  in naming between StmtExpr and ExprStmt!)

- The Call class, representing a function call, is new, and the Assign
  class, previously a subclass of Stmt, is now a subclass of StmtExpr.

- There is a new method, guaranteedToReturn(), that can be used to
  determine if the body of a function is guaranteed to execute a return.
  It is a static error for a source program to define a non-void
  function whose body is not guaranteed to return.

- The VarIntro and InitVarIntro classes includes extra methods that can
  be used to generate code for global variable definitions.

- There is a new class called Defn with subclasses Globals and Function
  that correspond to global variable definitions and function
  definitions, respectively.

StevieFun is not currently supported on Windows (enthusiastic Windows
users may be able to address this issue, but I do not have a machine of
my own that I can use to develop or test Windows support).  For obvious
reasons, StevieFun is also not supported on machines that do not use the
x86_64 instruction set, which include Sparc-based machines (like some of
the college's Unix servers), or machines using a MIPS or ARM CPU.

