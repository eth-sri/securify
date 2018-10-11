package ch.securify.dslpatterns.instructions;

public class DSLStop extends AbstractDSLInstruction {

    public DSLStop(DSLLabel label) {
        super(label);
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("stop(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }
}
