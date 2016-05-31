package llvm;
import java.io.PrintWriter;

/** A conditional branch to one of two basic blocks, the choice being
 *  determined by a specified value.
 */
public class Cond extends Code {

    /** The value to use in making the branch decision.
     */
    private Value v;

    /** The block to branch to if the value is true.
     */
    private Block ifTrue;

    /** The block to branch to if the value is false.
     */
    private Block ifFalse;

    /** Default constructor.
     */
    public Cond(Value v, Block ifTrue, Block ifFalse) {
        this.v = v;
        this.ifTrue = ifTrue;
        this.ifFalse = ifFalse;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  br i1 " + v.getName()
                               + ", label %" + ifTrue.label()
                               + ", label %" + ifFalse.label());
    }
}
