package llvm;

/** Represents binary operators, where op is add ty, fadd ty, ....
 */
public abstract class BinOp extends Rhs {

    /** The type of the result.
     */
    protected Type ty;

    /** The left operand.
     */
    private Value l;

    /** The right operand.
     */
    private Value r;

    /** Default constructor.
     */
    public BinOp(Type ty, Value l, Value r) {
        this.ty = ty;
        this.l = l;
        this.r = r;
    }

    /** Return the result type of this operation, which by default
     *  is the same as the type of the arguments.
     */
    public Type resultType() {
      return ty;
    }

    /** A single method interface whose instances can be used as
     *  helpers to build BinOp objects.
     */
    public interface Maker {
        public BinOp make(Type ty, Value l, Value r);
    }

    /** Generate a string for this right hand side using the given string
     *  as the operation name.
     */
    public String toString(String op) {
        return op + " " + ty + " " + l.getName() + ", " + r.getName();
    }
}
