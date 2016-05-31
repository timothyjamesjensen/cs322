package llvm;

/** Represents an LLVM formal parameter.  The formal parameter itself
 *  holds a pointer to a stack frame location where the variable is
 *  stored, but each formal has an associated parameter, param, which
 *  holds the argument value that is passed in at the start of the
 *  function.
 */
public class Formal extends Location {

    /** The name of this formal parameter.
     */
    private String name;

    /** Default constructor.
     */
    public Formal(Type ty, String name) {
        super(ty);
        this.name = name;
    
        param   = new Local(ty.ptsTo(), name + ".param");
    }

    /** The name of the associated function parameter.  (As opposed to
     *  the pointer to the stack frame location where this variable is
     *  stored during the function body.
     */
    private Local param;

    /** Return the location for the associated function parameter.
     */
    public Local getParam() {
        return param;
    }

    /** Return the LLVM name for this value.
     */
    public String getName() { return "%" + name; }
}
