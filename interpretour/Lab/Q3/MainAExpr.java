class MainAExpr {
  public static void main (String[] args) {

    Expr arr[] = new Expr[1];
    Expr array[] = new Expr[1];
    Expr array1[] = new Expr[2];
    array1[0] = new Int(1);
    array1[1] = new Int(2);
    Expr e1[] = new Expr[2];
    e1[0] = new Int(3);
    e1[1] = new Int(4);

    Array a1 = new Array(array1);
    Array a2 = new Array(e1);
    Array a3 = new Array(arr);
    array[0] = new Plus(a1, a2);
    Expr int1 = new Int(1);
    Expr int3 = new Int(3);
    Expr int4 = new Int(4);
    Expr int2 = new Int(2);

    Expr vals[] = new Expr[3];
    vals[0] = int2; vals[1] = int3; vals[2] = int4;
    Expr a4 = new Array(vals);  
 
    Stmt s = 
    new Seq(new VarDecl("array", new Plus(a1, a2)),
    new Seq(new VarDecl("range", new Range(new Plus(int1,int2),new Plus(int3,int4))),
    new Seq(new VarDecl("other", new Plus(new Plus(a3, new Int(1)), a4)),
    new Seq(new Print(new Plus(new Plus(new Var("array"), new Var("range")), new Var("other"))),
    new Seq(new Print(new Length(new Var("other"))),
    new Print(new Nth(new Var("range"), int4)))))));

    Program prog = new Program(s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running program:");
    prog.run();

    System.out.println("Done!");	
  }
}
