package llvm;
import java.io.PrintWriter;

/** Return from the current function with a specified return value,
 *  ending the current basic block.
 */
public class Ret extends Code {

    /** The value to be returned.
     */
    private Value v;

    /** Default constructor.
     */
    public Ret(Value v) {
        this.v = v;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  ret " + v);
     }
}
