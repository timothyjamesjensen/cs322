package llvm;

/** Integer greater than or equal test.
 */
public class Gte extends CmpOp {

    /** Default constructor.
     */
    public Gte(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp sge"); }
}
