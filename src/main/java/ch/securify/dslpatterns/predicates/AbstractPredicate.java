package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.AbstractDSLPattern;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class that represents predicates expressed in the DSL language,
 * like {@link Follow}, {@link MustFollow}...
 */
public abstract class AbstractPredicate extends AbstractDSLPattern implements DatalogElem {

    public String getStringRepresentation() {
        return "AbstractPredicate";
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return getStringRepresentation();
    }

    @Override
    public List<DSLLabel> getLabels() {
        return new ArrayList<>();
    }

    @Override
    public List<Variable> getVariables() {
        return new ArrayList<>();
    }
}
