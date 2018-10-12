package ch.securify.dslpatterns.instructions;

/**
 * Abstract supercalass for the objects that represent instructions in DSL patterns,
 * like {@link DSLGoto}, {@link DSLStop}...
 * */
public class AbstractDSLInstruction {
    //the label on which the instruction is located
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
