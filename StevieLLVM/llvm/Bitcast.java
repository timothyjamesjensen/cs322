package llvm;

/** Represents bitcast operators.
 */
public class Bitcast extends Rhs {

    /** The value to be recast.
     */
    private Value v;

    /** The desired result type.
     */
    private Type type;

    /** Default constructor.
     */
    public Bitcast(Value v, Type type) {
        this.v    = v;
        this.type = type;
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        return "bitcast " + v + " to " + type;
    }
}
