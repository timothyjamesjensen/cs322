package llvm;

/** Integer subtraction.
 */
public class Sub extends BinOp {

    /** Default constructor.
     */
    public Sub(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("sub");  }
}
