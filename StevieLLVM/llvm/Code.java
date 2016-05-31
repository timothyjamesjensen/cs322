package llvm;
import java.io.PrintWriter;

/** Represents a sequence of code in an LLVM basic block.
 */
public abstract class Code {

    /** Print out this code sequence to the specified PrintWriter.
     */
    public abstract void print(PrintWriter out);
}
