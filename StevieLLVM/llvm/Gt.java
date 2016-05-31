package llvm;

/** Integer greater than test.
 */
public class Gt extends CmpOp {

    /** Default constructor.
     */
    public Gt(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp sgt"); }
}
