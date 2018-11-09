package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.AbstractDSLPattern;
import ch.securify.dslpatterns.DSLInstrOrPred;

/**
 * Abstract class that represents predicates expressed in the DSL language,
 * like {@link Follow}, {@link MustFollow}...
 */
public abstract class AbstractPredicate extends DSLInstrOrPred {

    public String getStringRepresentation() {
        return "AbstractPredicate";
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return getStringRepresentation();
    }
}
