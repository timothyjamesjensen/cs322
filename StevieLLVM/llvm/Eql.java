package llvm;

/** Integer equality test.
 */
public class Eql extends CmpOp {

    /** Default constructor.
     */
    public Eql(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp eq");  }
}
