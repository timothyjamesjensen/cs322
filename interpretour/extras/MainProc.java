class MainProc {
  public static void main(String[] args) {
    Proc[] procs = new Proc[] {
      new Proc("gauss", new String[] {"limit"},
        new Seq(new VarDecl("t", new Int(0)),
        new Seq(new VarDecl("i", new Int(0)),
        new Seq(new While(new LT(new Var("i"), new Var("limit")),
                          new Seq(new Assign("i", new Plus(new Var("i"),
                                                            new Int(1))),
                                  new Assign("t", new Plus(new Var("t"),
                                                           new Var("i"))))),
               new Print(new Var("t")))))),

      new Proc("double", new String[] {"x"},
        new Print(new Plus(new Var("x"), new Var("x")))),

      new Proc("sum", new String[] {"n", "a"},
        new If(new LT(new Int(0), new Var("n")),
               new Call("sum", new Expr[] {
                                 new Minus(new Var("n"), new Int(1)),
                                 new Plus(new Var("a"), new Var("n"))
                               }),
               new Print(new Var("a"))))
    };

    Stmt s = new Seq(new VarDecl("n", new Int(97207)),
             new Seq(new Call("gauss", new Expr[] {new Int(5)}),
             new Seq(new Call("double", new Expr[] {new Int(21)}),
             new Seq(new Call("gauss", new Expr[] {new Int(10)}),
             new Seq(new Call("sum", new Expr[] {new Int(10), new Int(0)}),
                     new Print(new Var("n")))))));

    Program prog = new Program(procs, s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running program:");
    prog.run();

    System.out.println("Done!");
  }
}
