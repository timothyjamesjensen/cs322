abstract class Value {
  abstract String show();

  boolean asBool() {
    System.out.println("ABORT: Boolean value expected");
    System.exit(1);
    return true; // not reached
  }

  int asInt() {
    System.out.println("ABORT: integer value expected");
    System.exit(1);
    return 42; // not reached
  }
}

class BValue extends Value {
  private boolean b;
  BValue(boolean b) { this.b = b; }
  
  String show() { return Boolean.toString(b); }

  boolean asBool() { return b; }
}

class IValue extends Value {
  private int i;
  IValue(int i) { this.i = i; }

  String show() { return Integer.toString(i); }

  int asInt() { return i; }
}

//____________________________________________________________________________
// Expr ::= Var
//        |  Int
//        |  Expr + Expr
//        |  Expr - Expr

abstract class Expr {
  abstract Value eval(Env env);
  abstract String show();
}

class Var extends Expr {
  private String name;
  Var(String name) { this.name = name; }

  Value eval(Env env) { 
    return Env.lookup(env, name).getValue();
  }
  String show() { return name; }
}

class Int extends Expr {
  private int num;
  Int(int num) { this.num = num; }

  Value eval(Env env) { return new IValue(num); }
  String show() { return Integer.toString(num); }
}

class Plus extends Expr {
  private Expr l, r;
  Plus(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Env env) { 
    return new IValue(l.eval(env).asInt() + r.eval(env).asInt()); 
  }
  String show() { return "(" + l.show() + " + " + r.show() + ")"; }
}

class Mult extends Expr {
  private Expr l, r;
  Mult(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Env env) { 
    return new IValue(l.eval(env).asInt() * r.eval(env).asInt()); 
  }
  String show() { return "(" + l.show() + " * " + r.show() + ")"; }
}

class Minus extends Expr {
  private Expr l, r;
  Minus(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Env env) { 
    return new IValue(l.eval(env).asInt() - r.eval(env).asInt()); 
  }
  String show() { return "(" + l.show() + " - " + r.show() + ")"; }
}


class LT extends Expr {
  private Expr l, r;
  LT(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Env env) { 
    return new BValue(l.eval(env).asInt() < r.eval(env).asInt()); 
  }
  String show()  { return "(" + l.show() + " < " + r.show() + ")"; }
}

class EqEq extends Expr {
  private Expr l, r;
  EqEq(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Env env) { 
    return new BValue(l.eval(env).asInt() == r.eval(env).asInt()); 
  }
  String show()  { return "(" + l.show() + " == " + r.show() + ")"; }
}

//____________________________________________________________________________
// Stmt  ::= Seq Stmt Stmt
//        |  Var := Expr
//        |  While Expr Stmt
//        |  If Expr Stmt Stmt
//        |  Print Expr

abstract class Stmt {
  abstract Env exec(Env env);
  abstract void print(int ind);

  static void indent(int ind) {
    for (int i=0; i<ind; i++) {
      System.out.print(" ");
    }
  }
}

class Seq extends Stmt {
  private Stmt l, r;
  Seq(Stmt l, Stmt r) { this.l = l; this.r = r; }

  Env exec(Env env) {
    return r.exec(l.exec(env));
  }

  void print(int ind) {
    l.print(ind);
    r.print(ind);
  }
}

class Assign extends Stmt {
  private String lhs;
  private Expr  rhs;
  Assign(String lhs, Expr rhs) {
    this.lhs = lhs; this.rhs = rhs;
  }

  Env exec(Env env) {
    Env.lookup(env, lhs).setValue(rhs.eval(env));
    return env;
  }

  void print(int ind) {
    indent(ind);
    System.out.println(lhs + " = " + rhs.show() + ";");
  }
}

class While extends Stmt {
  private Expr test;
  private Stmt  body;
  While(Expr test, Stmt body) {
    this.test = test; this.body = body;
  }

  Env exec(Env env) {
    while (test.eval(env).asBool()) {
      body.exec(env);
    }
    return env;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("while (" + test.show() + ") {");
    body.print(ind+2);
    indent(ind);
    System.out.println("}");
  }
}

class If extends Stmt {
  private Expr test;
  private Stmt  t, f;
  If(Expr test, Stmt t, Stmt f) {
    this.test = test; this.t = t; this.f = f;
  }

  Env exec(Env env) {
    if (test.eval(env).asBool()) {
      t.exec(env);
    } else {
      f.exec(env);
    }
    return env;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("if (" + test.show() + ") {");
    t.print(ind+2);
    indent(ind);
    System.out.println("} else {");
    f.print(ind+2);
    indent(ind);
    System.out.println("}");
  }
}

class Print extends Stmt {
  private Expr exp;
  Print(Expr exp) { this.exp = exp; }

  Env exec(Env env) {
    System.out.println("Output: " + exp.eval(env).asInt());
    return env;
  }

  void print(int ind) {
    indent(ind);
    System.out.println("print " + exp.show() + ";");
  }
}

class VarDecl extends Stmt {
  private String var;
  private Expr expr;
  VarDecl(String var, Expr expr) { this.var = var; this.expr = expr; }

  Env exec(Env env) {
    return new Env(var, expr.eval(env), env);
  }
  
  void print (int ind) {
    indent(ind);
    System.out.println("var " + var + " = " + expr.show() + ";");
  }
}
