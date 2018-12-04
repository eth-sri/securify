package ch.securify.dslpatterns.datalogpattern;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.List;

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

    public String getDeclaration() {
        StringBuilder sb = new StringBuilder();

        //example of declaration
        //.decl jump		(l1: Label, l2: Label, l3: Label) output
        sb.append(".decl ");
        sb.append(head.getName());
        sb.append("( ");

        List<Variable> vars = head.getVars();

        if(!vars.isEmpty()) {
            sb.append(vars.get(0).getName());
            sb.append(": Var");
            for(int i = 1; i < vars.size(); i++) {
                sb.append(", ");
                sb.append(vars.get(i).getName());
                sb.append(": Var");
            }
        }

        List<DSLLabel> labels = head.getLabels();

        if(!labels.isEmpty()) {
            if(!vars.isEmpty())
                sb.append(" , ");
            sb.append(labels.get(0).getName());
            sb.append(": Label");
            for(int i = 1; i < labels.size(); i++) {
                sb.append(", ");
                sb.append(labels.get(i).getName());
                sb.append(": Label");
            }
        }

        sb.append(") output");


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
