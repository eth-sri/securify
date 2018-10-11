package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.DSLLabel;

/**
 * Equality between two labels
 */
public class EqWithLabel extends AbstractDSLPattern {
    private DSLLabel l1, l2;

    public EqWithLabel(DSLLabel l1, DSLLabel l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    /**
     * @return a string description of the equality
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(l1.getName());
        sb.append(" = ");
        sb.append(l2.getName());
        return sb.toString();
    }
}
