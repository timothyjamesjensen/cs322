//----------------------------------------------------------------------------
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.HashMap;
import llvm.Block;
import llvm.Code;
import llvm.Rhs;
import llvm.Value;
import llvm.Location;
import llvm.Local;
import llvm.Global;

//----------------------------------------------------------------------------
/** Represents a continuation that takes a value representing the
 *  result of a previous calculation and returns a code sequence
 *  that uses that value to complete a computation.
 */
interface ValCont {        // Value -> Code continuation
  public Code with(final Value v);
}

//____________________________________________________________________________
// Types:
//____________________________________________________________________________

abstract class Type {
  /** Represents the type of integers.
   */
  static final Type INT = new PrimType("int", llvm.Type.i32);

  /** Represents the type of booleans.
   */
  static final Type BOOLEAN = new PrimType("boolean", llvm.Type.i1);

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

  /** Determine the LLVM type corresponding to this source language type.
   */
  abstract llvm.Type toLLVM();
}

//----------------------------------------------------------------------------
/** Represents a primitive type.
 */
class PrimType extends Type {
  private String    typename;
  private llvm.Type ty;
  PrimType(String typename, llvm.Type ty) {
    this.typename = typename;
    this.ty       = ty;
  }

  /** Generate a printable name for this type.
   */
  public String toString() {
      return typename;
  }

  boolean equal(Type that) { return that.equalPrimType(this); }

  boolean equalPrimType(PrimType that) {
    return this.typename.equals(that.typename);
  }

  /** Determine the LLVM type corresponding to this source language type.
   */
  llvm.Type toLLVM() {
    return ty;
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

  public String toString() {
      return elemType + "[]";
  }

  /** Determine the LLVM type corresponding to this source language type.
   */
  llvm.Type toLLVM() {
    return elemType().toLLVM().ptr();
  }
}

//____________________________________________________________________________
// Expressions:
//____________________________________________________________________________

abstract class Expr {
  abstract String show();
  abstract Type typeOf(Context ctxt, TypeEnv env) throws StaticError;

  void require(Context ctxt, TypeEnv env, Type required) throws StaticError {
    if (!typeOf(ctxt, env).equal(required)) {
      throw new StaticError("Expression of type " + required
                            + " is required");
    }
  }

  /** Test to determine whether this expression is an LValue.
   */
  LValue asLValue() {
    return null; // default: most expressions are not LValues.
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  abstract Code compile(final llvm.Function fn, final ValCont k);

  /** Generate LLVM code that will evaluate this expression
   *  and then branch to one of the specified LLVM blocks
   *  depending on whether the expression is true or false.
   *  Of course, this only makes sense for expressions that
   *  produce boolean values; for everything else, the
   *  default implementation below will be sufficient (and
   *  should never actually be used if we ensure that our
   *  programs are type checked before we attempt to compile
   *  them).
   */
  Code compileCond(final llvm.Function fn,
                   final llvm.Block t,
                   final llvm.Block f) {
    System.err.println("compileCond() method NOT IMPLEMENTED");
    System.exit(1);
    return null; // not reached
  }
}

//----------------------------------------------------------------------------
class Int extends Expr {
  private int num;
  Int(int num)   { this.num = num; }

  String show() { return Integer.toString(num); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return Type.INT;
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return k.with(new llvm.IntVal(num));
  }
}

//----------------------------------------------------------------------------
class Bool extends Expr {
  private boolean val;
  Bool(boolean val) { this.val = val; }

  String show() { return Boolean.toString(val); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return Type.BOOLEAN;
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return k.with(new llvm.BoolVal(val));
  }
}

//----------------------------------------------------------------------------
abstract class LValue extends Expr {
  /** Test to determine whether this expression is an Lvalue.
   */
  LValue asLValue() {
    return this;
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return this.compileLoc(fn, loc -> {
      llvm.Reg rg = fn.reg(loc.getType().ptsTo());
      return new llvm.Op(rg, new llvm.Load(loc), k.with(rg));
    });
  }

  /** Generate LLVM code that will generate a location for
   *  this LValue.
   */
  abstract Code compileLoc(final llvm.Function fn, final ValCont k);
}

//----------------------------------------------------------------------------
class Var extends LValue {
  private String name;
  Var(String name) { this.name = name; }

  String show() { return name; }

  /** Records the LLVM location for the variable referenced here.
   */
  private Location loc;

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    TypeEnv te = ctxt.findTypeEnv(name, env);
    loc        = te.getLoc();
    return te.getType();
  }

  /** Generate LLVM code that will generate a location for
   *  this LValue.
   */
  Code compileLoc(final llvm.Function fn, final ValCont k) {
    return k.with(loc);
  }
}

//----------------------------------------------------------------------------
class Nth extends LValue {
  private Expr arr;
  private Expr idx;
  Nth(Expr arr, Expr idx) {
    this.arr = arr;
    this.idx = idx;
  }

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

  /** Generate LLVM code that will generate a location for
   *  this LValue.
   */
  Code compileLoc(final llvm.Function fn, final ValCont k) {
    return arr.compile(fn, a ->
           // "a" is a value that holds the address of the array
           idx.compile(fn, i -> {
             // "i" is a value that holds the value of the array index


             llvm.Reg tf = fn.reg(llvm.Type.i1);
             // make a value and register for adding 1 to the passed index
             llvm.Value iv = new llvm.IntVal(1);
             llvm.Reg r = fn.reg(llvm.Type.i32);


             // Make a new register, "rg", to hold the address of the
             // requested array element:
             llvm.Reg  rg = fn.reg(a.getType());



             return new llvm.Op(tf, new llvm.Gte(llvm.Type.i32, iv, iv), new llvm.Cond(tf, 
             // Use the llvm getelementptr instruction to calculate the
             // address of the "i"th element of the array starting at
             // address "a", save the result in register "rg", and then
             // pass "rg" as the input to the continuation "k":
	            new llvm.Block("L1", new llvm.Op(r, new llvm.Add(llvm.Type.i32, i, iv),
                      new llvm.Op(rg, new llvm.Getelementptr(a, r),
                      k.with(rg)))),
             new llvm.Block("L0", new llvm.Unreachable())));
           }));
  }
}

//----------------------------------------------------------------------------
class NewArray extends Expr {
  private Type type;
  private Expr size;
  NewArray(Type type, Expr size) {
    this.type = type;
    this.size = size;
  }

  String show() { return "new " + type + "[" + size.show() + "]"; }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    size.require(ctxt, env, Type.INT);
    return new ArrayType(type);
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return size.compile(fn, n -> {
      // "ty" is the type i8*, which corresponds to the
      // return type of the allocArray function.
      llvm.Type ty = llvm.Type.i8.ptr();

      // "call" is llvm operation that we will use to invoke
      // the allocArray function (the two parameters are
      // the length of the array, n, and the size of each
      // individual array element: you are only expected
      // to make this code work with int[] arrays, and
      // ints are 4 bytes, which is why the size is set
      // to 4 here.
      llvm.Rhs call
         = new llvm.Call(ty,
                         "allocArray",
                         new Value[] { n, new llvm.IntVal(4) });

      // "rg" is a new register that we will use to hold
      // the return result produced by allocArray.
      llvm.Reg  rg = fn.reg(ty);

      // The next step is to cast the pointer that is returned by
      // allocArray to match the type of the array.  We will capture
      // this type in a variable, "at".  Because we are limiting our
      // attention to int[] arrays, It would be sufficient to use
      // i32* here.  However, we will use a more general formulation
      // here that could be used with other arrays:
      llvm.Type at = new ArrayType(type).toLLVM();

      // "cast" is the llvm operation that we will use to cast the
      // the result of the call (in "rg") into a value of type "at".
      llvm.Rhs cast
        = new llvm.Bitcast(rg, at);

      // "na" is the register that we will use to capture
      // the result of the cast, and hence to hold the value of the
      // new array.
      llvm.Reg  na = fn.reg(at);

      // Finally, we can generate a sequence of code that calls the
      // allocArray function with the appropriate arguments, casts
      // the result to an appropriate type, and then passes the new
      // array to the the continuation, "k":
      return new llvm.Op(rg, call,
             new llvm.Op(na, cast,
             k.with(na)));
    });
  }
}

//----------------------------------------------------------------------------
class Length extends Expr {
  private Expr arr;
  Length(Expr arr) {
    this.arr = arr;
  }

  String show() { return "length(" + arr.show() + ")"; }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    if (arr.typeOf(ctxt, env).elemType() == null) {
      throw new StaticError(this
                    + " is not valid because argument is not an array");
    }
    return Type.INT;
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return this.compileLoc(fn, loc -> {
      llvm.Reg rg = fn.reg(loc.getType().ptsTo());
      return new llvm.Op(rg, new llvm.Load(loc), k.with(rg));
    });
  }
  
  Code compileLoc(final llvm.Function fn, final ValCont k) {
    // TODO: this is not a correct implementation: it always
    // behaves as if the array length is zero ...
    //return k.with(new llvm.IntVal(0));
    return arr.compile(fn, a -> {
      llvm.Reg rg = fn.reg(a.getType());
      return new llvm.Op(rg, new llvm.Getelementptr(a, new llvm.IntVal(0)), k.with(rg));
    });
  }
}

//----------------------------------------------------------------------------
/** An abstract base class for expressions that can be used in a
 *  a statement (i.e., for expressions that might have a side-effect).
 */
abstract class StmtExpr extends Expr {
  abstract Type check(Context ctxt, TypeEnv env) throws StaticError;

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    return check(ctxt, env);
  }

  /** Generate code to evaluate this expression and discard any result
   *  that it produces.
   */
  Code compileDiscard(final llvm.Function fn, final Code andThen) {
    return compile(fn, v -> andThen);
  }
}

//----------------------------------------------------------------------------
class Assign extends StmtExpr {
  private Expr lhs;
  private Expr rhs;
  Assign(Expr lhs, Expr rhs) {
    this.lhs = lhs; this.rhs = rhs;
  }

  String show() {
    return "(" + lhs.show() + " = " + rhs.show() + ")";
  }

  /** Stores an LValue corresponding to the left hand side; set during
   *  static analysis.
   */
  private LValue lv;

  Type check(Context ctxt, TypeEnv env) throws StaticError {
    if ((lv=lhs.asLValue())==null) {
      throw new StaticError("Value " + lhs.show() +
                            " used where a variable was expected");
    }
    Type type = lv.typeOf(ctxt, env);
    rhs.require(ctxt, env, type);
    return type;
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return rhs.compile(fn,   v   ->
           lv.compileLoc(fn, loc ->
           new llvm.Store(v, loc, k.with(v))));
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

  /** Records the Function object associated with this call.
   */
  private Function f;

  /** Default constructor.
   */
  Call(String name, Expr[] args) {
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

  Type check(Context ctxt, TypeEnv env) throws StaticError {
    if (ctxt.current==null) {
      throw new StaticError("illegal call in global variable initializer");
    }
    FunctionEnv fe = FunctionEnv.find(name, ctxt.functions);
    if (fe==null) {
      throw new StaticError("call to undefined function " + name);
    }

    // Record the function object associated with this name:
    f = fe.getFunction();

    // And then check the rest of the call:
    return f.checkArgs(ctxt, env, args);
  }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return f.compile(fn, args, k);
  }

  /** Generate code to evaluate this expression and discard any
   *  result that it produces.
   */
  Code compileDiscard(final llvm.Function fn, final Code andThen) {
    return f.compile(fn, args, andThen);
  }
}

//----------------------------------------------------------------------------
abstract class BinExpr extends Expr {
  protected Expr l, r;

  BinExpr(Expr l, Expr r) {
    this.l = l;
    this.r = r;
  }

  String show() { return "(" + l.show() + " " + op() + " " + r.show() + ")"; }
  abstract String op();

  /** Captures a general pattern for compiling a binary operator by
   *  evaluating each of the two arguments and then applying the
   *  specified LLVM primitive.  Note that we include two operators,
   *  one for use when an integer result is required and for use when
   *  a floating point result is expected.
   */
  Code binary(final llvm.Function fn,
              final llvm.BinOp.Maker op,
              final ValCont k) {
    return l.compile(fn, lv ->
           r.compile(fn, rv -> {
             llvm.BinOp rhs = op.make(lv.getType(), lv, rv);
             llvm.Reg   lhs = fn.reg(rhs.resultType());
             return new llvm.Op(lhs, rhs, k.with(lhs));
           }));
  }
}

//----------------------------------------------------------------------------
abstract class ArithBinExpr extends BinExpr {
  ArithBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.INT);
    r.require(ctxt, env, Type.INT);
    return /*type =*/ Type.INT;
  }
}

//----------------------------------------------------------------------------
class Plus extends ArithBinExpr {
  Plus(Expr l, Expr r) { super(l, r); }
  String op() { return "+"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Add(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
class Minus extends ArithBinExpr {
  Minus(Expr l, Expr r) { super(l, r); }
  String op() { return "-"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Sub(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
class Mult extends ArithBinExpr {
  Mult(Expr l, Expr r) { super(l, r); }
  String op() { return "*"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Mul(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
class Div extends ArithBinExpr {
  Div(Expr l, Expr r) { super(l, r); }
  String op() { return "/"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Div(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
abstract class RelBinExpr extends BinExpr {
  RelBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.INT);
    r.require(ctxt, env, Type.INT);
    return /*type =*/ Type.BOOLEAN;
  }
}

//----------------------------------------------------------------------------
class LT extends RelBinExpr {
  LT(Expr l, Expr r) { super(l, r); }
  String op() { return "<"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Lt(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
class EqEq extends RelBinExpr {
  EqEq(Expr l, Expr r) { super(l, r); }
  String op() { return "=="; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return binary(fn, (ty, l, r) -> new llvm.Eql(ty, l, r), k);
  }
}

//----------------------------------------------------------------------------
abstract class LogicBinExpr extends BinExpr {
  LogicBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(Context ctxt, TypeEnv env) throws StaticError {
    l.require(ctxt, env, Type.BOOLEAN);
    r.require(ctxt, env, Type.BOOLEAN);
    return /*type =*/ Type.BOOLEAN;
  }
}

//----------------------------------------------------------------------------
class LAnd extends LogicBinExpr {
  LAnd(Expr l, Expr r) { super(l, r); }

  String op() { return "&&"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return l.compile(fn, lv -> {
        final Block first = fn.block();
        final Block join  = fn.block();
        Code  evalRight   = r.compile(fn, rv -> {
            Block    second = fn.block(new llvm.Goto(join));
            Rhs      merge  = new llvm.Phi(first, lv, second, rv);
            llvm.Reg rg     = fn.reg(llvm.Type.i1);
            join.set(new llvm.Op(rg, merge, k.with(rg)));
            return new llvm.Goto(second);
          });
        first.set(new llvm.Cond(lv, fn.block(evalRight), join));
        return new llvm.Goto(first);
      });
  }
}

//----------------------------------------------------------------------------
class LOr extends LogicBinExpr {
  LOr(Expr l, Expr r) { super(l, r); }

  String op() { return "||"; }

  /** Generate LLVM code that will evaluate this expression,
   *  and then pass the resulting value on to the following
   *  code, represented by the continuation argument.
   */
  Code compile(final llvm.Function fn, final ValCont k) {
    return l.compile(fn, lv -> {
        final Block first = fn.block();
        final Block join  = fn.block();
        Code  evalRight   = r.compile(fn, rv -> {
            Block    second = fn.block(new llvm.Goto(join));
            Rhs      merge  = new llvm.Phi(first, lv, second, rv);
            llvm.Reg rg     = fn.reg(llvm.Type.i1);
            join.set(new llvm.Op(rg, merge, k.with(rg)));
            return new llvm.Goto(second);
          });
        first.set(new llvm.Cond(lv, join, fn.block(evalRight)));
        return new llvm.Goto(first);
      });
  }
}

//____________________________________________________________________________
// Statements:
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  abstract Code compile(final llvm.Function fn, final Code andThen);
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    return exp.compileDiscard(fn, andThen);
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    return l.compile(fn, r.compile(fn, andThen));
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    final Block head = fn.block();
    final Code  loop = new llvm.Goto(head);
    head.set(test.compile(fn, v ->
        new llvm.Cond(v, fn.block(body.compile(fn, loop)),
                         fn.block(andThen))));
    return loop;
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

  boolean guaranteedToReturn() {
    // An if statement is only guaranteed to return if it has two branches
    // (a true and a false branch), both of which are guaranteed to return:
    return ifTrue.guaranteedToReturn() &&
           ifFalse!=null &&
           ifFalse.guaranteedToReturn();
  }

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    return test.compile(fn, v -> {
      if (andThen==null) {
        return new llvm.Cond(v, fn.block(ifTrue.compile(fn, null)),
                                fn.block(ifFalse.compile(fn, null)));
      } else {
        Block join = fn.block(andThen);
        Code  jmp  = new llvm.Goto(join);
        Block  f   = (ifFalse==null)
                      ? join
                      : fn.block(ifFalse.compile(fn, jmp));
        return new llvm.Cond(v, fn.block(ifTrue.compile(fn, jmp)), f);
      }
    });
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    // Print is implemented by calling a runtime library
    // function called "print" that takes one argument:
    return exp.compile(fn, v -> new llvm.CallVoid("print", new Value[] {v}, andThen));
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    return (exp==null)
         ? new llvm.RetVoid()
         : exp.compile(fn, v -> new llvm.Ret(v));
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

  /** Generate LLVM code that will execute this statement and
   *  then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final Code andThen) {
    llvm.Type ty   = type.toLLVM();
    Code      code = andThen;
    for (int i=vars.length-1; i>=0; i--) {
     code = vars[i].compile(fn, ty, code);
    }
    return code;
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

  /** Records the LLVM location for the variable introduced here.
   */
  protected Location loc;

  TypeEnv check(Context ctxt, Type type, TypeEnv env) throws StaticError {
    loc = ctxt.current.addLocal(name, type);
    return te = new TypeEnv(name, type, loc, env);
  }

  /** Extend the global environment with an entry for the variable
   *  that is introduced here, using the given type.
   */
  void addToContext(Context ctxt, Type type) throws StaticError {
    throw new StaticError("Global variable " + name + " is not initialized");
  }

  /** Generate LLVM code that will initialize the variable that is
   *  being introduced and then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final llvm.Type ty,
               final Code andThen) {
      // Do nothing if there is no initializer
      return andThen;
  }

  /** Generate code on the specified output channel to provide
   *  definitions for global variables introduced here.
   */
  void declareGlobals(PrintWriter out) {
    llvm.Type ty = loc.getType().ptsTo();
    // The following line should be moved out of this file to
    // eliminate a dependency on the concrete syntax of LLVM.
    out.println(loc.getName() + " = global " + ty
                              + " " + ty.defaultValue()
                              + ", align " + ty.getAlign());
  }

  /** Generate code that will initialize any global variables
   *  introduced here and then continue with andThen.
   */
  Code initGlobals(llvm.Function fn, final Code andThen) {
    /* Variable introductions without initializer expressions
     * should be prevented by static analysis, so no action
     * is required here.
     */
    return andThen;
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
    loc = ctxt.addGlobal(type, name);
    ctxt.globals = new TypeEnv(name, type, loc, ctxt.globals);
  }

  /** Generate LLVM code that will initialize the variable that is
   *  being introduced and then continue with the follow on code.
   */
  Code compile(final llvm.Function fn, final llvm.Type ty,
               final Code andThen) {
    return expr.compile(fn, v -> new llvm.Store(v, loc, andThen));
  }

  /** Generate code that will initialize any global variables
   *  introduced here and then continue with andThen.
   */
  Code initGlobals(llvm.Function fn, final Code andThen) {
    return expr.compile(fn, v -> new llvm.Store(v, loc, andThen));
  }
}

//____________________________________________________________________________
// Definitions:
//____________________________________________________________________________

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

  /** Generate an LLVM version of the given program
   *  in text form using the specified output channel.
   */
  static void compile(PrintWriter out, Defn[] program) {
    // Declare external runtime library functions:
    // (Note: This code should be moved out of this file to
    // eliminate a dependency on the concrete syntax of LLVM.)
    out.println("; declare external runtime library functions");
    out.println("declare void @Xprint(i32)");
    out.println("declare i8*  @XallocArray(i32, i32)");
    out.println();

    // Generate declarations for global variables:
    out.println("; declare global variables");
    for (int i=0; i<program.length; i++) {
      program[i].declareGlobals(out);
    }
    out.println();

    // Generate code for function definitions:
    out.println("; function definitions");

    // Generate code for initGlobals function:
    initGlobals(program).print(out);

    // Process regular function definitions:
    for (int i=0; i<program.length; i++) {
      program[i].generateCode(out);
    }
  }

  /** Generate code for an initGlobals function that will
   *  initialize all of the global variables in the given
   *  program.
   */
  static llvm.Function initGlobals(Defn[] program) {
    String        name = "initGlobals";
    llvm.Formal[] fms  = new llvm.Formal[0];
    llvm.Function init = new llvm.Function(null, name, fms);
    Code          code = new llvm.RetVoid();
    for (int i=program.length-1; i>=0; i--) {
      code = program[i].initGlobals(init, code);
    }
    init.block("entry", code);
    return init;
  }

  /** Generate code for function definitions.
   */
  void generateCode(PrintWriter out) {
    // Nothing to do for non-function definitions!
  }

  /** Generate code on the specified output channel to provide
   *  definitions for global variables introduced here.
   */
  void declareGlobals(PrintWriter out) {
    /* By default, no action is required. */
  }

  /** Generate code that will initialize any global variables
   *  introduced here and then continue with andThen.
   */
  Code initGlobals(llvm.Function fn, final Code andThen) {
    // No additional code is required for function definitions
    return andThen;
  }
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

  /** Generate code on the specified output channel to provide
   *  definitions for global variables introduced here.
   */
  void declareGlobals(PrintWriter out) {
    for (int i=0; i<vars.length; i++) {
      vars[i].declareGlobals(out);
    }
  }

  /** Generate code that will initialize any global variables
   *  introduced here and then continue with andThen.
   */
  Code initGlobals(llvm.Function fn, final Code andThen) {
    Code code = andThen;
    for (int i=vars.length-1; i>=0; i--) {
      code = vars[i].initGlobals(fn, code);
    }
    return code;
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

  /** Records the set of local variables defined in this function.
   */
  private HashMap<String,Local> localVars;

  /** Return a local "address" for the named variable with the given
   *  type that is unique within this function.
   */
  Local addLocal(String name, Type type) {
    String unique = name;
    if (localVars.containsKey(unique)) {
      int i = 0;
      do {
        unique = name + i++;
      } while (localVars.containsKey(unique));
    }
    Local loc = new Local(type.toLLVM().ptr(), unique);
    localVars.put(unique, loc);
    return loc;
  }

  /** Default constructor.
   */
  Function(Type retType, String name, Formal[] formals, Stmt body) {
    this.retType   = retType;
    this.name      = name;
    this.formals   = formals;
    this.body      = body;
    this.localVars = new HashMap<String,Local>();
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
    // Make a note of the current function:
    ctxt.current = this;

    // Check for duplicate names in the formal parameter list:
    if (Formal.containsRepeats(formals)) {
      throw new StaticError("Repeated formal parameter names for function "
                            + name);
    }

    // Build an environment for this function's local variables:
    TypeEnv locals = TypeEnv.empty;
    for (int i=0; i<formals.length; i++) {
      locals = formals[i].extend(this, locals);
    }

    // Type check the body of this function:
    ctxt.retType = this.retType;
    body.check(ctxt, false, false, locals);

    // Check that non-void functions are guaranteed to return:
    if (!body.guaranteedToReturn()) {
      if (this.retType==null) {
        // Add an explicit return for a void function that might
        // otherwise fail to return:
        body = new Seq(body, new Return(null));
      } else {
        throw new StaticError("Body of function " + name + " may not return");
      }
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

  /** Generate LLVM code for a function call that appears in an expression
   *  where the final result will be passed on for possible use in the
   *  following computation.
   */
  Code compile(final llvm.Function fn, Expr[] actuals, final ValCont k) {
    llvm.Type ty   = retType.toLLVM();
    Value[]   args = new Value[actuals.length];
    Rhs       call = new llvm.Call(ty, name, args);
    llvm.Reg  rg   = fn.reg(ty);
    return addArgs(fn, actuals, args, new llvm.Op(rg, call, k.with(rg)));
  }

  /** Generate LLVM code for a function call whose result, if any, will not
   *  be used in the following computation.  This requires two cases to
   *  allow for the possibility of calling a void function or calling a
   *  function whose return value will be discarded.
   */
  Code compile(final llvm.Function fn, Expr[] actuals, final Code andThen) {
    Value[] args = new Value[actuals.length];
    Code    code;
    if (retType==null) {
      code = new llvm.CallVoid(name, args, andThen);
    } else {
      llvm.Type ty   = retType.toLLVM();
      Rhs       call = new llvm.Call(ty, name, args);
      code           = new llvm.Op(fn.reg(ty), call, andThen);
    }
    return addArgs(fn, actuals, args, code);
  }

  /** Generate code to evaluate the arguments for a function call prior to
   *  subsequent code, including the actual function call.
   */
  Code addArgs(llvm.Function fn, Expr[] actuals, final Value[] args, Code code) {
    for (int i=actuals.length-1; i>=0; i--) {
      final int  j = i;
      final Code c = code;
      code = actuals[i].compile(fn, v -> { args[j] = v; return c; });
    }
    return code;
  }

  /** Generate code for function definitions.
   */
  void generateCode(PrintWriter out) {
    llvm.Formal[] fms  = new llvm.Formal[formals.length];
    llvm.Type     rt   = (retType==null) ? null : retType.toLLVM();
    llvm.Function fn   = new llvm.Function(rt, name, fms);
    Code          code = body.compile(fn, null);

    // Add code to save parameters in stack frame:
    for (int i=formals.length-1; i>=0; i--) {
      fms[i] = formals[i].toLLVM();
      code   = new llvm.Store(fms[i].getParam(), fms[i], code);
    }

    // Add stack frame slots for all local variables, including parameters:
    for (Local loc : localVars.values()) {
      code = new llvm.Op(loc, new llvm.Alloca(loc.getType().ptsTo()), code);
    }
    fn.block("entry", code);
    fn.print(out);
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
  TypeEnv extend(Function f, TypeEnv locals) {
    loc = f.addLocal(name, type);
    return new TypeEnv(name, type, loc, locals);
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

  /** Records the LLVM location for the variable referenced here.
   */
  private Location loc;

  /** Generate an LLVM formal parameter reference corresponding to this
   *  source language formal parameter.
   */
  llvm.Formal toLLVM() {
    return new llvm.Formal(type.toLLVM().ptr(), name);
  }
}

//____________________________________________________________________________
// Static Analysis:
//____________________________________________________________________________

//----------------------------------------------------------------------------
/** Represents a typing environment, mapping identifier
 *  names to corresponding types.
 */
class TypeEnv {
  private String   name;
  private Type     type;
  private Location loc;
  private TypeEnv  next;
  TypeEnv(String name, Type type, Location loc, TypeEnv next) {
    this.name  = name;
    this.type  = type;
    this.loc   = loc;
    this.next  = next;
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
  Location getLoc() {
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

  /** Add a global variable with the specified name and type.
   */
  Global addGlobal(Type type, String name) {
    Global loc = new Global(type.toLLVM().ptr(), name);
    globals    = new TypeEnv(name, type, loc, globals);
    return loc;
  }

  /** Stores an environment for the functions in the current
   *  program.
   */
  FunctionEnv functions = FunctionEnv.empty;

  /** Records the return type of the current function, or null
   *  if it is a void function.
   */
  Type retType = null;

  /** Holds a pointer to the function whose body we are currently
   *  checking, or null if we are checking global declarations.
   */
  Function current = null;

  /** Run the type checker in this context.
   */
  void check(Defn[] defns) throws StaticError {
    // Reset the environments for this program:
    globals   = TypeEnv.empty;
    functions = FunctionEnv.empty;
    current   = null;

    // Build global variable and function environments for this program:
    for (int i=0; i<defns.length; i++) {
      defns[i].addToContext(this);
    }

    // Type check each function definition in this program:
    FunctionEnv.check(this, functions);

    // Check for main function:
    FunctionEnv main = FunctionEnv.find("main", functions);
    if (main==null) {
      throw new StaticError("No definition for main function");
    } else {
      main.getFunction().checkMain();
    }
  }

  /** Look for the type environment of this variable, starting in the
   *  given type environment for local variables, but then falling back
   *  to the environment for global variables in the context.
   */
  TypeEnv findTypeEnv(String name, TypeEnv env) throws StaticError {
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

    return te;
  }
}

//----------------------------------------------------------------------------
/** Represents an error detected during static analysis.
 */
class StaticError extends Exception {
  StaticError(String msg) { super(msg); }
}

//____________________________________________________________________________
// Main compiler entry point:
//____________________________________________________________________________

//____________________________________________________________________________
/** A top-level program for compiling a source program in to LLVM.
 */
class StevieLLVM {
  public static final String name = "demo";

  public static void main(String[] args) {
    // Read and parse a source program from standard input
    new Parser(System.in);
    Defn[] program = null;
    try {
      program = Parser.Top();

      // Display the source program:
      System.out.println("Complete program is:");
      Defn.print(4, program);

      // Run static analysis on the source program:
      new StaticAnalysis().run(program);
      System.out.println("Passes static analysis!");

      // Generate corresponding LLVM code:
      String      filename = name + ".ll";
      PrintWriter out      = new PrintWriter(new FileWriter(filename));
      Defn.compile(out, program);
      out.close();
      System.out.println("Generated LLVM code in " + filename);
    } catch (ParseException e) {
      System.out.println("Syntax Error");
      System.exit(1);
    } catch (Exception e) {
      System.out.println(e.toString());
      System.exit(1);
    }
  }
}

//____________________________________________________________________________
