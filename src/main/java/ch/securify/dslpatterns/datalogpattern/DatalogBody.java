package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;

import java.util.ArrayList;
import java.util.List;

public class DatalogBody {

    /**
     * They are single rules
     */
    List<DatalogElem> elements;

    public DatalogBody() {
        elements = new ArrayList<>();
    }

    public void addElement(DatalogElem elem) {
        elements.add(elem);
    }

    public List<DatalogElem> getElements() {
        return elements;
    }

    public String getStringRepresentation(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        if(elements.isEmpty())
            return null;

        sb.append(elements.get(0).getDatalogStringRep(analyzer));

        for(int i = 1; i < elements.size(); i++) {
            sb.append(" , ");
            sb.append(elements.get(i).getDatalogStringRep(analyzer));
        }

        return sb.toString();
    }
}
