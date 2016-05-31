package llvm;

/** Base class for representing LLVM types.
 */
public abstract class Type {

    /** Get the name of this type.
     */
    public abstract String toString();

    /** Get the alignment for this type.
     */
    public abstract int getAlign();

    /** Get a default value for this type (to be used in global
     *  variable declarations).
     */
    public abstract String defaultValue();

    /** A class for representing LLVM primitive types.
     */
    private static class PrimType extends Type {

        /** Name of the primitive type.
         */
        private String name;

        /** Default alignment of the primitive type.
         */
        private int align;

        /** Default value of the primitive type.
         */
        private String defValue;

        /** Default constructor.
         */
        private PrimType(String name, int align, String defValue) {
            this.name = name;
            this.align = align;
            this.defValue = defValue;
        }

        /** Get the name of this type.
         */
        public String toString() { return name; }

        /** Get the alignment for this type.
         */
        public int getAlign() { return align; }

        /** Get a default value for this type (to be used in global
         *  variable declarations).
         */
        public String defaultValue() { return defValue; }
    }

    /** Represents a primitive type of Boolean values.
     */
    public static final Type i1 = new PrimType("i1", 1, "0");

    /** Represents a primitive type of 8 bit signed integer values.
     */
    public static final Type i8 = new PrimType("i8", 1, "0");

    /** Represents a primitive type of 32 bit signed integer values.
     */
    public static final Type i32 = new PrimType("i32", 4, "0");

    /** Represents a pointer type.
     */
    private static class PtrType extends Type {

        /** The type of value that is pointed to.
         */
        private Type ty;

        /** Default constructor.
         */
        private PtrType(Type ty) {
            this.ty = ty;
        }

        /** Get the name of this type.
         */
        public String toString() { return ty + "*"; }

        /** Get the alignment for this type.
         */
        public int getAlign() { return 8; }

        /** Get the type of value that this (assumed) pointer type points to.
         */
        public Type ptsTo() { return ty; }

        /** Get a default value for this type (to be used in global
         *  variable declarations).
         */
        public String defaultValue() { return "null"; }
    }

    /** Identifies the type of pointers to this type, or null if there has
     *  not been any previous reference to this pointer type.
     */
    private Type ptrType = null;

    /** Return the type of pointers to values of this type.  Initializes the
     *  ptrType field if necessary to cache the pointer type for future uses.
     */
    public Type ptr() {
        return (ptrType==null) ? ptrType=new PtrType(this) : ptrType;
    }

    /** Get the type of value that this (assumed) pointer type points to.
     */
    public Type ptsTo() {
        throw new Error("Internal error: invoked ptsTo() on a non pointer type: " + this);
    }

    /** Represents a sized array type.
     */
    private static class ArrType extends Type {

        /** The type of value that is stored in the array.
         */
        private Type ty;

        /** Default constructor.
         */
        private ArrType(Type ty) {
            this.ty = ty;
        }

        /** Get the name of this type.
         */
        public String toString() { return "{int, [0 x " + ty + "]}*"; }

        /** Get the alignment for this type.
         */
        public int getAlign() { return ty.getAlign(); }

        /** Get the type of value that this (assumed) pointer type points to.
         */
        public Type arrOf() { return ty; }

        /** Get a default value for this type (to be used in global
         *  variable declarations).
         */
        public String defaultValue() { return "null"; }
    }

    /** Identifies the type of sized arrays of this type, or null if there has
     *  not been any previous reference to this pointer type.
     */
    private Type arrType = null;

    /** Return the type of arrays of values of this type.  Initializes the
     *  arrType field if necessary to cache the array type for future uses.
     */
    public Type arr() {
        return (arrType==null) ? arrType=new ArrType(this) : arrType;
    }

    /** Get the type of values that are stored arrays of this type.
     */
    public Type arrOf() {
        throw new Error("Internal error: invoked arrOf() on a non array type");
    }
}
