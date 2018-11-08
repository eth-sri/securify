package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.AbstractDSLPattern;

/**
 * The detBy predicate with input two variable
 */
public class DetByVarVar extends AbstractPredicate {
    private Variable var1, var2;

    public DetByVarVar(Variable var1, Variable var2) {
        this.var1 = var1;
        this.var2 = var2;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("DetBy(");
        sb.append(var1.getName());
        sb.append(" , ");
        sb.append(var2.getName());
        sb.append(")");

        return sb.toString();
    }
}
