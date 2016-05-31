package llvm;

/** Represents an LLVM boolean constant value.
 */
public class BoolVal extends Value {

    /** The boolean value associated with this BoolVal.
     */
    private boolean bool;

    /** Default constructor.
     */
    public BoolVal(boolean bool) {
        this.bool = bool;
    }

    /** The boolean constant true.
     */
    public static final BoolVal TRUE = new BoolVal(true);

    /** The boolean constant false.
     */
    public static final BoolVal FALSE = new BoolVal(false);

    /** Return the LLVM type of this value.
     */
    public Type getType() { return Type.i1; }

    /** Return the LLVM name for this value.
     */
    public String getName() { return bool ? "1" : "0"; }
}
