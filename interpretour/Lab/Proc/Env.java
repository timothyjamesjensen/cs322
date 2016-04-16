class Env {
  private String  var;
  private Value   val;
  private Env     rest;
  Env(String var, Value val, Env rest) {
    this.var = var;
    this.val = val;
    this.rest = rest;
  }
  
  Value getValue () { return val; }
  void setValue(Value val) { this.val = val; }

  static Env lookup(Env env, String name) {
    for (; env!=null; env=env.rest) {
      if (name.equals(env.var)) {
        return env;
      }
    } 
    System.out.println("ABORT: Variable " + name + " not defined");
    System.exit(1);
    return null; // not reached
  }
}
