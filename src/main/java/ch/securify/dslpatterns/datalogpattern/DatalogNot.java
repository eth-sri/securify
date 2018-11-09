package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;

public class DatalogNot implements DatalogElem {
    DatalogElem negatedElem;

    public DatalogNot(DatalogElem negatedElem) {
        this.negatedElem = negatedElem;
    }

    public DatalogElem getNegatedElem() {
        return negatedElem;
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return "!" + negatedElem.getDatalogStringRep(analyzer);
    }
}
