package llvm;

/** Integer inequality test.
 */
public class Neq extends CmpOp {

    /** Default constructor.
     */
    public Neq(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("icmp ne");  }
}
