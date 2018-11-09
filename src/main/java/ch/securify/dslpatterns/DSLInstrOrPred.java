package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;

/**
 * Just a superclass for both instructions and predicates
 */
public abstract class DSLInstrOrPred extends AbstractDSLPattern {

    public abstract String getDatalogStringRep(DSLAnalysis analyzer);

}
