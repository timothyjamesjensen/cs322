package llvm;

/** Integer division.
 */
public class Div extends BinOp {

    /** Default constructor.
     */
    public Div(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() { return toString("sdiv"); }
}
