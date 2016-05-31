package llvm;

/** Base class for LLVM left hand sides (i.e., variables that can be
 *  used to hold the result of an LLVM operation.
 */
public abstract class Lhs extends Location {

    /** Default constructor.
     */
    public Lhs(Type ty) {
        super(ty);
    }

    /** Caches a string representation for this left hand side in LLVM code.
     */
    protected String str;

    /** Return the LLVM name for this value.
     */
    public String getName() { return str; }
}
