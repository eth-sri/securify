package ch.securify.dslpatterns.predicates;

import ch.securify.dslpatterns.util.DSLLabel;

/**
 * The MayDepOn predicate with input a label and a tag represented by a class
 */
public class MayDepOnLabelTag extends AbstractPredicate {
    private DSLLabel l;
    private Class tag;

    public MayDepOnLabelTag(DSLLabel l, Class tag) {
        this.l = l;
        this.tag = tag;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("MayDepOn(");
        sb.append(l.getName());
        sb.append(" , ");
        sb.append(tag.getSimpleName());
        sb.append(")");

        return sb.toString();
    }
}
