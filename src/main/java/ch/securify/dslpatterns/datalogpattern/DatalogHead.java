package ch.securify.dslpatterns.datalogpattern;

import java.awt.*;

/**
 * Represents the head of the datalog rule
 */
public class DatalogHead {
    private String name;
    private Label label;

    public DatalogHead(String name, Label label) {
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

    public Label getLabel() {
        return label;
    }
}
