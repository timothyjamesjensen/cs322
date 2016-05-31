package llvm;

/** Base class for values (i.e., operands) in LLVM instructions.
 */
public abstract class Value {

    /** Return the LLVM type of this value.
     */
    public abstract Type getType();

    /** Return the LLVM name for this value.
     */
    public abstract String getName();

    /** Return a string representation for this value that includes both
     *  the type and the name with a single space between them.
     */
    public String toString() {
        return getType() + " " + getName();
    }

    /** Generate a string that lists a collection of LLVM values separated
     *  as necessary by commas.  This function is intended for use in
     *  displaying the values of the arguments in a Call or CallVoid.
     */
    public static String toString(Value[] args) {
        StringBuffer buf = new StringBuffer("(");
        for (int i=0; i<args.length; i++) {
            if (i>0) {
                buf.append(", ");
            }
            buf.append(args[i].toString());
        }
        buf.append(")");
        return buf.toString();
    }
}
