package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.AbstractDSLPattern;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.List;

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
    public List<Variable> getVariables() {
        List<Variable> vars = new ArrayList<>(1);
        vars.add(var);
        return vars;
    }

    @Override
    public List<DSLLabel> getLabels() {
        return new ArrayList<>();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();
        sb.append("MustDepOn(");
        sb.append("_ ,"); //todo: is it really ok? In the paper has only 2 elements, here 3, we are ignoring the label
        sb.append(var.getName());
        sb.append(" , ");
        sb.append(analyzer.getCode(tag));
        sb.append(")");

        return getStringRepresentation();
    }
}
