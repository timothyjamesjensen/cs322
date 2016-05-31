package llvm;
import java.io.PrintWriter;
import java.util.Vector;

/** Represents a function in an LLVM module.
 */
public class Function {

    /** The return type for this function.
     */
    private Type retType;

    /** The name of this function.
     */
    private String name;

    /** The list of formal parameters for this function.
     */
    private Formal[] formals;

    /** Default constructor.
     */
    public Function(Type retType, String name, Formal[] formals) {
        this.retType = retType;
        this.name = name;
        this.formals = formals;
    }

    /** Records the list of basic blocks in this function.  The last
     *  block that is added to blocks should be the entry point to
     *  the function.
     */
    private Vector<Block> blocks = new Vector<Block>();

    /** Counts the total number of registers/temporaries that have been
     *  used in this function.
     */
    private int regCount = 0;

    /** Allocate a new register/temporary for this function.
     */
    public Reg reg(Type ty) {
        return new Reg(ty, regCount++);
    }

    /** Create a new basic block in this function with the specified
     *  label and code for its body.
     */
    public Block block(String label, Code code) {
        Block l = new Block(label, code);
        blocks.add(l);
        return l;
    }

    /** Create a new basic block in this function with the specified
     *  code as its body.
     */
    public Block block(Code code) {
        return block("L" + blocks.size(), code);
    }

    /** Create an empty basic block in this function whose code will be
     *  filled in later by a call to the set method.  This method is
     *  necessary to allow the construction of flowgraphs with loops
     *  between basic blocks.
     */
    public Block block() {
        return block(null);
    }

    /** Print the code for this function on the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        // Print function header
        out.print("define ");
        out.print(retType==null ? "void" : retType.toString());
        out.print(" @X" + name + "(");
        for (int i=0; i<formals.length; i++) {
            if (i>0) {
                out.print(", ");
            }
            out.print(formals[i].getParam().toString());
        }
        out.println(") {");
  
        // Print blocks in reverse order so that entry block is first:
        for (int i=blocks.size()-1; i>=0; i--) {
            blocks.elementAt(i).print(out);
        }
        out.println("}");
        out.println();
    }
}
