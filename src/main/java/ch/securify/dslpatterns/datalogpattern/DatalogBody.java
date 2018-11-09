package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.AbstractDSLPattern;
import ch.securify.dslpatterns.DSLInstrOrPred;

import java.util.ArrayList;
import java.util.List;

public class DatalogBody {

    /**
     * They are single rules
     */
    List<DSLInstrOrPred> elements;

    public DatalogBody() {
        elements = new ArrayList<>();
    }

    public void addElement(DSLInstrOrPred elem) {
        elements.add(elem);
    }

    public List<DSLInstrOrPred> getElements() {
        return elements;
    }

    public String getStringRepresentation(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        if(elements.isEmpty())
            return null;

        sb.append(elements.get(0).getDatalogStringRep(analyzer));

        for(int i = 0; i < elements.size(); i++) {
            sb.append(" , ");
            sb.append(elements.get(i).getDatalogStringRep(analyzer));
        }

        return sb.toString();
    }
}
