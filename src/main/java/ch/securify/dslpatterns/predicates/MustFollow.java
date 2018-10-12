package ch.securify.dslpatterns.predicates;

import ch.securify.dslpatterns.instructions.DSLLabel;

/**
 * The must follow DSL predicate
 */
public class MustFollow extends AbstractPredicate {
    DSLLabel l1, l2;

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
}
