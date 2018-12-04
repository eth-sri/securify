package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.HashSet;
import java.util.Set;

/**
 * Just the father of both {@link IsConst} and {@link IsArg}
 */
public abstract class AbstractVariablePredicate extends AbstractPredicate {

    private Variable x;
    public AbstractVariablePredicate(Variable x) {
        this.x = x;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
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
        return getStringRepresentation();
    }

    protected abstract String getName();
}
