package llvm;

/** Represents a getelementptr instruction.
 */
public class Getelementptr extends Rhs {

    /** Base pointer:
     */
    private Value ptr;

    /** The index from the pointer:
     */
    private Value[] offsets;

    /** Default constructor.
     */
    public Getelementptr(Value ptr, Value[] offsets) {
        this.ptr     = ptr;
        this.offsets = offsets;
    }

    public Getelementptr(Value ptr, Value o1) {
        this(ptr, new Value[] { o1 });
    }

    public Getelementptr(Value ptr, Value o1, Value o2) {
        this(ptr, new Value[] { o1, o2 });
    }

    public Getelementptr(Value ptr, Value o1, Value o2, Value o3) {
        this(ptr, new Value[] { o1, o2, o3 });
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder("getelementptr inbounds ");
        buf.append(ptr.toString());
        for (int i=0; i<offsets.length; i++) {
          buf.append(", ");
          buf.append(offsets[i].toString());
        }
        return buf.toString();
    }
}
