abstract class Env {
  private String  var;
  private Env     rest;
  Env(String var, Env rest) {
    this.var = var;
    this.rest = rest;
  }
  
  abstract Value getValue ();
  abstract void setValue(Value val);

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

class ValEnv extends Env {
  private Value val;
  ValEnv(String var, Value val, Env rest) {
    super(var, rest);
    this.val = val;
  }

  Value getValue () { return val; }
  void setValue(Value val) { this.val = val; }
}

class RefEnv extends Env {
  private Env ref;
  RefEnv(String var, Env ref, Env rest) {
    super(var, rest);
    this.ref = ref;
  }

  Value getValue () { return ref.getValue(); }
  void setValue(Value val) { ref.setValue(val); }
}
