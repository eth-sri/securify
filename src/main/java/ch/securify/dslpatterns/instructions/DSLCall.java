package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.List;

/**
 * Placeholder for the call instruction inside the dsl language
 */
public class DSLCall extends AbstractDSLInstruction {

    private final Variable out;
    private final Variable in;
    private final Variable amount;

    /**
     * @param label the label at which instruction is placed
     * @param out the output of the call
     * @param in the input of the call
     * @param amount the amount connected to the call
     */
    public DSLCall(DSLLabel label, Variable out, Variable in, Variable amount) {
        super(label);
        this.out = out;
        this.in = in;
        this.amount = amount;
    }

    /**
     * @return a list of all the variables contained in the instruction
     */
    @Override
    public List<Variable> getAllVars() {
        List<Variable> varsList = super.getAllVars();

        if(isValidVariable(out))
            varsList.add(out);
        if(isValidVariable(in))
            varsList.add(in);
        if(isValidVariable(amount))
            varsList.add(amount);

        return varsList;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("call(");
        sb.append(label.getName());
        sb.append(" , ");
        sb.append(out.getName());
        sb.append(" , ");
        sb.append(in.getName());
        sb.append(" , ");
        sb.append(amount.getName());
        sb.append(")");
        return sb.toString();
    }

}
