class MainArray {
  public static void main(String[] args) {
    AValue a0 = new SingleValue(new IValue(12));
    AValue a1 = new SingleValue(new FValue(null, "x", new Var("x")));
    AValue a2 = new SingleValue(new BValue(true));
    AValue a3 = new RangeValue(1, 5);
    AValue a4 = new MultiValue(new Value[] {
                    new IValue(11), new BValue(false)
                });
    AValue a5 = new MultiValue(new Value[] { a0, a1, a2, a3, a4 });
    /*AValue a6 = new ConcatValue(a0, a1);
    AValue a7 = new ConcatValue(a3, a3);
*/
    System.out.println(a0.show());
    System.out.println(a1.show());
    System.out.println(a2.show());
    System.out.println(a3.show());
    System.out.println(a4.show());
    System.out.println(a5.show());
  /*  System.out.println(a6.show());
    System.out.println(a7.show());
*/
  }
}  
