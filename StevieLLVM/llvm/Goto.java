package llvm;
import java.io.PrintWriter;

/** Branch to the start of another basic block, ending the current block.
 */
public class Goto extends Code {

    /** The block that we should branch to.
     */
    private Block b;

    /** Default constructor.
     */
    public Goto(Block b) {
        this.b = b;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  br label %" + b.label());
    }
}
