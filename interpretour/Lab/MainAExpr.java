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
    Expr e2 = new Int(1);
    Expr e3 = new Int(3);
    Expr e4 = new Int(4);
    Expr e5 = new Array(e1);
    Expr e6 = new Int(2);
    Expr e7 = new Int(4);
    //Expr e8
    Stmt s = 
    new Seq(new VarDecl("array", new Plus(a1, a3)),
    new Seq(new VarDecl("range", new Range(new Plus(e2,e6),new Plus(e3,e7))),
    new Seq(new VarDecl("other", new Plus(a1, new Int(1))),
    new Seq(new Print(new Plus(new Plus(new Var("array"), new Var("range")), new Var("other"))),
    new Seq(new Print(new Length(new Var("array"))),
    new Print(new Nth(new Var("range"), e4)))))));

    Program prog = new Program(s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running program:");
    prog.run();

    System.out.println("Done!");	
  }
}
