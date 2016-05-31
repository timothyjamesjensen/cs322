package llvm;

/** Represents an LLVM integer constant value.
 */
public class IntVal extends Value {

    /** The numeric value associated with this IntVal.
     */
    private int num;

    /** Default constructor.
     */
    public IntVal(int num) {
        this.num = num;
    }

    /** The integer constant with all bits zero.
     */
    public static final IntVal ZERO = new IntVal(0);

    /** The integer constant with all bits one.
     */
    public static final IntVal ONES = new IntVal(~0);

    /** Return the LLVM type of this value.
     */
    public Type getType() { return Type.i32; }

    /** Return the LLVM name for this value.
     */
    public String getName() { return Integer.toString(num); }
}
