//____________________________________________________________________________
// Static Analysis:
//____________________________________________________________________________

//----------------------------------------------------------------------------
abstract class Type {
  /** Represents the type of integers.
   */
  static final Type INT = new PrimType("int") {
    Width getWidth() { return Width.data; }
  };

  /** Represents the type of booleans.
   */
  static final Type BOOLEAN = new PrimType("boolean") {
    Width getWidth() { return Width.data; }
  };

  /** Test two types for equality.
   */
  abstract boolean equal(Type that);

  /** Test this type for equality with a given PrimType.
   */
  boolean equalPrimType(PrimType that) { return false; }

  /** Test this type for equality with a given ArrayType.
   */
  boolean equalArrayType(ArrayType that) { return false; }

  /** Return the type of elements if this is an array, or else null.
   */
  Type elemType() { return null; }

  /** Return the width for this type of value.
   */
  abstract Width getWidth();
}

//----------------------------------------------------------------------------
/** Represents a primitive type.
 */
abstract class PrimType extends Type {
  private String typename;
  PrimType(String typename) { this.typename = typename; }

  /** Generate a printable name for this type.
   */
  public String toString() {
      return typename;
  }

  boolean equal(Type that) { return that.equalPrimType(this); }

  boolean equalPrimType(PrimType that) {
    return this.typename.equals(that.typename);
  }
}

//----------------------------------------------------------------------------
/** Represents an array type.
 */
class ArrayType extends Type {
  private Type elemType;
  ArrayType(Type elemType) { this.elemType = elemType; }

  boolean equal(Type that) { return that.equalArrayType(this); }
  boolean equalArrayType(ArrayType that) {
    return this.elemType.equal(that.elemType);
  }

  Type elemType()  { return elemType; }

  Width getWidth() { return Width.addr; }

  public String toString() {
      return elemType + "[]";
  }
}

//----------------------------------------------------------------------------
/** Represents a typing environment, mapping identifier
 *  names to corresponding types.
 */
class TypeEnv {
  private String  name;
  private Type    type;
  private TypeEnv next;
  private int     loc;
  TypeEnv(String name, Type type, TypeEnv next) {
    this.name  = name;
    this.type  = type;
    this.next  = next;
    this.loc   = (next==null) ? 1 : 1 + next.loc;
  }

  /** Represents the empty environment that does not bind any
   *  variables.
   */
  static final TypeEnv empty = null;

  /** Search an environment for a specified variable name,
   *  returning null if no such entry is found, or else
   *  returning a pointer to the first matching TypeEnv
   *  object in the list.
   */
  static TypeEnv find(String name, TypeEnv env) {
    while (env!=null && !env.name.equals(name)) {
      env = env.next;
    }
    return env;
  }

  /** Return the value associated with this entry.
   */
  Type getType() {
    return type;
  }

  /** Return the location associated with this entry.
   */
  int getLoc() {
    return loc;
  }
}

//----------------------------------------------------------------------------
/** Represents a function environment, mapping function
 *  names to corresponding function definitions.
 */
class FunctionEnv {

  private String name;

  private Function function;

  private FunctionEnv next;

  FunctionEnv(String name, Function function, FunctionEnv next) {
      this.name      = name;
      this.function  = function;
      this.next      = next;
  }

  /** Represents the empty environment that does not bind any
   *  variables.
   */
  static final FunctionEnv empty = null;

  /** Search an environment for a specified variable name,
   *  returning null if no such entry is found, or else
   *  returning a pointer to the first matching FunctionEnv
   *  object in the list.
   */
  static FunctionEnv find(String name, FunctionEnv env) {
    while (env!=null && !env.name.equals(name)) {
      env = env.next;
    }
    return env;
  }

  /** Return the Function associated with this entry.
   */
  Function getFunction() {
    return function;
  }

  /** Check each of the functions in the specified environment
   *  using the given context.
   */
  static void check(Context ctxt, FunctionEnv fenv) throws StaticError {
    while (fenv!=null) {
      fenv.function.check(ctxt);
      fenv = fenv.next;
    }
  }
}

//----------------------------------------------------------------------------
/** Represents a simple static analysis phase.
 */
class StaticAnalysis {
  void run(Defn[] defns) {
    try {
      new Context().check(defns);
    } catch (StaticError e) {
      System.out.println(e.toString());
      System.exit(1);
    }
  }
}

//----------------------------------------------------------------------------
/** Captures the global context (global variables, functions, etc.) that are
 *  needed for static analysis of an expression, statement, or function.
 */
class Context {
  /** Stores an environment for the global variables in the
   *  current program.
   */
  TypeEnv globals = TypeEnv.empty;

  /** Stores an environment for the functions in the current
   *  program.
   */
  FunctionEnv functions = FunctionEnv.empty;

  /** Records the return type of the current function, or null
   *  if it is a void function.
   */
  Type retType = null;

  /** This flag is set to true if we are checking global declarations
   *  or to false if we are checking code that is local to a specific
   *  function definition.
   */
  boolean isGlobal;

  /** Run the type checker in this context.
   */
  public void check(Defn[] defns) throws StaticError {
    // Reset the environments for this program:
    globals   = TypeEnv.empty;
    functions = FunctionEnv.empty;
    isGlobal  = true;

    // Build global variable and function environments for this program:
    for (int i=0; i<defns.length; i++) {
      defns[i].addToContext(this);
    }

    // Type check each function definition in this program:
    isGlobal = false;
    FunctionEnv.check(this, functions);

    // Check for main function:
    FunctionEnv main = FunctionEnv.find("main", functions);
    if (main==null) {
      throw new StaticError("No definition for main function");
    } else {
      main.getFunction().checkMain();
    }
  }

  /** Look for the type of this variable, starting in the given
   *  type environment for local variables, but then falling back
   *  to the environment for global variables in the context.
   */
  Type findType(String name, TypeEnv env) throws StaticError {
    // Look for a definition of this variable in the local environment:
    TypeEnv te = TypeEnv.find(name, env);

    // If there is no local definition, try the global environment:
    if (te==null) {
      te = TypeEnv.find(name, globals);
    }

    // If we still have not found a definition, then there is an error:
    if (te==null) {
      throw new StaticError("No definition for variable " + name);
    }

    return te.getType();
  }
}

//----------------------------------------------------------------------------
/** Represents an error detected during static analysis.
 */
class StaticError extends Exception {
  StaticError(String msg) { super(msg); }
}

//____________________________________________________________________________
// Expr ::= Var
//       |  Int
//       |  Bool
//       |  Expr + Expr
//       |  Expr - Expr
//       |  Expr < Expr
//       |  Expr == Expr
//       |  Expr && Expr
//       |  Expr || Expr
//       |  Expr[Expr]

abstract class Expr {
  abstract String show();
  abstract Type typeOf(Context ctxt, TypeEnv env) throws StaticError;

  void require(Context ctxt, TypeEnv env, Type required) throws StaticError {
    if (!typeOf(ctxt, env).equal(required)) {
      throw new StaticError("Expression of type " + required
                            + " is required");
    }
  }

  /** Return the depth of this expression as a measure of how complicated
   *  the expression is / how many registers will be needed to evaluate it.
   */
  abstract int getDepth();

  /** Used as a depth value to indicate an expression that has a
   *  potential side effect, and hence requires order of evaluation
   *  to be preserved.  (The same depth value could, in theory, be
   *  produced as the depth of a stunningly complex but side-effect
   *  free expression; oh well, we'll just miss the attempt to
   *  minimize register usage in such (highly unlikely) cases. :-)
   */
  public static final int DEEP = 1000;

  /** Generate assembly language code for this expression that will
   *  evaluate the expression when it is executed and leave the result
   *  in the next free register, as specified by the layout.
   */
  abstract void compileExpr(Assembly a, Frame f);

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is true.
   */
  void branchTrue(Assembly a, Frame f, String lab) {
    compileExpr(a, f);
    a.emit("orl", f.free32(), f.free32());
    a.emit("jnz", lab);
  }

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is false.
   */
  void branchFalse(Assembly a, Frame f, String lab) {
    compileExpr(a, f);
    a.emit("orl", f.free32(), f.free32());
    a.emit("jz", lab);
  }
}

//----------------------------------------------------------------------------
class Var extends Expr {
  private String name;
  Var(String name) { this.name = name; }
  int getDepth()   { return 1; }

  String show() { return name; }

  private TypeEnv te = null; // initialized during static analysis

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return ctxt.findType(name, env);
  }

  void compileExpr(Assembly a, Frame f) {
    f.load(a, name);
  }
}

//----------------------------------------------------------------------------
class Int extends Expr {
  private int num;
  Int(int num)   { this.num = num; }
  int getDepth() { return 1; }

  String show() { return Integer.toString(num); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return Type.INT;
  }

  void compileExpr(Assembly a, Frame f) {
    a.emit("movl", a.immed(num), f.free32());
  }
}

//----------------------------------------------------------------------------
class Bool extends Expr {
  private boolean val;
  Bool(boolean val) { this.val = val; }
  int getDepth()    { return 1; }

  String show() { return Boolean.toString(val); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return Type.BOOLEAN;
  }

  void compileExpr(Assembly a, Frame f) {
    a.emit("movl", a.immed(val ? 1 : 0), f.free32());
  }
}

//----------------------------------------------------------------------------
class Nth extends Expr {
  private Expr arr;
  private Expr idx;
  private int  depth;
  Nth(Expr arr, Expr idx) {
    this.arr   = arr;
    this.idx   = idx;
    this.depth = 1 + Math.max(arr.getDepth(), idx.getDepth());
  }

  int getDepth() { return depth; }

  String show() { return arr.show() + "[" + idx.show() + "]"; }

  private Type elemType = null;  // initialized during static analysis

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    idx.require(ctxt, env, Type.INT);
    Type arrType = arr.typeOf(ctxt, env);
    elemType = arrType.elemType();
    if (elemType==null) {
      throw new StaticError(arr.show() + " has type " + arrType
                          + " and cannot be used as an array");
    }
    return elemType;
  }

  void compileExpr(Assembly a, Frame f) {
    Width  wd = elemType.getWidth();
    String r0 = f.free64();  // Use for address calculations
    String r  = wd.free(f);  // Use for final result
    arr.compileExpr(a, f);
    String r1 = f.spill64(a);// Use for index calculation
    idx.compileExpr(a, f);
    a.emit(wd.mov(), a.indexed(r0, r1, wd.bytes()), r);
    f.unspill(a);
  }
}

//----------------------------------------------------------------------------
/** An abstract base class for expressions that can be used in a
 *  a statement (i.e., for expressions that might have a side-effect).
 */
abstract class StmtExpr extends Expr {
  int getDepth() {
    return DEEP; // A StmtExpr may have side effects
  }

  abstract Type check(Context ctxt, TypeEnv env) throws StaticError;
}

//----------------------------------------------------------------------------
class Assign extends StmtExpr {
  private String lhs;
  private Expr   rhs;
  Assign(String lhs, Expr rhs) {
    this.lhs = lhs; this.rhs = rhs;
  }

  String show() {
    return "(" + lhs + " = " + rhs.show() + ")";
  }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return check(ctxt, env);
  }

  Type check(Context ctxt, TypeEnv env) throws StaticError {
    Type type = ctxt.findType(lhs, env);
    rhs.require(ctxt, env, type);
    return type;
  }

  void compileExpr(Assembly a, Frame f) {
    rhs.compileExpr(a, f);
    f.store(a, lhs);
  }
}

//----------------------------------------------------------------------------
class Call extends StmtExpr {
  /** The name of the function that is being called.
   */
  private String name;

  /** The sequence of expressions provided as arguments.
   */
  private Expr[] args;

  /** Default constructor.
   */
  public Call(String name, Expr[] args) {
    this.name = name;
    this.args = args;
  }

  String show() {
    StringBuilder buf = new StringBuilder(name);
    buf.append('(');
    for (int i=0; i<args.length; i++) {
      if (i>0) {
        buf.append(", ");
      }
      buf.append(args[i].show());
    }
    buf.append(')');
    return buf.toString();
  }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    Type rt = check(ctxt, env);
    if (rt==null) {
      throw new StaticError("function " + name + " does not return a value");
    }
    return rt;
  }

  public Type check(Context ctxt, TypeEnv env) throws StaticError {
    if (ctxt.isGlobal) {
      throw new StaticError("illegal call in global variable initializer");
    }
    FunctionEnv fe = FunctionEnv.find(name, ctxt.functions);
    if (fe==null) {
      throw new StaticError("call to undefined function " + name);
    }
    return fe.getFunction().checkArgs(ctxt, env, args);
  }

  void compileExpr(Assembly a, Frame f) {   // name(args)
    // Create a new call frame, saving registers as necessary:
    CallFrame cf = f.prepareCallFrame(a, args.length);
    cf.dump(a);

    // Evaluate the arguments and add them to the frame:
    for (int i=0; i<args.length; i++) {
      args[i].compileExpr(a, cf);
      cf.saveArg(a);
    }

    // Call the function, and remove stack arguments:
    cf.call(a, name);

    // Set result register and restore saved registers:
    f.removeCallFrame(a);
  }
}

//----------------------------------------------------------------------------
abstract class BinExpr extends Expr {
  protected Expr l, r;

  /** Records the depth of this expression; this value is computed
   *  at the time the constructor is called and then saved here so
   *  that it can be accessed without further computation later on.
   */
  protected int depth;

  BinExpr(Expr l, Expr r) {
    this.l     = l;
    this.r     = r;
    this.depth = 1 + Math.max(l.getDepth(), r.getDepth());
  }

  int getDepth() {
    // Return the depth value that was computed by the constructor
    return depth;
  }

  String show() { return "(" + l.show() + " " + op() + " " + r.show() + ")"; }
  abstract String op();

  /** Generate code to evalute both of the expressions l and r,
   *  changing the order of evaluation if possible/beneficial to
   *  reduce the number of registers that are required.  The return
   *  boolean indicates the order in which the two expressions have
   *  been evaluated and stored in registers.  A true result indicates
   *  that reg(free) contains the value of l and reg(free+1) contains
   *  the value of r.  A false result indicates that the order has
   *  been reversed.  In both cases, reg(free+1) will need to be
   *  unspilled once the value in that register has been used.
   */
  void compileBin(Assembly a, Frame f, String op, boolean commutative) {
    String r0 = f.free32();
    String r1;
    if (l.getDepth()>=r.getDepth() || r.getDepth()>=DEEP) {
      l.compileExpr(a, f);
      r1 = f.spill32(a);
      r.compileExpr(a, f);
    } else {
      r.compileExpr(a, f);
      r1 = f.spill32(a);
      l.compileExpr(a, f);
      if (!commutative) {
          a.emit("xchgl", r1, r0);
      }
    }
    a.emit(op, r1, r0);
    f.unspill(a);
  }
}

//----------------------------------------------------------------------------
abstract class ArithBinExpr extends BinExpr {
  ArithBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.INT);
    r.require(ctxt, env, Type.INT);
    return Type.INT;
  }
}

//----------------------------------------------------------------------------
class Plus extends ArithBinExpr {
  Plus(Expr l, Expr r) { super(l, r); }
  String op() { return "+"; }

  void compileExpr(Assembly a, Frame f) {
    compileBin(a, f, "addl", true);
  }
}

//----------------------------------------------------------------------------
class Minus extends ArithBinExpr {
  Minus(Expr l, Expr r) { super(l, r); }
  String op() { return "-"; }

  void compileExpr(Assembly a, Frame f) {
    compileBin(a, f, "subl", false);
  }
}

//----------------------------------------------------------------------------
class Mult extends ArithBinExpr {
  Mult(Expr l, Expr r) { super(l, r); }
  String op() { return "*"; }

  void compileExpr(Assembly a, Frame f) {
    compileBin(a, f, "imull", true);
  }
}

//----------------------------------------------------------------------------
abstract class RelBinExpr extends BinExpr {
  RelBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.INT);
    r.require(ctxt, env, Type.INT);
    return Type.BOOLEAN;
  }

  /** Generate code for a comparision operation.  The resulting
   *  code evaluates both l and r arguments, and then does
   *  a comparision, setting the flags ready for the appropriate
   *  conditional jump.  The free+1 register is both spilled and
   *  unspilled in this code, which means that the caller does
   *  not need to handle spilling.
   */
  void compileCond(Assembly a, Frame f) {
    String r0 = f.free32();
    String r1;
    if (l.getDepth()>r.getDepth() || r.getDepth()>=DEEP) {
      l.compileExpr(a, f);
      r1 = f.spill32(a);
      r.compileExpr(a, f);
      a.emit("cmpl", r1, r0);
    } else {
      r.compileExpr(a, f);
      r1 = f.spill32(a);
      l.compileExpr(a, f);
      a.emit("cmpl", r0, r1);
    }
    f.unspill(a);
  }

  /** Generate code for a comparison that computes either 1 (for
   *  true) or 0 (for false) in the specified free register.  The
   *  given "test" instruction is used to trigger a branch in the
   *  true case.
   */
  void compileCondValue(Assembly a, Frame f, String test) {
    String lab1 = a.newLabel();  // jump here if true
    String lab2 = a.newLabel();  // jump here when done
    compileCond(a, f);           // compare the two arguments
    a.emit(test, lab1);          // jump if condition is true
    a.emit("movl", a.immed(0), f.free32());
    a.emit("jmp",  lab2);
    a.emitLabel(lab1);
    a.emit("movl", a.immed(1), f.free32());
    a.emitLabel(lab2);           // continue with value in free
  }
}

//----------------------------------------------------------------------------
class LT extends RelBinExpr {
  LT(Expr l, Expr r) { super(l, r); }
  String op() { return "<"; }

  /** Generate assembly language code for this expression that will
   *  evaluate the expression when it is executed and leave the result
   *  in the next free register, as specified by the frame.
   */
  void compileExpr(Assembly a, Frame f) {
    // Assume integer comparison:
    compileCondValue(a, f, "jl");
  }

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is true.
   */
  void branchTrue(Assembly a, Frame f, String lab) {
    compileCond(a, f);
    a.emit("jl", lab);
  }

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is false.
   */
  void branchFalse(Assembly a, Frame f, String lab) {
    compileCond(a, f);
    a.emit("jnl", lab);
  }
}

//----------------------------------------------------------------------------
class EqEq extends RelBinExpr {
  EqEq(Expr l, Expr r) { super(l, r); }
  String op() { return "=="; }

  /** Generate assembly language code for this expression that will
   *  evaluate the expression when it is executed and leave the result
   *  in the next free register, as specified by the frame.
   */
  void compileExpr(Assembly a, Frame f) {
    // Assume integer comparison:
    compileCondValue(a, f, "je");
  }

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is true.
   */
  void branchTrue(Assembly a, Frame f, String lab) {
    compileCond(a, f);
    a.emit("jz", lab);
  }

  /** Generate code that will evaluate this (boolean-valued) expression
   *  and jump to the specified label if the result is false.
   */
  void branchFalse(Assembly a, Frame f, String lab) {
    compileCond(a, f);
    a.emit("jnz", lab);
  }
}

//----------------------------------------------------------------------------
abstract class LogicBinExpr extends BinExpr {
  LogicBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.BOOLEAN);
    r.require(ctxt, env, Type.BOOLEAN);
    return Type.BOOLEAN;
  }
}

//----------------------------------------------------------------------------
class LAnd extends LogicBinExpr {
  LAnd(Expr l, Expr r) { super(l, r); }

  String op() { return "&&"; }

  void compileExpr(Assembly a, Frame f) {
    System.err.println("LAnd compile() method NOT IMPLEMENTED");
    System.exit(1);
  }
}

//----------------------------------------------------------------------------
class LOr extends LogicBinExpr {
  LOr(Expr l, Expr r) { super(l, r); }

  String op() { return "||"; }

  void compileExpr(Assembly a, Frame f) {
    System.err.println("LOr compile() method NOT IMPLEMENTED");
    System.exit(1);
  }
}

//____________________________________________________________________________
// Stmt  ::= Seq Stmt Stmt
//        |  ExprStmt
//        |  While Expr Stmt
//        |  If Expr Stmt Stmt
//        |  Print Expr
//        |  Return Expr
//        |  VarDecl VarIntro[]
//        |  DoWhile Stmt Expr
//        |  Break
//        |  Continue
//        |  Switch Expr Case[]
//____________________________________________________________________________

/** Base class for abstract syntax trees that represent statements.
 */
abstract class Stmt {
  abstract void print(int ind);

  static void indent(int ind) {
    for (int i=0; i<ind; i++) {
      System.out.print(" ");
    }
  }

  abstract TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError;

  /** Return true if this statement can be guaranteed to return, ensuring
   *  that any immediately following statement will not be executed.
   */
  boolean guaranteedToReturn() {
    // Most statements will continue on the next statement after execution,
    // suggesting that a return result of false is a reasonable default here.
    return false;
  }

  /** Generate code for executing this statement.
   *  Returns true if there is a chance that execution may
   *  continue with the next statement.
   */
  abstract boolean compile(Assembly a, Frame f);

  /** Generate code for executing the statement as a new
   *  block so that local variables are discarded at the
   *  end of the block.
   */
  boolean compileBlock(Assembly a, Frame f) {
    LocEnv  origEnv = f.getEnv();
    boolean b       = compile(a,f);
    f.resetTo(a, origEnv);
    return b;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for an expression used as a statement.
 */
class ExprStmt extends Stmt {
  private StmtExpr exp;
  ExprStmt(StmtExpr exp) { this.exp = exp; }

  void print(int ind) {
    indent(ind);
    System.out.print(exp.show());
    System.out.println(";");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    exp.check(ctxt, env);
    return env;
  }

  boolean compile(Assembly a, Frame f) {
      exp.compileExpr(a, f);
      return true;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a sequence of two statements.
 */
class Seq extends Stmt {
  private Stmt l, r;
  Seq(Stmt l, Stmt r) { this.l = l; this.r = r; }

  void print(int ind) {
    l.print(ind);
    r.print(ind);
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    return r.check(ctxt, canContinue, canBreak,
           l.check(ctxt, canContinue, canBreak, env));
  }

  boolean guaranteedToReturn() {
    return l.guaranteedToReturn() || r.guaranteedToReturn();
  }

  boolean compile(Assembly a, Frame f) {
    return l.compile(a, f) && r.compile(a, f);
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a while loop.
 */
class While extends Stmt {
  private Expr test;
  private Stmt body;
  While(Expr test, Stmt body) {
    this.test = test; this.body = body;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("while (" + test.show() + ") {");
    body.print(ind+2);
    indent(ind);
    System.out.println("}");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    test.require(ctxt, env, Type.BOOLEAN);
    body.check(ctxt, true, true, env);
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    String lab1 = a.newLabel();
    String lab2 = a.newLabel();
    a.emit("jmp", lab2);
    a.emitLabel(lab1);
    body.compileBlock(a, f);
    a.emitLabel(lab2);
    test.branchTrue(a, f, lab1);
    return true;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for an if then else statement.
 */
class If extends Stmt {
  private Expr test;
  private Stmt ifTrue, ifFalse;
  If(Expr test, Stmt ifTrue, Stmt ifFalse) {
    this.test = test; this.ifTrue = ifTrue; this.ifFalse = ifFalse;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("if (" + test.show() + ") {");
    ifTrue.print(ind+2);
    if (ifFalse!=null) {
      indent(ind);
      System.out.println("} else {");
      ifFalse.print(ind+2);
    }
    indent(ind);
    System.out.println("}");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    test.require(ctxt, env, Type.BOOLEAN);
    ifTrue.check(ctxt, canContinue, canBreak, env);
    if (ifFalse!=null) {
      ifFalse.check(ctxt, canContinue, canBreak, env);
    }
    return env;
  }

  public boolean guaranteedToReturn() {
    // An if statement is only guaranteed to return if it has two branches
    // (a true and a false branch), both of which are guaranteed to return:
    return ifTrue.guaranteedToReturn() &&
           ifFalse!=null &&
           ifFalse.guaranteedToReturn();
  }

  boolean compile(Assembly a, Frame f) {
    String lab1 = a.newLabel();
    test.branchFalse(a, f, lab1);
    boolean c = ifTrue.compileBlock(a, f);
    if (ifFalse==null) {
      a.emitLabel(lab1);
      return true;
    } if (!c) {
      a.emitLabel(lab1);
      c |= ifFalse.compileBlock(a, f);
    } else {
      String lab2 = a.newLabel();
      a.emit("jmp", lab2);
      a.emitLabel(lab1);
      c |= ifFalse.compileBlock(a, f);
      a.emitLabel(lab2);
    }
    return c;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a print statement.
 */
class Print extends Stmt {
  private Expr exp;
  Print(Expr exp) { this.exp = exp; }

  void print(int ind) {
    indent(ind);
    System.out.println("print " + exp.show() + ";");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    exp.require(ctxt, env, Type.INT);
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    // Print is implemented by calling a runtime library
    // function called "print" that takes one argument:
    CallFrame cf = f.prepareCallFrame(a, 1);
    exp.compileExpr(a, cf);
    cf.saveArg(a);
    cf.call(a, "print");
    f.removeCallFrame(a);
    return true;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a return statement.
 */
class Return extends Stmt {
  private Expr exp;
  Return(Expr exp) { this.exp = exp; }

  void print(int ind) {
    indent(ind);
    if (exp==null) {
      System.out.println("return;");
    } else {
      System.out.println("return " + exp.show() + ";");
    }
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    if (ctxt.retType==null) {  // appears in void function
      if (exp!=null) {
        throw new StaticError("void function should not return a value");
      }
    } else {                   // return in non-void function
      if (exp==null) {
        throw new StaticError("function must return a value of type "
                              + ctxt.retType);
      } else {
        exp.require(ctxt, env, ctxt.retType);
      }
    }
    return env;
  }

  boolean guaranteedToReturn() {
    // Unsurprisingly, return statements are guaranteed to return!
    return true;
  }

  boolean compile(Assembly a, Frame f) {
      if (exp!=null) {
         exp.compileExpr(a, f);
      }
      a.emitEpilogue();
      return false;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a (local) variable declaration.
 */
class VarDecl extends Stmt {
  private Type       type;
  private VarIntro[] vars;
  VarDecl(Type type, VarIntro[] vars) { this.type = type; this.vars = vars; }

  void print(int ind) {
    VarIntro.print(ind, type, vars);
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    for (int i=0; i<vars.length; i++) {
      env = vars[i].check(ctxt, type, env);
    }
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    for (int i=0; i<vars.length; i++) {
       vars[i].compile(a, type, f);
    }
    f.dump(a);
    return true;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a variable introduction (no initializer).
 */
class VarIntro {
  protected String name;
  VarIntro(String name) { this.name = name; }

  static void print(int ind, Type type, VarIntro[] vars) {
    Stmt.indent(ind);
    System.out.print(type.toString());
    for (int i=0; i<vars.length; i++) {
      System.out.print((i==0)? " " : ", ");
      vars[i].print();
    }
    System.out.println(";");
  }

  void print() {
    System.out.print(name);
  }

  protected TypeEnv te;

  TypeEnv check(Context ctxt, Type type, TypeEnv env) throws StaticError {
    return te = new TypeEnv(name, type, env);
  }

  /** Extend the global environment with an entry for the variable
   *  that is introduced here, using the given type.
   */
  void addToContext(Context ctxt, Type type) throws StaticError {
    throw new StaticError("Global variable " + name + " is not initialized");
  }

  /** Declare storage for global variables.
   */
  LocEnv declareGlobals(Assembly a, Width wd, LocEnv env) {
    a.emitLabel(a.name(name));
    a.emit(wd.storage(), "0");
    return new GlobalEnv(name, wd, env);
  }

  void compile(Assembly a, Type type, Frame f) {
      // If no explicit initializer is given, initialize with zero:
      f.allocLocal(a, name, type, a.immed(0));
  }

  /** Generate code to initialize local variables introduced
   *  in this definition.
   */
  void emitInitGlobals(Assembly a, Frame f) {
    // Shouldn't occur if static analysis is doing it's job!
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a variable introduction with an initializer.
 */
class InitVarIntro extends VarIntro {
  private Expr expr;
  InitVarIntro(String name, Expr expr) { super(name); this.expr = expr; }

  void print() {
    super.print();
    System.out.print(" = ");
    System.out.print(expr.show());
  }

  TypeEnv check(Context ctxt, Type type, TypeEnv env) throws StaticError {
    expr.require(ctxt, env, type);
    return super.check(ctxt, type, env);
  }

  void addToContext(Context ctxt, Type type) throws StaticError {
    if (TypeEnv.find(name, ctxt.globals)!=null) {
      throw new StaticError("multiple global definitions for " + name);
    }
    expr.require(ctxt, TypeEnv.empty, type);
    ctxt.globals = new TypeEnv(name, type, ctxt.globals);
  }

  void compile(Assembly a, Type type, Frame f) {
    // Evaluate the initializer expression and save the result:
    expr.compileExpr(a, f);
    f.allocLocal(a, name, type, f.free64());
  }

  /** Generate code to initialize local variables introduced
   *  in this definition.
   */
  void emitInitGlobals(Assembly a, Frame f) {
    expr.compileExpr(a, f);
    f.store(a, name);
  }
}

//____________________________________________________________________________
// Further Implementation Required!
//____________________________________________________________________________

//----------------------------------------------------------------------------
/** Abstract syntax for a for loop.
 */
class For extends Stmt {
  private StmtExpr init; // could be null
  private Expr     test; // could be null
  private StmtExpr step; // could be null
  private Stmt     body;
  For(StmtExpr init, Expr test, StmtExpr step, Stmt body) {
    this.init = init; this.test = test; this.step = step; this.body = body;
  }

  void print(int ind) {
    indent(ind);
    System.out.print("for (");
    if (init!=null) {
      System.out.print(init.show());
    }
    System.out.print("; ");
    if (test!=null) {
      System.out.print(test.show());
    }
    System.out.print("; ");
    if (step!=null) {
      System.out.print(step.show());
    }
    System.out.println(") {");
    body.print(ind+2);
    indent(ind);
    System.out.println("}");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    if (init!=null) {
      init.check(ctxt, env);
    }
    if (test!=null) {
      test.require(ctxt, env, Type.BOOLEAN);
    }
    if (step!=null) {
      step.check(ctxt, env);
    }
    body.check(ctxt, true, true, env);
    return env;
  }

  boolean compile(Assembly a, Frame f) {

    String lab1 = a.newLabel();
    String lab2 = a.newLabel();

    init.compileExpr(a,f);
    a.emit("jmp", lab2);
    a.emitLabel(lab1);
    body.compileBlock(a,f);
    step.compileExpr(a,f);
    a.emitLabel(lab2);
    
    test.branchTrue(a, f, lab1);

    
    return true;
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for do while loops.
 */
class DoWhile extends Stmt {
  private Stmt body;
  private Expr test;
  DoWhile(Stmt body, Expr test) {
    this.body = body; this.test = test;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("do {");
    body.print(ind+2);
    indent(ind);
    System.out.println("} while (" + test.show() + ");");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    body.check(ctxt, true, true, env);
    test.require(ctxt, env, Type.BOOLEAN);
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    System.err.println("DoWhile compile() method NOT IMPLEMENTED");
    System.exit(1);
    return false; // not reached
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for break statements.
 */
class Break extends Stmt {
  Break() { }

  void print(int ind) {
    indent(ind);
    System.out.println("break;");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    if (!canBreak) {
      throw new StaticError("illegal use of break statement");
    }
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    System.err.println("Break compile() method NOT IMPLEMENTED");
    System.exit(1);
    return false; // not reached
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for continue statements.
 */
class Continue extends Stmt {
  Continue() { }

  void print(int ind) {
    indent(ind);
    System.out.println("continue;");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    if (!canContinue) {
      throw new StaticError("illegal use of continue statement");
    }
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    System.err.println("Continue compile() method NOT IMPLEMENTED");
    System.exit(1);
    return false; // not reached
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for switch statements.
 */
class Switch extends Stmt {
  private Expr   test;
  private Case[] cases;
  Switch(Expr test, Case[] cases) {
    this.test = test; this.cases = cases;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("switch (" + test.show() + ") {");
    for (int i=0; i<cases.length; i++) {
      cases[i].print(ind+2);
    }
    indent(ind);
    System.out.println("}");
  }

  TypeEnv check(Context ctxt, boolean canContinue, boolean canBreak, TypeEnv env)
   throws StaticError {
    test.require(ctxt, env, Type.INT);
    for (int i=0; i<cases.length; i++) {
      cases[i].check(ctxt, canContinue, env);
      for (int j=i+1; j<cases.length; j++) {
        cases[i].distinctFrom(cases[j]);
      }
    }
    return env;
  }

  boolean compile(Assembly a, Frame f) {
    System.err.println("Switch compile() method NOT IMPLEMENTED");
    System.exit(1);
    return false; // not reached
  }
}

//----------------------------------------------------------------------------
/** Abstract base class for the cases in a switch statement.
 */
abstract class Case {
  protected Stmt body;  // Note: body may be empty (i.e., null)
  Case(Stmt body) { this.body = body; }

  abstract void print(int ind);

  void check(Context ctxt, boolean canContinue, TypeEnv env)
   throws StaticError {
    if (body!=null) {
      body.check(ctxt, canContinue, true, env);
    }
  }

  // Methods for checking that cases are disjoint: a single
  // switch statement should not two default cases or two
  // numeric cases with the same integer value.
  abstract void distinctFrom(Case that) throws StaticError;
  abstract void notCaseFor(int num)     throws StaticError;
  abstract void notDefault()            throws StaticError;
}

//----------------------------------------------------------------------------
/** Abstract syntax for a numbered case in a switch statement.
 */
class NumCase extends Case {
  private int  num;
  NumCase(int num, Stmt body) { super(body); this.num = num; }
  
  void print(int ind) {
    Stmt.indent(ind);
    System.out.println("case " + num + ":");
    if (body!=null) {
      body.print(ind+2);
    }
  }

  void distinctFrom(Case that) throws StaticError {
    that.notCaseFor(num);
  }

  void notCaseFor(int num) throws StaticError {
    if (this.num==num) {
      throw new StaticError("switch statement contains two cases for " + num);
    }
  }

  void notDefault() throws StaticError {
    /* this is not a default case, so no action is required. */
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for a default case in a switch statement.
 */
class DefaultCase extends Case {
  DefaultCase(Stmt body) { super(body); }

  void print(int ind) {
    Stmt.indent(ind);
    System.out.println("default:");
    if (body!=null) {
      body.print(ind+2);
    }
  }

  void distinctFrom(Case that) throws StaticError {
    that.notDefault();
  }

  void notCaseFor(int num) throws StaticError {
    /* this is not a number case, so no action is required. */
  }

  void notDefault() throws StaticError {
    throw new StaticError("switch statement contains two default cases");
  }
}

//____________________________________________________________________________
// Programs:

//----------------------------------------------------------------------------
/** Abstract syntax for definitions (either global variables or functions.
 */
abstract class Defn {

  static void print(int ind, Defn[] defns) {
    for (int i=0; i<defns.length; i++) {
      defns[i].print(ind);
      System.out.println();
    }
  }

  abstract void print(int ind);

  /** Extend the environments in the given program with entries from
   *  this definition.
   */
  abstract void addToContext(Context ctxt) throws StaticError;

  /** Generate assembly code for given set of top-level definitions.
   */
  static void compile(String name, Defn[] defns) {
    LocEnv   globals = null;
    Assembly a       = Assembly.assembleToFile(name);

    a.emit();
    a.emit(".data");
    for (int i=0; i<defns.length; i++) {
      globals = defns[i].declareGlobals(a, globals);
    }

    a.emit();
    a.emit(".text");

    // Emit a definition for the initGlobals function:
    Function.emitInitGlobals(a, defns, globals);

    for (int i=0; i<defns.length; i++) {
      defns[i].compileFunction(a, globals);
    }

    a.close();
  }

  /** Declare storage for global variables.
   */
  abstract LocEnv declareGlobals(Assembly a, LocEnv env);

  /** Generate compiled code for a function.
   */
  abstract void compileFunction(Assembly a, LocEnv globals);

  /** Generate code to initialize local variables introduced
   *  in this definition.
   */
  abstract void emitInitGlobals(Assembly a, Frame f);
}

//----------------------------------------------------------------------------
/** Abstract syntax for global variable definitions.
 */
class Globals extends Defn {

  /** The type of the variable(s) being defined.
   */
  private Type type;

  /** The names and initial values of the variables.
   */
  private VarIntro[] vars;

  /** Default constructor.
   */
  Globals(Type type, VarIntro[] vars) {
    this.type = type;
    this.vars = vars;
  }

  void print(int ind) {
    VarIntro.print(ind, type, vars);
  }

  /** Extend the environments in the given context with entries from
   *  this definition.
   */
  void addToContext(Context ctxt) throws StaticError {
    for (int i=0; i<vars.length; i++) {
      vars[i].addToContext(ctxt, type);
    }
  }

  /** Declare storage for global variables.
   */
  LocEnv declareGlobals(Assembly a, LocEnv env) {
    // Initialize global variables:
    Width wd = type.getWidth();
    for (int i=0; i<vars.length; i++) {
      env = vars[i].declareGlobals(a, wd, env);
    }
    return env;
  }

  /** Generate compiled code for a function.
   */
  void compileFunction(Assembly a, LocEnv globals) {
    // Nothing to do here, but this method is required
    // because Globals extends Defn.
  }

  /** Generate code to initialize local variables introduced
   *  in this definition.
   */
  void emitInitGlobals(Assembly a, Frame f) {
    for (int i=0; i<vars.length; i++) {
      vars[i].emitInitGlobals(a, f);
    }
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax for function definitions.
 */
class Function extends Defn {

  /** The return type of this function (or null for
   *  a procedure/void function).
   */
  private Type retType;

  /** The name of this function.
   */
  private String name;

  /** The formal parameters for this function.
   */
  private Formal[] formals;

  /** The body of this function.
   */
  private Stmt body;

  /** Default constructor.
   */
  Function(Type retType, String name, Formal[] formals, Stmt body) {
    this.retType = retType;
    this.name    = name;
    this.formals = formals;
    this.body    = body;
  }

  void print(int ind) {
    Stmt.indent(ind);
    System.out.print((retType==null) ? "void" : retType.toString());
    System.out.print(" " + name + "(");
    for (int i=0; i<formals.length; i++) {
      if (i>0) {
        System.out.print(", ");
      }
      formals[i].print();
    }
    System.out.println(") {");
    body.print(ind+2);
    Stmt.indent(ind);
    System.out.println("}");
  }

  /** Extend the environments in the given context with entries from
   *  this definition.
   */
  void addToContext(Context ctxt) throws StaticError {
    // Check that there is no previously defined function with
    // the same name:
    if (FunctionEnv.find(name, ctxt.functions)!=null) {
      throw new StaticError("Multiple functions called " + name);
    }

    // Extend the function environment with a new entry for this
    // definition:
    ctxt.functions = new FunctionEnv(name, this, ctxt.functions);
  }

  /** Check that this is a valid function definition.
   */
  void check(Context ctxt) throws StaticError {
    // Check for duplicate names in the formal parameter list:
    if (Formal.containsRepeats(formals)) {
      throw new StaticError("Repeated formal parameter names for function "
                            + name);
    }

    // Build an environment for this function's local variables:
    TypeEnv locals = TypeEnv.empty;
    for (int i=0; i<formals.length; i++) {
      locals = formals[i].extend(locals);
    }

    // Type check the body of this function:
    ctxt.retType = this.retType;
    body.check(ctxt, false, false, locals);

    // Check that non-void functions are guaranteed to return:
    if (this.retType!=null && !body.guaranteedToReturn()) {
      throw new StaticError("Body of function " + name + " may not return");
    }
  }

  /** Check to see if this function would be a valid main function.
   *  We assume that we have already checked that the function's
   *  name is "main", so it just remains to check that the return
   *  type is void, and that it has no formal parameters.
   */
  void checkMain() throws StaticError {
    if (retType!=null) {
      throw new StaticError("main function does not have void return type");
    }
    if (formals.length!=0) {
      throw new StaticError("main function should not have any parameters");
    }
  }

  /** Check that the given list of arguments is valid for a
   *  call to this function, and then return the resulting
   *  argument type.
   */
  Type checkArgs(Context ctxt, TypeEnv locals, Expr[] args) throws StaticError {
    if (args.length != formals.length) {
      throw new StaticError("Call to function " + name
                          + " has the wrong number of arguments");
    }
    for (int i=0; i<formals.length; i++) {
      args[i].require(ctxt, locals, formals[i].getType());
    }
    return retType;
  }

  /** Declare storage for global variables.
   */
  LocEnv declareGlobals(Assembly a, LocEnv env) {
    // No global variables declared here! But this method
    // is required because Function extends Defn.
    return env;
  }

  /** Generate compiled code for a function.
   */
  void compileFunction(Assembly a, LocEnv globals) {
    a.emit(".globl", a.name(name));
    a.emitLabel(a.name(name));
    a.emitPrologue();
    Frame f = new FunctionFrame(formals, globals);
    f.dump(a);
    if (body.compile(a, f)) {
      a.emitEpilogue();
    }
    a.emit();
  }

  /** Generate code for the initGlobals function.
   */
  static void emitInitGlobals(Assembly a, Defn[] defns, LocEnv globals) {
    String   name    = "initGlobals"; // Name this function
    Formal[] formals = new Formal[0]; // No formal parameters

    a.emit(".globl", a.name(name));
    a.emitLabel(a.name(name));
    a.emitPrologue();
    Frame f = new FunctionFrame(formals, globals);

    for (int i=0; i<defns.length; i++) { // Initialize each global
      defns[i].emitInitGlobals(a, f);
    }

    a.emitEpilogue();
    a.emit();
  }

  /** Generate code to initialize local variables introduced
   *  in this definition.
   */
  void emitInitGlobals(Assembly a, Frame f) {
    // No global variables introduced in a function definition!
  }
}

//----------------------------------------------------------------------------
/** Abstract syntax representation for a formal parameter.
 */
class Formal {

  /** The type of the parameter.
   */
  private Type type;

  /** The name of the parameter.
   */
  private String name;

  /** Default constructor.
   */
  Formal(Type type, String name) {
    this.type = type;
    this.name = name;
  }

  void print() {
    System.out.print(type + " " + name);
  }

  /** Extend the given environment with an entry for this
   *  formal parameter.
   */
  public TypeEnv extend(TypeEnv locals) {
    return new TypeEnv(name, type, locals);
  }

  /** Extend the given local environment with an entry for
   *  storing this formal parameter in a register.
   */
  LocEnv extend(Reg reg, LocEnv env) {
    return new RegEnv(name, type.getWidth(), env, reg);
  }

  /** Extend the given local environment with an entry for
   *  storing this formal parameter in a frame slot.
   */
  LocEnv extend(int offset, LocEnv env) {
    return new FrameEnv(name, type.getWidth(), env, offset);
  }

  /** Return the type associated with this formal parameter.
   */
  Type getType() {
    return type;
  }

  /** Check to see if this array of formal parameter includes
   *  two definitions for the same variable name.
   */
  static boolean containsRepeats(Formal[] formals) {
    for (int i=0; i<formals.length; i++) {
      for (int j=i+1; j<formals.length; j++) {
        if (formals[i].name.equals(formals[j].name)) {
          return true;
        }
      }
    }
    return false;
  }

  /** Return the name associated with this formal parameter.
   */
  String getName() { return name; }
}

//____________________________________________________________________________
