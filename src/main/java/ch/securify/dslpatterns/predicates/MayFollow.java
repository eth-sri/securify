package ch.securify.dslpatterns.predicates;

import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The may follow DSL predicate
 */
public class MayFollow extends AbstractPredicate {
    private DSLLabel l1, l2;

    public MayFollow(DSLLabel l1, DSLLabel l2) {
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
        sb.append("mayFollow(");
        sb.append(l1.getName());
        sb.append(" , ");
        sb.append(l2.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>(2);
        if(DSLLabel.isValidLabel(l1))
            labels.add(l1);
        if(DSLLabel.isValidLabel(l2))
            labels.add(l2);

        return labels;
    }
}
