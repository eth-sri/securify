package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;

/**
 * Equality between two variables
 */
public class Eq extends AbstractDSLPattern {
    private Variable v1, v2;

    public Eq(Variable v1, Variable v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    /**
     * @return a string description of the equality
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(v1.getName());
        sb.append(" = ");
        sb.append(v2.getName());
        return sb.toString();
    }
}
