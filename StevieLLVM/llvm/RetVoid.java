package llvm;
import java.io.PrintWriter;

/** Return from the current function without a return value, ending
 *  the current basic block.
 */
public class RetVoid extends Code {

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  ret void");
     }
}
