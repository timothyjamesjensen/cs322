class MainDT {
  public static void main(String[] args) {

    // i = (0 == 0);    sets i to a boolean value
    Stmt setBool = new Assign("i", new EqEq(new Int(0), new Int(0)));

    // print i;         prints i (requires integer)
    Stmt printI  = new Print(new Var("i"));

    // i = 41 + 1;      sets i to an integer value
    Stmt setInt  = new Assign("i", new Plus(new Int(41), new Int(1)));

    Stmt s
     = new Seq(setBool,
       new Seq(new If(new Var("i"), setInt, printI),
               printI));

    System.out.println("Complete program is:");
    s.print(4);

    System.out.println("Running on an empty memory:");
    Memory mem = new Memory();
    s.exec(mem);

    System.out.println("Done!");
  }
}
