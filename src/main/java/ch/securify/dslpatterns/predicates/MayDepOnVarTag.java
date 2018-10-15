package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;

/**
 * The MayDepOn predicate with input a variable and a tag represented by a class
 */
public class MayDepOnVarTag extends AbstractPredicate {
    private Variable v;
    private Class tag;

    public MayDepOnVarTag(Variable v, Class tag) {
        this.v = v;
        this.tag = tag;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("MayDepOn(");
        sb.append(v.getName());
        sb.append(" , ");
        sb.append(tag.getSimpleName());
        sb.append(")");

        return sb.toString();
    }
}
