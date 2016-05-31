package llvm;
import java.io.PrintWriter;

/** Represents a basic block in an LLVM function.
 */
public class Block {

    /** The label for the entry to this basic block.
     */
    private String label;

    /** The code for the body of this basic block.
     */
    private Code code;

    /** Default constructor.
     */
    public Block(String label, Code code) {
        this.label = label;
        this.code = code;
    }

    /** Set the code associated with this basic block.
     */
    public void set(Code code) {
        this.code = code;
    }

    /** Return the entry label for this basic block.
     */
    public String label() {
        return label;
    }

    /** Return the code for this basic block.
     */
    public Code code() {
        return code;
    }

    /** Print the code for this basic block on the specified PrintWriter.
     */
    void print(PrintWriter out) {
        out.println(label + ":");
        code.print(out);
    }
}
