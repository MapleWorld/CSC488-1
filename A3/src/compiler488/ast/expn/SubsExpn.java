package compiler488.ast.expn;

/**
 * References to an array element variable
 *
 */
public class SubsExpn extends VarRefExpn {
    private String ident;
    private Expn subscript1;	 // first subscript
    private Expn subscript2 = null;	// second subscript (if any)

    public SubsExpn(String ident, Expn subscript1, Expn subscript2) {
        this.ident = ident;
        this.subscript1 = subscript1;
        this.subscript2 = subscript2;
    }

    public SubsExpn(String ident, Expn subscript1) {
        this(ident, subscript1, null);
    }

    /** Returns a string that represents the array subscript. */
    @Override
    public String toString() {
        return (ident + "[" + subscript1 +
                ( subscript2 != null ? "," + subscript2 : "" )
                + "]");
    }

    public String getIdent() {
        return ident;
    }

    public void setIdent(String ident) {
        this.ident = ident;
    }

    public Expn getSubscript1() {
        return subscript1;
    }

    public void setSubscript1(Expn subscript1) {
        this.subscript1 = subscript1;
    }

    public Expn getSubscript2() {
        return subscript2;
    }

    public void setSubscript2(Expn subscript2) {
        this.subscript2 = subscript2;
    }
}
