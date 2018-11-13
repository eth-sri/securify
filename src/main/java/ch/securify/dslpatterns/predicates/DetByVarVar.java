package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {

        StringBuilder sb = new StringBuilder();
        sb.append("MustDepOn(");
        sb.append("_ ,"); //todo: is it really ok?
        sb.append(var1.getName());
        sb.append(" , ");
        sb.append(var2.getName());
        sb.append(")");

        return getStringRepresentation();
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>(2);
        if(VariableDC.isValidVariable(var1))
            vars.add(var1);
        if(VariableDC.isValidVariable(var2))
            vars.add(var2);
        return vars;
    }
}
