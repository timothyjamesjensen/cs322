package llvm;

/** Integer less than test.
 */
public class Lt extends CmpOp {

    /** Default constructor.
     */
    public Lt(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp slt"); }
}
