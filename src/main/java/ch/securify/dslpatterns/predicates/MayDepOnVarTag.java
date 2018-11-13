package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;

import java.util.ArrayList;
import java.util.List;

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
        sb.append("mayDepOn(");
        sb.append(v.getName());
        sb.append(" , ");
        sb.append(tag.getSimpleName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public List<Variable> getVariables() {
        List<Variable> vars = new ArrayList<>(1);
        vars.add(v);
        return vars;
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        sb.append("mayDepOn(");
        sb.append(v.getName());
        sb.append(" , ");
        sb.append(analyzer.getCode(tag));
        sb.append(")");

        return sb.toString();
    }

    public Variable getVariable() {
        return v;
    }

    public Class getTag() {
        return tag;
    }
}
