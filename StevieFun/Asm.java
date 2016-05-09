import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/** Provides a simple mechanism for assembly language output.
 */
class Assembly {

    /** Used to store the output stream for this assembly file.
     */
    private PrintWriter out;

    /** A private constructor, used from the assembleToFile() method.
     */
    private Assembly(PrintWriter out) {
        this.out = out;
    }

    /** Include this code in the platform choice if external symbols
     *  require a leading underscore.
     */
    static final int UNDERSCORES = 1;

    /** Include this code in the platform choice if the code generator
     *  should ensure that stack frames are aligned on a 16 byte boundary.
     */
    static final int ALIGN16 = 2;

    /** Platform flags for Linux.
     */
    static final int LINUX = 0;

    /** Platform flags for Mac OS X.
     */
    static final int MACOSX = UNDERSCORES | ALIGN16;

    /** Set the platform flag for this machine.
     */
    static final int platform = LINUX;

    /** A convenience method that creates an assembly object for
     *  a named output file.
     */
    static final boolean DEBUG_FRAMES = false;

    /** A convenience method that creates an assembly object for
     *  a named output file.
     */
    public static Assembly assembleToFile(String name) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(name));
            Assembly    a   = new Assembly(out);
            a.emit(".file",  "\"" + name + "\"");
            return a;
        } catch (IOException e) {
            return null;
        }
    }

    /** Close this Assembly object and free up associated resources.
     */
    public void close() {
        out.close();
        out = null;
    }

    /** A counter that is used to generate new labels; the counter is
     *  incremented each time a new label is produced.
     */
    private int labelCounter = 0;

    /** Generate a string for a label from an integer input.
     *  We require that distinct inputs produce distinct outputs
     *  and that none of the generated label names can clash with
     *  names in user programs.
     */
    public String label(int l) {
        return "l" + l;
    }

    /** Return a fresh (i.e., previously unused) label name.
     */
    public String newLabel() {
        return label(labelCounter++);
    }

    /** Output a label at the beginning of a line.
     */
    public void emitLabel(String name) {
        handlePendingAdjust();
        out.print(name);
        out.println(":");
    }

    /** Emit a blank line.
     */
    public void emit() {
        out.println();
    }

    /** Emit an instruction with no operands.
     */
    public void emit(String op) {
        handlePendingAdjust();
        out.println("\t" + op);
    }

    /** Emit an instruction with one operand.
     */
    public void emit(String op, String op1) {
        handlePendingAdjust();
        out.println("\t" + op + "\t" + op1);
    }

    /** Emit an instruction with two operands.
     */
    public void emit(String op, String op1, String op2) {
        handlePendingAdjust();
        out.println("\t" + op + "\t" + op1 + ", " + op2);
    }

    /** Return a number as a string for use in contexts where only
     *  a number is allowed and hence no special characters are
     *  required to distinguish an addressing mode.
     */
    public String number(int v) {
        return Integer.toString(v);
    }

    /** Return a string for an operand using immediate addressing.
     */
    public String immed(int v) {
        return "$" + v;
    }

    /** Output a function/variable name using the appropriate
     *  platform naming conventions with respect to underscores.
     */
    public String name(String n) {
        return (platform & UNDERSCORES)==0 ? ("X"+n) : ("_X" + n);
    }

    /** Return a reference to a memory location using indirect
     *  addressing.
     */
    public String indirect(int n, String s) {
        if (n==0) {
            return "(" + s + ")";
        } else {
            return n + "(" + s + ")";
        }
    }

    /** Return a reference to a memory location using scaled, indexed
     *  addressing.  The base and index should be compatible register
     *  names; scale should be a valid size (1, 2, 4, or 8).
     */
    public String indexed(String base, String index, int scale) {
        return "(" + base + ", " + index + ", " + scale + ")";
    }

    /** We assume that all values can be stored within a single "quadword"
     *  (i.e., 64 value), each of which takes 8 bytes in memory:
     */
    public static final int QUADSIZE = 8;

    /** Track the number of bytes by which the stack pointer should be
     *  adjusted before the next instruction.  This mechanism allows us
     *  to combine multiple adjustments into a single instruction, and
     *  to omit adjustments completely when they are not required (for
     *  example, immediately before a function's epilogue).
     */
    private int pendingAdjust = 0;

    /** Check to see if a stack adjustment is required before the next
     *  instruction is emitted.
     */
    public void handlePendingAdjust() {
        if (pendingAdjust!=0) {
            int adjust    = pendingAdjust;
            pendingAdjust = 0;
            if (adjust>0) {
                emit("subq", immed(adjust),  Reg.stackPointer.r64());
            } else {
                emit("addq", immed(-adjust), Reg.stackPointer.r64());
            }
        }
    }

    /** Adjust the stack by inserting space for the specified number of
     *  bytes.  Can be used to reserve space for locals, or to establish
     *  alignment constraints.  Can also be used with a negative argument
     *  to remove space.
     */
    void insertAdjust(int adjust) {
        pendingAdjust += adjust;
    }

    /** Calculate how many additional bytes need to be pushed onto
     *  the stack to ensure correct alignment once "pushed" bytes
     *  have been pushed.
     */
    int alignmentAdjust(int pushed) {
        // For platforms that need it (i.e., Mac OS X), we determine
        // how many extra bytes must be added to the stack to ensure
        // alignment on a 16 byte boundary.  For other platforms, we
        // can just return zero.
        return ((platform & ALIGN16)==0) ? 0 : ((16 - pushed) & 15);
    }

    /** Output the prologue code section at the start of a function.
     */
    public void emitPrologue() {
        emit("pushq", Reg.basePointer.r64());
        emit("movq",  Reg.stackPointer.r64(), Reg.basePointer.r64());
        if (Assembly.DEBUG_FRAMES) {
            emit("# end prologue");
            emit();
        }
    }

    /** Output the epilogue code section at the end of a program.
     */
    public void emitEpilogue() {
        pendingAdjust = 0;
        if (Assembly.DEBUG_FRAMES) {
            emit();
            emit("# start epilogue");
        }
        emit("movq", Reg.basePointer.r64(), Reg.stackPointer.r64());
        emit("popq", Reg.basePointer.r64());
        emit("ret");
    }
}

/** Represents a 64 bit register that also has a 32 bit register
 *  included as a component.
 */
class Reg {

    /** The name of this register for 32 bit uses.
     */
    private String name32;

    /** The name of this register for 64 bit uses.
     */
    private String name64;

    /** Default constructor.
     */
    public Reg(String name32, String name64) {
        this.name32 = name32;
        this.name64 = name64;
    }

    /** Return the name of this register for 32 bit uses.
     */
    public String r32() { return name32; }

    /** Return the name of this register for 64 bit uses.
     */
    public String r64() { return name64; }

    /** The list of registers that are used to pass argument values.
     */
    public static final Reg[] args = new Reg[] {
        new Reg("%edi", "%rdi"),
        new Reg("%esi", "%rsi"),
        new Reg("%ecx", "%rcx"),
        new Reg("%edx", "%rdx"),
        new Reg("%r8d", "%r8"),
        new Reg("%r9d", "%r9")
      };

    /** The list of registers that are used to return results.
     */
    public static final Reg[] results = new Reg[] {
        new Reg("%eax", "%rax")
      };

    /** The list of caller saves registers.
     */
    public static final Reg[] callerSaves = new Reg[] {
        new Reg("%r10d", "%r10"),
        new Reg("%r11d", "%r11")
      };

    /** The list of callee saves registers.
     */
    public static final Reg[] calleeSaves = new Reg[] {
        new Reg("%ebx",  "%rbx"),
        new Reg("%r12d", "%r12"),
        new Reg("%r13d", "%r13"),
        new Reg("%r14d", "%r14"),
        new Reg("%r15d", "%r15")
      };

    /** The base pointer register.
     */
    public static final Reg basePointer = new Reg("%ebp",  "%rbp");

    /** The stack pointer register.
     */
    public static final Reg stackPointer = new Reg("%esp",  "%rsp");
}

abstract class Width {
  abstract int    bytes();
  abstract String mov();
  abstract String reg(Reg r);
  abstract String free(Frame f);
  abstract String storage();

  /** Describes data values stored in a 4 byte / 32 bit word.
   */
  public static final Width data = new Width() {
    int    bytes()       { return 4; }
    String mov()         { return "movl"; }
    String reg(Reg r)    { return r.r32(); }
    String free(Frame f) { return f.free32(); }
    String storage()     { return ".long"; }
  };

  /** Describes address values stored in an 8 byte / 64 bit word.
   */
  public static final Width addr = new Width() {
    int    bytes()       { return 8; }
    String mov()         { return "movq"; }
    String reg(Reg r)    { return r.r64(); }
    String free(Frame f) { return f.free64(); }
    String storage()     { return ".quad"; }
  };
}

/** Captures information about the layout of the frame for a given
 *  function, including details about register use as well as stack
 *  layout.
 */
abstract class Frame {

    /** Records the list of formal parameters for the corresponding
     *  function.
     */
    protected Formal[] formals;

    /** Holds an environment describing the mappings from variables
     *  to locations in the current stack frame.
     */
    protected LocEnv env;

    /** Default constructor.
     */
    public Frame(Formal[] formals, LocEnv env) {
        this.formals = formals;
        this.env = env;
    }

    /** Return the environment at this point in the code.
     */
    public LocEnv getEnv() { return env; }

    /** Holds the register map for this layout.
     */
    protected Reg[] regmap;

    /** Map from logical register number to physical register.
     */
    protected Reg reg(int n) {
        return regmap[n % regmap.length];
    }

    /** Holds the index of the first parameter register.
     */
    protected int paramBase;

    /** Holds the index of the first free register.
     */
    protected int freeBase;

    /** Holds the index of the current free register.
     */
    protected int free;

    /** Holds the number of bytes that are currently pushed on
     *  the stack.  Initially, of course, there are no bytes
     *  pushed on the stack.
     */
    protected int pushed = 0;

    /** Return the current free register.
     */
    public Reg free() { return reg(free); }

    /** Return the name of the current 32 bit register.
     */
    public String free32() { return free().r32(); }

    /** Return the name of the current 64 bit register.
     */
    public String free64() { return free().r64(); }

    /** Save the value in the current free register in
     *  the location corresponding to a particular value.
     */
    void store(Assembly a, String lhs) {
        LocEnv le = env.find(lhs);
        Width  wd = le.getWidth();
        a.emit(wd.mov(), wd.free(this), le.loc(a));
    }

    /** Load a value from a location corresponding to a particular
     *  variable into the current free register.
     */
    void load(Assembly a, String name) {
        LocEnv le = env.find(name);
        Width  wd = le.getWidth();
        a.emit(wd.mov(), le.loc(a), wd.free(this));
    }

    /** Make the next available register free, spilling the contents
     *  of that register on to the stack if it was already in use.
     *  Every call to spill() must also be paired with a correponding
     *  call to unspill().
     */
    public Reg spill(Assembly a) {
        Reg r = reg(++free);
        // Save old register value if necessary:
        if (free>=regmap.length) {
            // Save register on the stack
            a.emit("pushq", r.r64());
            pushed += Assembly.QUADSIZE;

            // If we just spilled a formal parameter, update
            // the environment to reflect that.
            int n = free - (regmap.length + paramBase);
            if (n>=0 && formals!=null && n<formals.length && n<Reg.args.length) {
                env = formals[n].extend(-pushed, env);
            }
        }
        return r;
    }

    /** Spill, as necessary, to ensure that the next free register is
     *  available for use, returning the associated 32 bit register
     *  name as a result.
     */
    public String spill32(Assembly a) { return spill(a).r32(); }

    /** Spill, as necessary, to ensure that the next free register is
     *  available for use, returning the associated 32 bit register
     *  name as a result.
     */
    public String spill64(Assembly a) { return spill(a).r64(); }

    /** Release the current free register, potentially unspilling a
     *  previously saved value for the underlying physical memory
     *  from the stack.  Pairs with a previous call to spill().
     */
    public void unspill(Assembly a) {
        Reg r = reg(free);
        if (free>=regmap.length) {
            // Restore saved register value:
            a.emit("popq", r.r64());
            pushed -= Assembly.QUADSIZE;

            // If we just unspilled a formal parameter, update
            // the environment to reflect that.
            int n = free - (regmap.length + paramBase);
            if (n>=0 && formals!=null && n<formals.length && n<Reg.args.length) {
                env = env.next();
            }
        }
        free--;
    }

    /** Allocate space on the stack for a local variable.
     */
    public void allocLocal(Assembly a, String name, Type type, String src) {
        a.emit("pushq", src);
        pushed += Assembly.QUADSIZE;
        env = new FrameEnv(name, type.getWidth(), env, -pushed);
    }

    /** Reset the stack pointer to a previous position at the end
     *  of a block, decrementing the stack pointer as necessary
     *  and removing items from the environment to reflect local
     *  variables going out of scope.
     */
    public void resetTo(Assembly a, LocEnv origEnv) {
        for (; env!=origEnv; env=env.next()) {
            pushed -= Assembly.QUADSIZE;
            a.insertAdjust(-Assembly.QUADSIZE);
        }
    }

    /** Add some number of bytes at the top of the stack, typically
     *  to meet some alignment constraint.
     */
    public void insertAdjust(Assembly a, int adjust) {
        pushed += adjust;
        a.insertAdjust(adjust);
    }

    /** Dump a description of this frame.
     */
    public void dump(Assembly a) {
        if (Assembly.DEBUG_FRAMES) {
            a.emit("# Registers: (free = " + free64() + ")");
            StringBuffer b = new StringBuffer("# ");
            int i = 0;
            for (; i<paramBase; i++) {
                b.append(" ");
                b.append(regmap[i].r64());
            }
            b.append(" <");
            for (; i<freeBase; i++) {
                b.append(" ");
                b.append(regmap[i].r64());
            }
            b.append(" >");
            for (; i<regmap.length; i++) {
                b.append(" ");
                b.append(regmap[i].r64());
            }
            a.emit(b.toString());
            a.emit("#");
            a.emit("# Pushed on stack: " + pushed);
            b = new StringBuffer("# Environment:");
            for (LocEnv env=this.env; env!=null; env=env.next()) {
                String n = env.name;
                String s = env.loc(a);
                b.append(" ");
                b.append(n);
                if (!n.equals(s)) {
                   b.append("->" + s);
                }
            }
            a.emit(b.toString());
            a.emit("#");
        }
    }

    /** Create a new Frame for a call from within the current frame.
     *  This entails saving the values of any active caller saves
     *  registers on the stack and updating the environment to record
     *  the new locations for each formal parameter.
     */
    public CallFrame prepareCallFrame(Assembly a, int nargs) {
        // Save [b..r), counting down, skipping callee saves registers.
        int    r   = free;
        int    b   = Math.max(paramBase, free - (regmap.length+1));
        LocEnv env = this.env;
        while (--r>=b) {
            int rmod = r % regmap.length;
            if (rmod>=paramBase) {   // Caller saves registers
                pushed += a.QUADSIZE;// must be saved on the stack
                a.emit("pushq", reg(r).r64());
                if (r<freeBase) {    // Parameter registers?
                    // Update environment to indicate that a parameter
                    // variable is now on the stack instead of a register.
                    env = formals[r-paramBase].extend(-pushed, env);
                }
            }
        }

        // Calculate space needed for stack arguments:
        int argBytes = Math.max(0, nargs-Reg.args.length) * a.QUADSIZE;

        // Add bytes as necessary to ensure correct alignment:
        argBytes    += a.alignmentAdjust(pushed + argBytes);

        // Create the new call frame:
        CallFrame cf = new CallFrame(env, pushed, argBytes);

        // The current frame includes saved registers for this frame,
        // but the argBytes are part of the call frame:
        cf.insertAdjust(a, argBytes);

        return cf;
    }

    /** Restore a frame to its original state after a call, ensuring that
     *  the return result from the function is in the free register and
     *  restoring any saved registers from the stack.
     */
    public void removeCallFrame(Assembly a) {
        // Move result into free register.
        Reg result = Reg.results[0];
        if (free()!=result) {
            a.emit("movq", result.r64(), free64());
        }

        // Restore [b..r), counting up, skipping callee saves registers.
        int r = free;
        int b = Math.max(paramBase, free - (regmap.length+1));
        for (; b<r; b++) {
            int bmod = b % regmap.length;
            if (bmod>=paramBase) {   // Caller saves registers
                pushed -= a.QUADSIZE;
                a.emit("popq", reg(b).r64());
            }
        }
    }
}

class FunctionFrame extends Frame {

    /** Construct a new Frame Layout object for a function with the
     *  given list of formal parameters and the given environment
     *  describing global variables.
     */
    public FunctionFrame(Formal[] formals, LocEnv globals) {
        super(formals, globals);

        // Initialize the register map, including paramBase,
        // freeBase, and free:
        regmap = new Reg[Reg.calleeSaves.length
                       + Reg.args.length
                       + Reg.results.length
                       + Reg.callerSaves.length];
        int r  = 0;
        int i;
        // Callee Saves Register are considered "in use" from the
        // start of the function.
        for (i=0; i<Reg.calleeSaves.length; i++) {
            regmap[r++] = Reg.calleeSaves[i];
        }

        // Next come the registers that are used to supply parameters.
        // These registers also contribute entries to the environment.
        paramBase = r;
        for (i=0; i<Reg.args.length && i<formals.length; i++) {
            regmap[r++] = Reg.args[i];
            env         = formals[i].extend(Reg.args[i], env);
        }

        // Any formal parameters that did not fit in registers will be
        // found on the stack at positive offsets from the base pointer,
        // and will require corresponding entries in the stack frame.
        for (int j=i; j<formals.length; j++) {
            int offset = (2+j-i)*Assembly.QUADSIZE;
            env        = formals[j].extend(offset, env);
        }
        
        // Any remaining registers are considered free for use, starting
        // with the result register(s):
        freeBase = free = r;
        for (int j=0; j<Reg.results.length; j++) {
            regmap[r++] = Reg.results[j];
        }
        // Followed by any unused argument registers:
        for (; i<Reg.args.length; i++) {
            regmap[r++] = Reg.args[i];
        }
        // And then any callerSaves registers:
        for (i=0; i<Reg.callerSaves.length; i++) {
            regmap[r++] = Reg.callerSaves[i];
        }
        // If we need any registers beyond this, we will need to wrap around
        // and start using the callee saves registers at the start of the
        // register map.  (With appropriate spilling, of course.)
    }
}

class CallFrame extends Frame {

    /** Records the number of bytes that are pushed on the stack
     *  for arguments and alignment purposes.
     */
    private int argBytes;

    /** Construct a new Frame Layout object for a function call.
     */
    public CallFrame(LocEnv env, int pushed, int argBytes) {
        super(null, env); // TODO: eliminate need for null?
        this.pushed   = pushed;
        this.argBytes = argBytes;
        argOffset     = pushed;

        // Initialize the register map, including paramBase,
        // freeBase, and free:
        regmap = new Reg[Reg.calleeSaves.length
                       + Reg.args.length
                       + Reg.results.length
                       + Reg.callerSaves.length];
        int r = 0;
        for (int i=0; i<Reg.calleeSaves.length; i++) {
            regmap[r++] = Reg.calleeSaves[i];
        }
        // FreeBase coincides with ParamBase so we can start loading
        // parameter registers.
        free = freeBase = paramBase = r;
        for (int i=0; i<Reg.args.length; i++) {
            regmap[r++] = Reg.args[i];
        }
        for (int i=0; i<Reg.results.length; i++) {
            regmap[r++] = Reg.results[i];
        }
        for (int i=0; i<Reg.callerSaves.length; i++) {
            regmap[r++] = Reg.callerSaves[i];
        }
    }

    /** Counts the number of arguments added to the frame.
     */
    int argsAdded = 0;

    /** Records offset for next stack argument.
     */
    int argOffset;

    /** Save the value in the current free register as the next
     *  argument to this function.
     */
    public void saveArg(Assembly a) {
        if (argsAdded<Reg.args.length) {
            // The first few arguments are passed in registers:
            free++;
        } else {
            // Remaining arguments are passed on the stack:
            a.emit("movq", free64(), a.indirect(-argOffset,Reg.basePointer.r64()));
            argOffset -= a.QUADSIZE;
        }
        argsAdded++;
    }

    /** Call the function, and remove argument bytes.
     */
    public void call(Assembly a, String name) {
        // On systems where alignment is important, pushed should be
        // a multiple of 16 at this point.
        a.emit("call", a.name(name));
        insertAdjust(a, -argBytes);
    }
}

/** Represents a linked list of location environments, with each entry
 *  documenting the location of a particular variable in memory.
 */
abstract class LocEnv {
    protected String name;
    protected Width  wd;
    private   LocEnv next;

    /** Default constructor.
     */
    public LocEnv(String name, Width wd, LocEnv next) {
        this.name = name;
        this.wd   = wd;
        this.next = next;
    }

    /** Return the variable name for this environment entry.
     */
    public String getName() { return name; }

    /** Return the width for this environment entry.
     */
    public Width getWidth() { return wd; }

    /** Return the tail of this environment.
     */
    public LocEnv next() { return next; }

    /** Search this environment for an occurence of a given variable.
     *  We assume that a previous static analysis has already identified
     *  and eliminated references to unbound variables.
     */
    public LocEnv find(String name) {
        for (LocEnv env=this; env!=null; env=env.next) {
            if (name.equals(env.name)) {
                return env;
            }
        }
        throw new Error("Could not find environment entry for " + name);
    }

    /** Return a string that describes the location associated with
     *  this enviroment entry.
     */
    public abstract String loc(Assembly a);
}

/** Represents an environment entry for a variable stored in a register.
 */
class RegEnv extends LocEnv {
    private Reg reg;

    /** Default constructor.
     */
    public RegEnv(String name, Width wd, LocEnv next, Reg reg) {
        super(name, wd, next);
        this.reg = reg;
    }

    /** Return a string that describes the location associated with
     *  this enviroment entry.
     */
    public String loc(Assembly a) { return wd.reg(reg); }
}

/** Represents an environment entry for a variable stored in the stack frame.
 */
class FrameEnv extends LocEnv {
    private int offset;

    /** Default constructor.
     */
    public FrameEnv(String name, Width wd, LocEnv next, int offset) {
        super(name, wd, next);
        this.offset = offset;
    }

    /** Return a string that describes the location associated with
     *  this enviroment entry.
     */
    public String loc(Assembly a) {
      return a.indirect(offset, Reg.basePointer.r64());
    }
}

/** Represents an environment entry for a global variable.
 */
class GlobalEnv extends LocEnv {

    /** Default constructor.
     */
    public GlobalEnv(String name, Width wd, LocEnv next) {
        super(name, wd, next);
    }

    /** Return a string that describes the location associated with
     *  this enviroment entry.
     */
    public String loc(Assembly a) { return a.name(name) + "(%rip)"; }
}

/** Represents a simple assembly language code generation phase.
 */
class StevieFun {
  public static final String entry    = "main";
  public static final String filename = "demo";

  public static void main(String[] args) {
    // Read and parse a source program from standard input
    new Parser(System.in);
    Defn[] program = null;
    try {
      program = Parser.Top();
    } catch (ParseException e) {
      System.out.println("Syntax Error");
      System.exit(1);
    }

    // Display the source program:
    System.out.println("Complete program is:");
    Defn.print(4, program);

    // Run static analysis on the source program:
    new StaticAnalysis().run(program);
    System.out.println("Passes static analysis!");

    // Generate some assembly code:
    Defn.compile(filename + ".s", program);
    System.out.println("Generated assembly code in " + filename + ".s");

    // Generate an executable file:
    try {
      Runtime.getRuntime()
             .exec("gcc -o " + filename + " " + filename + ".s runtime.c")
             .waitFor(); 
    } catch (Exception e) {
      System.out.println(e.toString());
      System.exit(1);
    }
    System.out.println("Executable program in " + filename);
  }
}

