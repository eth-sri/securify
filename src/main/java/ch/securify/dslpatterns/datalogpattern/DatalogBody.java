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

    public DatalogBody(DatalogElem rule) {
        elements = new ArrayList<>();
        elements.add(rule);
    }

    public void addElement(DatalogElem elem) {
        elements.add(elem);
    }
    public void addElementInFront(DatalogElem elem) {
        elements.add(0, elem);
    }

    public void addAllElements(List<DatalogElem> elems) {
        elements.addAll(elems);
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

    /**
     * @return a copy of the object, only a new array is created, the elements are the same.
     * The two arrays will be different but point to the same elements (saves memory)
     */
    public DatalogBody duplicateUpUntilList() {
        DatalogBody copy = new DatalogBody();
        copy.addAllElements(elements);
        return copy;
    }

    /**
     * Collapses two bodies into one
     * @param one the first body
     * @param two the second one
     * @return the body containing the elements from both
     */
    public static DatalogBody collapseTwo(DatalogBody one, DatalogBody two) {
        DatalogBody newBody = new DatalogBody();
        newBody.addAllElements(one.getElements());
        newBody.addAllElements(two.getElements());

        return newBody;
    }
}
