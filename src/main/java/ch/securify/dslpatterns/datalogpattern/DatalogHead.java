package ch.securify.dslpatterns.datalogpattern;

import ch.securify.dslpatterns.util.DSLLabel;

import java.awt.*;

/**
 * Represents the head of the datalog rule
 */
public class DatalogHead {
    private String name;
    private DSLLabel label;

    public DatalogHead(String name, DSLLabel label) {
        this.name = name;
        this.label = label;
    }

    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append("(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }

    public String getName() {
        return name;
    }

    public DSLLabel getLabel() {
        return label;
    }
}
