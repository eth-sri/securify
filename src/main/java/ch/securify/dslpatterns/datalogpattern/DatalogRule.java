package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.util.DSLLabel;

public class DatalogRule {
    private DatalogHead head;
    private DatalogBody body;

    public DatalogRule(DatalogHead head, DatalogBody body) {
        this.head = head;
        this.body = body;
    }

    public DatalogRule(String ruleName, DSLLabel label) {
        this.head = new DatalogHead(ruleName, label);
        this.body = new DatalogBody();
    }

    public DatalogHead getHead() {
        return head;
    }

    public DatalogBody getBody() {
        return body;
    }

    public String getStingRepresentation(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        sb.append(head.getStringRepresentation());
        sb.append(" :- ");
        sb.append(body.getStringRepresentation(analyzer));
        sb.append(".");

        return sb.toString();
    }

    /**
     * @return a copy of the object, only a new body with a new array of elements is created, the elements are the same.
     * The two arrays will be different but point to the same elements (saves memory).
     * The head is the same in the new object
     */
    public DatalogRule duplicateUpUntilBodyList() {
        return new DatalogRule(head, body.duplicateUpUntilList());
    }
}
