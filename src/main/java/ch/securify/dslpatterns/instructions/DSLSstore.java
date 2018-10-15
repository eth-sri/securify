package ch.securify.dslpatterns.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

/**
 * The dsl placeholder for the instruction sstore
 */
public class DSLSstore extends AbstractDSLInstruction {

    private Variable offset;
    private Variable var;

    /**
     * @param label the label at which the instrucion is placed
     * @param offset the offset in the stack
     * @param var the variable to store
     */
    public DSLSstore(DSLLabel label, Variable offset, Variable var) {
        super(label);
        this.offset = offset;
        this.var = var;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("sstore(");
        sb.append(label.getName());
        sb.append(" , ");
        sb.append(offset.getName());
        sb.append(" , ");
        sb.append(var.getName());
        sb.append(")");

        return sb.toString();
    }
}

