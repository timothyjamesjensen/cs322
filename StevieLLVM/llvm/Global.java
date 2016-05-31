package llvm;

/** Represents a reference to a global variable.  Note that this is
 *  actually a pointer to the global variable, so if the variable
 *  itself has type T, then this variable will have type T*.
 */
public class Global extends Location {

    /** The name of this global variable.
     */
    private String name;

    /** Default constructor.
     */
    public Global(Type ty, String name) {
        super(ty);
        this.name = name;
    }

    /** Return the LLVM name for this value.
     */
    public String getName() { return "@" + name; }
}
