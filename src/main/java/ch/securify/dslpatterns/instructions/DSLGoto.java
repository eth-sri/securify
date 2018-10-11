package ch.securify.dslpatterns.instructions;

import ch.securify.decompiler.Variable;

public class DSLGoto extends AbstractDSLInstruction{

    Variable var;
    DSLLabel secondBranchLabel;

    public DSLGoto(DSLLabel label, Variable var, DSLLabel secondBranchLabel) {
        super(label);
        this.var = var;
        this.secondBranchLabel = secondBranchLabel;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("goto(");
        sb.append(label.getName());
        sb.append(" , ");
        sb.append(var.getName());
        sb.append(" , ");
        sb.append(secondBranchLabel.getName());
        sb.append(")");

        return sb.toString();
    }
}
