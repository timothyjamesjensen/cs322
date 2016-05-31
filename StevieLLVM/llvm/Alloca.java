package llvm;

/** Allocate space for a variable on the stack.
 */
public class Alloca extends Rhs {

    /** The type of value for which storage is required.
     */
    private Type ty;

    /** Default constructor.
     */
    public Alloca(Type ty) {
        this.ty = ty;
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        return "alloca " + ty /* + ", align " + ty.getAlign()*/;
    }
}
