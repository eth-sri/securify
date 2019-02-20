package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.HashSet;
import java.util.Set;

/**
 * IsArg predicated, checks if a variable is the argument of a function,
 * is used to express the "arg" tag present in the paper
 */
public class IsArg extends AbstractPredicate {
    Variable x;
    DSLLabel label;

    public IsArg(Variable x, DSLLabel label) {
        this.x = x;
        this.label = label;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("isArg(");
        sb.append(x.getName());
        sb.append(" , ");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>(1);
        labels.add(label);
        return  labels;
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>(1);
        vars.add(x);
        return  vars;
    }
}