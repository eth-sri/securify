package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.List;

/**
 * The dsl placeholder for the instruction sload
 */
public class DSLSload extends AbstractDSLInstruction {

    private Variable offset;
    private Variable var;

    /**
     * @param label the label at which the instrucion is placed
     * @param offset the offset in the stack
     * @param var the variable to load
     */
    public DSLSload(DSLLabel label, Variable offset, Variable var) {
        super(label);
        this.offset = offset;
        this.var = var;
    }

    @Override
    public DSLSload getCopy() {
        return new DSLSload(getLabel(), offset, var);
    }

    /**
     * @return a list of all the variables contained in the instruction
     */
    @Override
    public List<Variable> getAllVars() {
        List<Variable> varsList = super.getAllVars();

        if(isValidVariable(offset))
            varsList.add(offset);
        if(isValidVariable(var))
            varsList.add(var);

        return varsList;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("sload(");
        sb.append(label.getName());
        sb.append(" , ");
        sb.append(offset.getName());
        sb.append(" , ");
        sb.append(var.getName());
        sb.append(")");

        return sb.toString();
    }

}

