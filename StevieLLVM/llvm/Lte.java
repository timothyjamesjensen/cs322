package llvm;

/** Integer less than or equal test..
 */
public class Lte extends CmpOp {

    /** Default constructor.
     */
    public Lte(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp sle"); }
}
