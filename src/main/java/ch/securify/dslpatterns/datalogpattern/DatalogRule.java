package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;

public class DatalogRule {
    private DatalogHead head;
    private DatalogBody body;

    public DatalogRule(DatalogHead head, DatalogBody body) {
        this.head = head;
        this.body = body;
    }

    public DatalogRule(DatalogHead head) {
        this.head = head;
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
}
