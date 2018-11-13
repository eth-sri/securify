package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The must follow DSL predicate
 */
public class MustFollow extends AbstractPredicate {
    private DSLLabel l1, l2;

    public MustFollow(DSLLabel l1, DSLLabel l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    public DSLLabel getL1() {
        return l1;
    }

    public DSLLabel getL2() {
        return l2;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("MustFollow(");
        sb.append(l1.getName());
        sb.append(" , ");
        sb.append(l2.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        //using must precede instead of must follow seems ok since it is never quantified over
        StringBuilder sb = new StringBuilder();
        sb.append("mustPrecede(");
        sb.append(l1.getName());
        sb.append(" , ");
        sb.append(l2.getName());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>(2);
        labels.add(l1);
        labels.add(l2);

        return labels;
    }
}
