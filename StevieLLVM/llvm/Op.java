package llvm;
import java.io.PrintWriter;

/** A basic operation: perform the specified right hand operation,
 *  capture the result in the specified location, and then continue
 *  with the rest of the code in the block.
 */
public class Op extends Code {

    /** The left hand side where the result will be saved.
     */
    private Lhs lhs;

    /** The right hand side operation.
     */
    private Rhs rhs;

    /** Points to the rest of the code in this basic block.
     */
    private Code next;

    /** Default constructor.
     */
    public Op(Lhs lhs, Rhs rhs, Code next) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.next = next;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  " + lhs.getName() + " = " + rhs);
        next.print(out);
    }
}
