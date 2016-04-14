class MainFCF {
  public static void main(String[] args) {
    Proc[] procs = new Proc[] {
      new Proc("adder", new Formal[] {new Formal("n"), new ByRef("f")},
        new Assign("f", new Lambda("m",
                           new Plus(new Var("n"),
                                    new Var("m")))))
    };

    Stmt s = new Seq(new VarDecl("double", new Lambda("x",
                                              new Plus(new Var("x"),
                                                       new Var("x")))),
             new Seq(new VarDecl("inc",    new Lambda("x",
                                              new Plus(new Var("x"),
                                                       new Int(1)))),
             new Seq(new VarDecl("comp",   new Lambda("f",
                                           new Lambda("g",
                                           new Lambda("x",
                                             new Apply(new Var("f"),
                                             new Apply(new Var("g"),
                                                       new Var("x"))))))),
             new Seq(new VarDecl("toOdd", new Apply(
                                            new Apply(new Var("comp"),
                                                      new Var("inc")),
                                            new Var("double"))),
             new Seq(new Print(new Apply(new Var("toOdd"), new Int(3))),
             new Seq(new Print(new Apply(new Var("toOdd"), new Int(9))),
             new Seq(new Call("adder", new Expr[] {new Int(4),
                                                   new Var("inc")}),
             new Seq(new Print(new Apply(new Var("inc"),new Int(1))),
                     new Print(new Apply(new Var("inc"),new Int(0)))))))))));

    Program prog = new Program(procs, s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running program:");
    prog.run();

    System.out.println("Done!");
  }
}
