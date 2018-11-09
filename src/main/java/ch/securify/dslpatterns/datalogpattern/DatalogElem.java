package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.AbstractDSLPattern;

/**
 * Just a superclass for both instructions and predicates
 */
public interface DatalogElem {

    public String getDatalogStringRep(DSLAnalysis analyzer);

}