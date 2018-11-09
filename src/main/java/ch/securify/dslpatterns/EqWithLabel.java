package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;

/**
 * Equality between two labels
 */
public class EqWithLabel extends AbstractDSLPattern implements DatalogElem {
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

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return getStringRepresentation();
    }
}
