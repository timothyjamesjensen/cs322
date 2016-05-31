package llvm;

/** Represents an LLVM value that specifies a location in memory or in
 *  a register/temporary.
 */
public abstract class Location extends Value {

    /** The type of value stored in this location.
     */
    protected Type ty;

    /** Default constructor.
     */
    public Location(Type ty) {
        this.ty = ty;
    }

    /** Return the LLVM type of this value.
     */
    public Type getType() { return ty; }
}
