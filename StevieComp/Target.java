import java.util.Vector;

/** Implements a compiler from Stevie to the target language.
 */
class StevieComp {

  public static void main(String[] args) {
    // Read and parse a source program from standard input
    new Parser(System.in);
    Stmt p = null;
    try {
      p = Parser.stmts();
    } catch (ParseException e) {
      System.out.println("Syntax Error");
      System.exit(1);
    }

    // Display the source program:
    System.out.println("Complete program is:");
    p.print(4);

    // Run static analysis on the source program:
    new StaticAnalysis().run(p);
    System.out.println("Passes static analysis!");

    // Compile to the target language:
    System.out.println("Compiling:");
    Program target = new Program();
    Tmp     result = new Tmp();
    Code    done   = new Immed(result, 0,
                     new Ret(result));
    Block   entry  = target.block(p.compile(target, done));
    System.out.println("Entry point is at " + entry);
    target.show();
    
    // Run the target language program:
    System.out.println("Running on an empty memory:");
    Memory mem = new Memory();
    Block  pc  = entry;
    while (pc!=null)  {
      pc = pc.code().run(mem);
    } 

    // Display final result:
    System.out.println("Return value was: " + mem.load(0));
  }
}

/** Represent a memory as a simple array of integers,
 *  identifying individual variables by their locations.
 *  But how will we calculate those locations ... ?
 */
class Memory {
  private int[] mem = new int[100];

  int load(int loc) { return mem[loc]; }
  void store(int loc, int val) { mem[loc] = val; }
}

class Program {
  private Vector<Block> blocks = new Vector<Block>();

  Block block(Code code) {
    Block l = new Block(blocks.size(), code);
    blocks.add(l);
    return l;
  }

  Block block() {
    return block(null);
  }

  void show() {
    for (int i=0; i<blocks.size(); i++) {
      blocks.elementAt(i).print();
      System.out.println();
    }
  }
}  

class Block {
  private int num;
  private Code code;

  Block(int num, Code code) {
    this.num  = num;
    this.code = code;
  }

  void set(Code code) {
    this.code = code;
  }

  Code code() {
    return code;
  }

  public String toString() {
    return "L" + num;
  }

  void print() {
    System.out.println(this + ":");
    code.print();
  }
}

class Tmp {
  private static int count = 0;
  private int num;
  private int val = 0;

  Tmp() { num = count++; }

  public String toString() { return "r" + num; }

  void set(int x) { val = x; }
  int  get()      { return val; }

  void setBool(boolean x) { val = (x ? 1 : 0); }
  boolean getBool() { return (val != 0); }
}

//____________________________________________________________________________
// Code ::=  Ret Tmp
//        |  Goto Block
//        |  Cond Tmp Block Block
//        |  Load Tmp Var Code
//        |  Store Var Tmp Code
//        |  Immed Tmp Int Code
//        |  Op Tmp Tmp Tmp Code
//        |  PCode Tmp Code

abstract class Code {
  abstract Block run(Memory mem);
  abstract void print();
}

class Ret extends Code {
  private Tmp reg;
  Ret(Tmp reg) { this.reg = reg; }

  void print() {
    System.out.println("  ret " + reg);
  }

  Block run(Memory mem) {
    mem.store(0, reg.get());
    return null;
  }
}

class Goto extends Code {
  private Block block;
  Goto(Block block) {
    this.block = block;
  }

  void print() {
    System.out.println("  goto " + block);
  }

  Block run(Memory mem) {
    return block;
  }
}

class Cond extends Code {
  private Tmp reg;
  private Block t, f;
  Cond(Tmp reg, Block t, Block f) {
    this.reg = reg;
    this.t   = t;
    this.f   = f;
  }

  void print() {
    System.out.println( "  " + reg + " -> " + t + ", " + f);
  }

  Block run(Memory mem) {
    return reg.getBool() ? t : f;
  }
}

class Load extends Code {
  private Tmp reg;
  private int loc;
  private Code next;
  Load(Tmp reg, int loc, Code next) {
    this.reg  = reg;
    this.loc  = loc;
    this.next = next;
  }

  void print() {
    System.out.println("  " + reg + " <- [" + loc + "]");
    next.print();
  }

  Block run(Memory mem) {
    reg.set(mem.load(loc));
    return next.run(mem);
  }
}

class Store extends Code {
  private int  loc;
  private Tmp  reg;
  private Code next;
  Store(int loc, Tmp reg, Code next) {
    this.loc  = loc;
    this.reg  = reg;
    this.next = next;
  }

  void print() {
    System.out.println("  [" + loc + "] <- " + reg);
    next.print();
  }

  Block run(Memory mem) {
    mem.store(loc, reg.get());
    return next.run(mem);
  }
}

class Immed extends Code {
  private Tmp reg;
  private int num;
  private Code next;
  Immed(Tmp reg, int num, Code next) {
    this.reg = reg;
    this.num = num;
    this.next = next;
  }

  void print() {
    System.out.println("  " + reg + " <- " + num);
    next.print();
  }

  Block run(Memory mem) {
    reg.set(num);
    return next.run(mem);
  }
}

class Op extends Code {
  private Tmp r, x, y;
  private char op;
  private Code next;
  Op(Tmp r, Tmp x, char op, Tmp y, Code next) {
    this.r    = r;
    this.x    = x;
    this.op   = op;
    this.y    = y;
    this.next = next;
  }

  void print() {
    System.out.println("  " + r + " <- " + x + op + y);
    next.print();
  }

  Block run(Memory mem) {
    switch (op) {
      case '+' : r.set(x.get() + y.get());
                 break;
      case '-' : r.set(x.get() - y.get());
                 break;
      case '*' : r.set(x.get() * y.get());
                 break;
      case '<' : r.setBool(x.get() < y.get());
                 break;
      case '=' : r.setBool(x.get() == y.get());
                 break;
    }
    return next.run(mem);
  }
}

class PCode extends Code {
  private Tmp reg;
  private Code next;
  PCode(Tmp reg, Code next) {
    this.reg  = reg;
    this.next = next;
  }

  void print() {
    System.out.println("  print " + reg);
    next.print();
  }

  Block run(Memory mem) {
    System.out.println("Output: " + reg.get());
    return next.run(mem);
  }
}
