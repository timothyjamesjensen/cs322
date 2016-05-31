package llvm;

/** Represents a function call.
 */
public class Call extends Rhs {

    /** The type of value that will be returned by this call.
     */
    private Type type;

    /** The name of the function to call.
     */
    private String name;

    /** The arguments to this call.
     */
    private Value[] args;

    /** Default constructor.
     */
    public Call(Type type, String name, Value[] args) {
        this.type = type;
        this.name = name;
        this.args = args;
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        return "call " + type + " @X" + name + Value.toString(args);
    }
}
