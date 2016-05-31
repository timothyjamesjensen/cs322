package llvm;

/** Integer multiplication.
 */
public class Mul extends BinOp {

    /** Default constructor.
     */
    public Mul(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("mul");  }
}
