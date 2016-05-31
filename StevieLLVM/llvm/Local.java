package llvm;

/** Represents a local variable, introduced in a local variable declaration
 *  or as a formal parameter to a function.
 */
public class Local extends Lhs {

    /** The name of this local variable.
     */
    private String name;

    /** Default constructor.
     */
    public Local(Type ty, String name) {
        super(ty);
        this.name = name;
    
        str = "%" + name;
    }
}
