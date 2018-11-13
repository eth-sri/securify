package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>(1);
        if(VariableDC.isValidVariable(x))
            vars.add(x);
        return vars;
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        //todo
        return getStringRepresentation();
    }


}
