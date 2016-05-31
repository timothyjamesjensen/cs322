package llvm;
import java.io.PrintWriter;

/** Store a value at a specified address and then continue with the
 *  rest of the code in this block.
 */
public class Store extends Code {

    /** The value to be stored.
     */
    Value v;

    /** The location where it will be stored.
     */
    Value addr;

    /** Points to the rest of the code in this basic block.
     */
    private Code next;

    /** Default constructor.
     */
    public Store(Value v, Value addr, Code next) {
        this.v = v;
        this.addr = addr;
        this.next = next;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  store " + v + ", " + addr
                        /*+ ", align " + v.getType().getAlign()*/);
        next.print(out);
    }
}
