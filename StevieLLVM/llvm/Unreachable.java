package llvm;
import java.io.PrintWriter;

/** Indicates that the current code is unreachable; should only be
 *  the current basic block.
 */
public class Unreachable extends Code {

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  unreachable");
    }
}
