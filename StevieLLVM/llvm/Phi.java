package llvm;

/** Represents a phi function.
 */
public class Phi extends Rhs {

    /** List of predecessor blocks.
     */
    private Block[] blocks;

    /** List of values passed in from predecessor blocks.
     *  (Length and order must match blocks array.)
     */
    private Value[] values;

    /** Default constructor.
     */
    public Phi(Block[] blocks, Value[] values) {
        this.blocks = blocks;
        this.values = values;
    }

    /** Special case constructor for use when there are
     *  precisely two predecessor blocks.
     */
    public Phi(Block b1, Value v1, Block b2, Value v2) {
        this(new Block[] { b1, b2 }, new Value[] { v1, v2 });
    }

    /** Generate a printable string for this instruction.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder("phi ");
        buf.append(values[0].getType().toString());
        buf.append(" ");
        for (int i=0; i<blocks.length; i++) {
            if (i>0) {
                buf.append(", ");
            }
            buf.append("[ ");
            buf.append(values[i].getName());
            buf.append(", %");
            buf.append(blocks[i].label());
            buf.append(" ]");
        }
        return buf.toString();
    }
}
