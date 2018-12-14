package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.HashSet;
import java.util.Set;

public class GreaterThanComparison extends AbstractDSLPattern implements DatalogElem {

    private Variable v1;
    private Integer n;

    public GreaterThanComparison(Variable v1, Integer n) {
        this.v1 = v1;
        this.n = n;
    }

    /**
     * @return a string description of the equality
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(v1.getName());
        sb.append(" > ");
        sb.append(n);
        return sb.toString();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();
        sb.append(v1.getName());
        sb.append(" > ");
        sb.append(analyzer.getCode(n));
        return sb.toString();
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>(1);
        vars.add(v1);

        return vars;
    }

    @Override
    public Set<DSLLabel> getLabels() {
        return new HashSet<>();
    }
}
