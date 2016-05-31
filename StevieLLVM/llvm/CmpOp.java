package llvm;

/** Comparison operations return a boolean as the result of comparing
 *  two values of the same type.
 */
public abstract class CmpOp extends BinOp {

    /** Default constructor.
     */
    public CmpOp(Type ty, Value l, Value r) {
        super(ty, l, r);
    }

    /** Return the type of the results that are produced by
     *  comparison operations.
     */
    public Type resultType() { return Type.i1; }
}
