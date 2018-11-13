package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.List;

/**
 * Normal logic negation
 */
public class Not extends AbstractDSLPattern {

    private AbstractDSLPattern negatedPattern;

    public Not(AbstractDSLPattern negatedPattern) {
        this.negatedPattern = negatedPattern;
    }

    /**
     * @return a string description of the negated pattern
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("!(");
        sb.append(negatedPattern.getStringRepresentation());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public List<Variable> getVariables() {
        return negatedPattern.getVariables();
    }

    @Override
    public List<DSLLabel> getLabels() {
        return negatedPattern.getLabels();
    }

    public AbstractDSLPattern getNegatedPattern() {
        return negatedPattern;
    }
}
