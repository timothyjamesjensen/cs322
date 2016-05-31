package llvm;

/** Represents a register/temporary that is used in LLVM calculations.
 */
public class Reg extends Lhs {

    /** A number that distinguishes this particular register
     *  from any other register in the same function.
     */
    private int num;

    /** Default constructor.
     */
    public Reg(Type ty, int num) {
        super(ty);
        this.num = num;
    
        str = "%t" + num;
    }
}
