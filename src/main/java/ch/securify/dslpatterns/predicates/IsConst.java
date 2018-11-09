package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;

/**
 * The IsConst predicate, takes as input a variable
 */
public class IsConst extends AbstractPredicate {
    Variable x;
    public IsConst(Variable x) {
        this.x = x;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("IsConst(");
        sb.append(x.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        //todo
        return getStringRepresentation();
    }
}
