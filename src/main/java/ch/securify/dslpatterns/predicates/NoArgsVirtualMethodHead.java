package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.HashSet;
import java.util.Set;

public class NoArgsVirtualMethodHead extends AbstractPredicate {

    private DSLLabel label;

    public NoArgsVirtualMethodHead(DSLLabel label) {
        this.label = label;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("noArgsVirtualMethodHead(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>(1);
        labels.add(label);
        return labels;
    }
}
