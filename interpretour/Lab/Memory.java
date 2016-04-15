import java.util.Hashtable;

class Memory {
  private Hashtable<String,Value> memory
     = new Hashtable<String,Value>();

  Value load(String name) {
    Value val = memory.get(name);
    if (val==null) {
      System.out.println("ABORT: Variable " + name + " not defined");
      System.exit(1);
    }
    return val;
  }

  void store(String name, Value val) {
    memory.put(name, val);
  }
}
