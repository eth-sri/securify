package ch.securify.dslpatterns.datalogpattern;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the head of the datalog rule
 */
public class DatalogHead {
    private String name;
    private List<DSLLabel> labels;
    private List<Variable> vars;

    public DatalogHead(String name, DSLLabel label) {
        this.name = name;
        this.labels = new ArrayList<>(1);
        this.labels.add(label);
        this.vars = new ArrayList<>(0);
    }

    public DatalogHead(String name, List<DSLLabel> labels, List<Variable> vars) {
        this.name = name;
        this.labels = labels;
        this.vars = vars;
    }

    public String getStringRepresentation() {

        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append("(");

        if(!vars.isEmpty()) {
            sb.append(vars.get(0).getName());
            for(int i = 1; i < vars.size(); i++) {
                sb.append(", ");
                sb.append(vars.get(i).getName());
            }
        }

        if(!labels.isEmpty()) {
            if(!vars.isEmpty())
                sb.append(" , ");
            sb.append(labels.get(0).getName());
            for(int i = 1; i < labels.size(); i++) {
                sb.append(", ");
                sb.append(labels.get(i).getName());
            }
        }

        sb.append(")");

        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public DSLLabel getLabel() {
        return labels.get(0);
    }

    public List<DSLLabel> getLabels() {
        return labels;
    }

    public List<Variable> getVars() {
        return vars;
    }
}
