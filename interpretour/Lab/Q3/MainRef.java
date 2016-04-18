class MainRef {
  public static void main(String[] args) {
    Proc[] procs = new Proc[] {
      new Proc("byref", new Formal[] {new ByRef("x"),
                                      new ByRef("y")},
        new Seq(new Call("redirect", new Expr[] {new Var("x")}),
        new Seq(new Call("redirect", new Expr[] {new Var("y")}),
                new Print(new Plus(new Var("x"), new Var("y")))))),

      new Proc("redirect", new Formal[] {new ByRef("x")},
        new Call("inc", new Expr[] {new Var("x")})),

      new Proc("inc", new Formal[] {new ByRef("y")},
        new Assign("y", new Plus(new Var("y"), new Int(1))))
    };

    Stmt s = new Seq(new VarDecl("z", new Int(25)),
             new Seq(new Call("byref",
                              new Expr[] {new Var("z"), new Int(23)}),
                     new Print(new Var("z"))));

    Program prog = new Program(procs, s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running program:");
    prog.run();

    System.out.println("Done!");
  }
}
