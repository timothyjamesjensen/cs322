//____________________________________________________________________________
// Static Analysis:

abstract class Type {
  /** Represents the type of integers.
   */
  static final Type INT = new PrimType("int");

  /** Represents the type of booleans.
   */
  static final Type BOOLEAN = new PrimType("boolean");

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
}

/** Represents a primitive type.
 */
class PrimType extends Type {
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
}

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

/** Represents a simple static analysis phase.
 */
class StaticAnalysis {
  void run(Stmt s) {
    TypeEnv env = TypeEnv.empty;
    try {
      s.check(env, false, false);
    } catch (StaticError e) {
      System.out.println(e.toString());
      System.exit(1);
    }
  }
}

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
  abstract Type typeOf(TypeEnv env) throws StaticError;

  void require(TypeEnv env, Type required) throws StaticError {
    if (!typeOf(env).equal(required)) {
      throw new StaticError("Expression of type " + required
                            + " is required");
    }
  }

  abstract Code compileTo(Tmp reg, Code next);
}

class Var extends Expr {
  private String name;
  Var(String name) { this.name = name; }

  String show() { return name; }

  private TypeEnv te = null; // initialized during static analysis

  Type typeOf(TypeEnv env) throws StaticError {
    if ((te = TypeEnv.find(name, env))==null) {
      throw new StaticError("No definition for variable " + name);
    }
    return te.getType();
  }

  Code compileTo(Tmp reg, Code next) {
    return new Load(reg, te.getLoc(), next);
  }
}

class Int extends Expr {
  private int num;
  Int(int num)   { this.num = num; }

  String show() { return Integer.toString(num); }

  Type typeOf(TypeEnv env) throws StaticError {
    return Type.INT;
  }

  Code compileTo(Tmp reg, Code next) {
    return new Immed(reg, num, next);
  }
}

class Bool extends Expr {
  private boolean val;
  Bool(boolean val) { this.val = val; }

  String show() { return Boolean.toString(val); }

  Type typeOf(TypeEnv env) throws StaticError {
    return Type.BOOLEAN;
  }

  Code compileTo(Tmp reg, Code next) {
    return new Immed(reg, (val ? 1 : 0), next);
  }
}

class Nth extends Expr {
  private Expr arr;
  private Expr idx;
  Nth(Expr arr, Expr idx) {
    this.arr   = arr;
    this.idx   = idx;
  }

  String show() { return arr.show() + "[" + idx.show() + "]"; }

  private Type elemType = null;  // initialized during static analysis

  Type typeOf(TypeEnv env) throws StaticError {
    idx.require(env, Type.INT);
    Type arrType = arr.typeOf(env);
    elemType = arrType.elemType();
    if (elemType==null) {
      throw new StaticError(arr.show() + " has type " + arrType
                          + " and cannot be used as an array");
    }
    return elemType;
  }

  Code compileTo(Tmp reg, Code next) {
    System.out.println("Array indexing not implemented");
    System.exit(1);
    return next; // not reached
  }
}

abstract class BinExpr extends Expr {
  protected Expr l, r;

  BinExpr(Expr l, Expr r) {
    this.l     = l;
    this.r     = r;
  }

  String show() { return "(" + l.show() + " " + op() + " " + r.show() + ")"; }
  abstract String op();
}

abstract class ArithBinExpr extends BinExpr {
  ArithBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(TypeEnv env) throws StaticError {
    l.require(env, Type.INT);
    r.require(env, Type.INT);
    return Type.INT;
  }
}

class Plus extends ArithBinExpr {
  Plus(Expr l, Expr r) { super(l, r); }
  String op() { return "+"; }

  Code compileTo(Tmp reg, Code next) {
    Tmp tmp = new Tmp();
    return l.compileTo(tmp,
           r.compileTo(reg,
           new Op(reg, tmp, '+', reg, next)));
  }
}

class Minus extends ArithBinExpr {
  Minus(Expr l, Expr r) { super(l, r); }
  String op() { return "-"; }

  Code compileTo(Tmp reg, Code next) {
    Tmp tmp = new Tmp();
    return l.compileTo(tmp,
           r.compileTo(reg,
           new Op(reg, tmp, '-', reg, next)));
  }
}

class Mult extends ArithBinExpr {
  Mult(Expr l, Expr r) { super(l, r); }
  String op() { return "*"; }

  Code compileTo(Tmp reg, Code next) {
    Tmp tmp = new Tmp();
    return l.compileTo(tmp,
           r.compileTo(reg,
           new Op(reg, tmp, '*', reg, next)));
  }
}

abstract class RelBinExpr extends BinExpr {
  RelBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(TypeEnv env) throws StaticError {
    l.require(env, Type.INT);
    r.require(env, Type.INT);
    return Type.BOOLEAN;
  }
}

class LT extends RelBinExpr {
  LT(Expr l, Expr r) { super(l, r); }
  String op() { return "<"; }

  Code compileTo(Tmp reg, Code next) {
    Tmp tmp = new Tmp();
    return l.compileTo(tmp,
           r.compileTo(reg,
           new Op(reg, tmp, '<', reg, next)));
  }
}

class EqEq extends RelBinExpr {
  EqEq(Expr l, Expr r) { super(l, r); }
  String op() { return "=="; }

  Code compileTo(Tmp reg, Code next) {
    Tmp tmp = new Tmp();
    return l.compileTo(tmp,
           r.compileTo(reg,
           new Op(reg, tmp, '=', reg, next)));
  }
}

abstract class LogicBinExpr extends BinExpr {
  LogicBinExpr(Expr l, Expr r) { super(l, r); }

  Type typeOf(TypeEnv env) throws StaticError {
    l.require(env, Type.BOOLEAN);
    r.require(env, Type.BOOLEAN);
    return Type.BOOLEAN;
  }
}

class LAnd extends LogicBinExpr {
  LAnd(Expr l, Expr r) { super(l, r); }

  String op() { return "&&"; }

  Code compileTo(Tmp reg, Code next) {
    System.err.println("LAnd compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

class LOr extends LogicBinExpr {
  LOr(Expr l, Expr r) { super(l, r); }

  String op() { return "||"; }

  Code compileTo(Tmp reg, Code next) {
    System.err.println("LOr compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

//____________________________________________________________________________
// Stmt  ::= Seq Stmt Stmt
//        |  Var := Expr
//        |  While Expr Stmt
//        |  If Expr Stmt Stmt
//        |  Print Expr
//        |  Return Expr
//        |  VarDecl VarIntro[]
//        |  DoWhile Stmt Expr
//        |  Break
//        |  Continue
//        |  Switch Expr Case[]

abstract class Stmt {
  abstract void print(int ind);

  static void indent(int ind) {
    for (int i=0; i<ind; i++) {
      System.out.print(" ");
    }
  }

  abstract TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError;

  abstract Code compile(Program prog, Code next);
}

class Seq extends Stmt {
  private Stmt l, r;
  Seq(Stmt l, Stmt r) { this.l = l; this.r = r; }

  void print(int ind) {
    l.print(ind);
    r.print(ind);
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    return r.check(l.check(env, canContinue, canBreak),
                   canContinue, canBreak);
  }

  Code compile(Program prog, Code next) {
    return l.compile(prog, r.compile(prog, next));
  }
}

class Assign extends Stmt {
  private String lhs;
  private Expr   rhs;
  Assign(String lhs, Expr rhs) {
    this.lhs = lhs; this.rhs = rhs;
  }

  void print(int ind) {
    indent(ind);
    System.out.println(lhs + " = " + rhs.show() + ";");
  }

  private TypeEnv te = null; // initialized during static analysis

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    if ((te = TypeEnv.find(lhs, env))==null) {
      throw new StaticError("No definition for left hand side " + lhs);
    }
    rhs.require(env, te.getType());
    return env;
  }

  Code compile(Program prog, Code next) {
    Tmp tmp = new Tmp();
    return rhs.compileTo(tmp, new Store(te.getLoc(), tmp, next));
  }
}

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

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    test.require(env, Type.BOOLEAN);
    body.check(env, true, true);
    return env;
  }

  Code compile(Program prog, Code next) {
    Block head = prog.block();
    Code  loop = new Goto(head);
    Tmp   tmp  = new Tmp();
    head.set(test.compileTo(tmp,
             new Cond(tmp,
                      prog.block(body.compile(prog, loop)),
                      prog.block(next))));
    return loop;
  }
}

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

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    test.require(env, Type.BOOLEAN);
    ifTrue.check(env, canContinue, canBreak);
    if (ifFalse!=null) {
      ifFalse.check(env, canContinue, canBreak);
    }
    return env;
  }

  Code compile(Program prog, Code next) {
    Tmp   tmp = new Tmp();
    Block n   = prog.block(next);
    Goto  got = new Goto(n);
    Block t   = prog.block(ifTrue.compile(prog, got));
    Block f   = (ifFalse==null)
                 ? n : prog.block(ifFalse.compile(prog, got));
    return test.compileTo(tmp, new Cond(tmp, t, f));
  }
}

class Print extends Stmt {
  private Expr exp;
  Print(Expr exp) { this.exp = exp; }

  void print(int ind) {
    indent(ind);
    System.out.println("print " + exp.show() + ";");
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    exp.require(env, Type.INT);
    return env;
  }

  Code compile(Program prog, Code next) {
    Tmp tmp = new Tmp();
    return exp.compileTo(tmp, new PCode(tmp, next));
  }
}

class Return extends Stmt {
  private Expr exp;
  Return(Expr exp) { this.exp = exp; }

  void print(int ind) {
    indent(ind);
    System.out.println("return " + exp.show() + ";");
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    exp.require(env, Type.INT);
    return env;
  }

  Code compile(Program prog, Code next) {
    Tmp tmp = new Tmp();
    return exp.compileTo(tmp, new Ret(tmp));
  }
}

class VarDecl extends Stmt {
  private Type       type;
  private VarIntro[] vars;
  VarDecl(Type type, VarIntro[] vars) { this.type = type; this.vars = vars; }

  void print(int ind) {
    indent(ind);
    System.out.print(type.toString());
    for (int i=0; i<vars.length; i++) {
      System.out.print((i==0)? " " : ", ");
      vars[i].print();
    }
    System.out.println(";");
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    for (int i=0; i<vars.length; i++) {
      env = vars[i].check(type, env);
    }
    return env;
  }

  Code compile(Program prog, Code next) {
    int i = vars.length;
    while (--i>=0) {
      next = vars[i].compile(next);
    }
    return next;
  }
}

class VarIntro {
  protected String name;
  VarIntro(String name) { this.name = name; }

  void print() {
    System.out.print(name);
  }

  protected TypeEnv te;

  TypeEnv check(Type type, TypeEnv env) throws StaticError {
    return te = new TypeEnv(name, type, env);
  }

  Code compile(Code next) {
    Tmp tmp = new Tmp();
    return new Immed(tmp, 0,
           new Store(te.getLoc(), tmp,
           next));
  }
}

class InitVarIntro extends VarIntro {
  private Expr expr;
  InitVarIntro(String name, Expr expr) { super(name); this.expr = expr; }

  void print() {
    super.print();
    System.out.print(" = ");
    System.out.print(expr.show());
  }

  TypeEnv check(Type type, TypeEnv env) throws StaticError {
    expr.require(env, type);
    return super.check(type, env);
  }

  Code compile(Code next) {
    Tmp tmp = new Tmp();
    return expr.compileTo(tmp,
           new Store(te.getLoc(), tmp,
           next));
  }
}

//____________________________________________________________________________
// Further Implementation Required!

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

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    body.check(env, true, true);
    test.require(env, Type.BOOLEAN);
    return env;
  }

  Code compile(Program prog, Code next) {
    System.err.println("DoWhile compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

class Break extends Stmt {
  Break() { }

  void print(int ind) {
    indent(ind);
    System.out.println("break;");
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    if (!canBreak) {
      throw new StaticError("illegal use of break statement");
    }
    return env;
  }

  Code compile(Program prog, Code next) {
    System.err.println("Break compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

class Continue extends Stmt {
  Continue() { }

  void print(int ind) {
    indent(ind);
    System.out.println("continue;");
  }

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    if (!canContinue) {
      throw new StaticError("illegal use of continue statement");
    }
    return env;
  }

  Code compile(Program prog, Code next) {
    System.err.println("Continue compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

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

  TypeEnv check(TypeEnv env, boolean canContinue, boolean canBreak)
   throws StaticError {
    test.require(env, Type.INT);
    for (int i=0; i<cases.length; i++) {
      cases[i].check(env, canContinue);
      for (int j=i+1; j<cases.length; j++) {
        cases[i].distinctFrom(cases[j]);
      }
    }
    return env;
  }

  Code compile(Program prog, Code next) {
    System.err.println("DoWhile compile() method NOT IMPLEMENTED");
    System.exit(1);
    return next; // not reached
  }
}

abstract class Case {
  protected Stmt body;  // Note: body may be empty (i.e., null)
  Case(Stmt body) { this.body = body; }

  abstract void print(int ind);

  void check(TypeEnv env, boolean canContinue)
   throws StaticError {
    if (body!=null) {
      body.check(env, canContinue, true);
    }
  }

  // Methods for checking that cases are disjoint: a single
  // switch statement should not two default cases or two
  // numeric cases with the same integer value.
  abstract void distinctFrom(Case that) throws StaticError;
  abstract void notCaseFor(int num)     throws StaticError;
  abstract void notDefault()            throws StaticError;
}

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
