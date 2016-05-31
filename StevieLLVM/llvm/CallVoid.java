package llvm;
import java.io.PrintWriter;

/** Call a void function and then continue with the rest of the code
 *  in this block.
 */
public class CallVoid extends Code {

    /** The name of the function to call.
     */
    private String name;

    /** The values that will be passed as arguments.
     */
    private Value[] args;

    /** Points to the rest of the code in this basic block.
     */
    private Code next;

    /** Default constructor.
     */
    public CallVoid(String name, Value[] args, Code next) {
        this.name = name;
        this.args = args;
        this.next = next;
    }

    /** Print out this code sequence to the specified PrintWriter.
     */
    public void print(PrintWriter out) {
        out.println("  call void @X" + name + Value.toString(args));
        next.print(out);
    }
}
