package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * Equality between two variables
 */
public class Eq extends AbstractDSLPattern implements DatalogElem {
    private Variable v1, v2;

    public Eq(Variable v1, Variable v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    /**
     * @return a string description of the equality
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(v1.getName());
        sb.append(" = ");
        sb.append(v2.getName());
        return sb.toString();
    }

    @Override
    public List<Variable> getVariables() {
        List<Variable> vars = new ArrayList<>(2);
        vars.add(v1);
        vars.add(v2);

        return vars;
    }

    @Override
    public List<DSLLabel> getLabels() {
        return new ArrayList<>();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return getStringRepresentation();
    }
}
