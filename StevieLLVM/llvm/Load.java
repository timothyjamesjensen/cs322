package llvm;

/** Load a value from memory.
 */
public class Load extends Rhs {

    /** A location from which a value will be loaded.
     */
    Value v;

    /** Default constructor.
     */
    public Load(Value v) {
        this.v = v;
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        return "load " + v /*+ ", align " + v.getType().ptsTo().getAlign()*/;
    }
}
