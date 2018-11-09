package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.AbstractDSLPattern;

/**
 * The detBy predicate with input a variable and a tag represented by a class
 */
public class DetByVarTag extends AbstractPredicate {
    private Variable var;
    private Class tag;

    public DetByVarTag(Variable var, Class tag) {
        this.var = var;
        this.tag = tag;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("DetBy(");
        sb.append(var.getName());
        sb.append(" , ");
        sb.append(tag.getSimpleName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        //todo
        return getStringRepresentation();
    }
}
