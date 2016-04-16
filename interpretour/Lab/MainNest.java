class MainNest {
  public static void main(String[] args) {
    Stmt s = new Seq(new VarDecl("i", new Int(0)),
             new Seq(new If(new EqEq(new Var("i"), new Int(0)),
                            new Seq(new VarDecl("i", new Int(1)),
                                    new Print(new Var("i"))),
                            new Assign("i", new Int(3))),
                     new Print(new Var("i"))));

    Program prog = new Program(s);

    System.out.println("Complete program is:");
    prog.print();

    System.out.println("Running in an empty environment:");
    prog.run();

    System.out.println("Done!");
  }
}
