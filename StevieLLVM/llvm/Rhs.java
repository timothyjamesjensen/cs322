package llvm;

/** A base class for representing right hand sides of LLVM operations, lhs = rhs.
 */
public abstract class Rhs {

    /** Generate a printable string for this instruction.
     */
    public abstract String toString();
}
