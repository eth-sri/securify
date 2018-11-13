package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Normal logic implication
 */
public class Implies extends AbstractDSLPattern {

    private AbstractDSLPattern lhs, rhs;

    public Implies(AbstractDSLPattern lhs, AbstractDSLPattern rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public AbstractDSLPattern getLhs() {
        return lhs;
    }

    public AbstractDSLPattern getRhs() {
        return rhs;
    }

    /**
     * @return a string description of the Implies
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.getStringRepresentation());
        sb.append(" => ");
        sb.append(rhs.getStringRepresentation());
        return sb.toString();
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>();
        vars.addAll(lhs.getVariables());
        vars.addAll(rhs.getVariables());

        return vars;
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>();
        labels.addAll(lhs.getLabels());
        labels.addAll(rhs.getLabels());

        return labels;
    }
}
