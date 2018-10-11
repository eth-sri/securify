package ch.securify.dslpatterns.instructions;

public class AbstractDSLInstruction {
    protected DSLLabel label = null;

    public AbstractDSLInstruction(DSLLabel label) {
        this.label = label;
    }

    public DSLLabel getLabel() {
        return label;
    }

    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("AbstractDSLInstruction(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }
}
