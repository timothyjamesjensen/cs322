package llvm;

/** Integer addition.
 */
public class Add extends BinOp {

    /** Default constructor.
     */
    public Add(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("add");  }
}
