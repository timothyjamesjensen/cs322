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
  abstract Value eval(Memory mem);
  abstract String show();
}

class Var extends Expr {
  private String name;
  Var(String name) { this.name = name; }

  Value eval(Memory mem) { return mem.load(name); }
  String show() { return name; }
}

class Int extends Expr {
  private int num;
  Int(int num) { this.num = num; }

  Value eval(Memory mem) { return new IValue(num); }
  String show() { return Integer.toString(num); }
}

class Plus extends Expr {
  private Expr l, r;
  Plus(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Memory mem) { 
    return new IValue(l.eval(mem).asInt() + r.eval(mem).asInt()); 
  }
  String show() { return "(" + l.show() + " + " + r.show() + ")"; }
}

class Mult extends Expr {
  private Expr l, r;
  Mult(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Memory mem) { 
    return new IValue(l.eval(mem).asInt() * r.eval(mem).asInt()); 
  }
  String show() { return "(" + l.show() + " * " + r.show() + ")"; }
}

class Minus extends Expr {
  private Expr l, r;
  Minus(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Memory mem) { 
    return new IValue(l.eval(mem).asInt() - r.eval(mem).asInt()); 
  }
  String show() { return "(" + l.show() + " - " + r.show() + ")"; }
}


class LT extends Expr {
  private Expr l, r;
  LT(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Memory mem) { 
    return new BValue(l.eval(mem).asInt() < r.eval(mem).asInt()); 
  }
  String show()  { return "(" + l.show() + " < " + r.show() + ")"; }
}

class EqEq extends Expr {
  private Expr l, r;
  EqEq(Expr l, Expr r) { this.l = l; this.r = r; }

  Value eval(Memory mem) { 
    return new BValue(l.eval(mem).asInt() == r.eval(mem).asInt()); 
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
  abstract void exec(Memory mem);
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

  void exec(Memory mem) {
    l.exec(mem);
    r.exec(mem);
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

  void exec(Memory mem) {
    mem.store(lhs, rhs.eval(mem));
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

  void exec(Memory mem) {
    while (test.eval(mem).asBool()) {
      body.exec(mem);
    }
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

  void exec(Memory mem) {
    if (test.eval(mem).asBool()) {
      t.exec(mem);
    } else {
      f.exec(mem);
    }
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

  void exec(Memory mem) {
    System.out.println("Output: " + exp.eval(mem).asInt());
  }

  void print(int ind) {
    indent(ind);
    System.out.println("print " + exp.show() + ";");
  }
}
