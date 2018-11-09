package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract supercalass for the objects that represent instructions in DSL patterns,
 * like {@link DSLGoto}, {@link DSLStop}...
 * */
public abstract class AbstractDSLInstruction implements DatalogElem {
    //the label on which the instruction is located
    protected DSLLabel label = null;

    public AbstractDSLInstruction(DSLLabel label) {
        this.label = label;
    }

    public DSLLabel getLabel() {
        return label;
    }

    /**
     * @return a list of all the labels contained in the instruction
     */
    public List<DSLLabel> getAllLabels() {
        List<DSLLabel> labelsList = new ArrayList<DSLLabel>();
        if(isValidLabel(label))
            labelsList.add(label);
        return labelsList;
    }

    /**
     * @return a list of all the variables contained in the instruction
     */
    public List<Variable> getAllVars() {
        List<Variable> varsList = new ArrayList<Variable>();
        return varsList;
    }

    /**
     * Checks if the label is null or an instance of {@link ch.securify.dslpatterns.util.DSLLabelDC}
     * @param l the label
     * @return true if it's a valid label
     */
    protected boolean isValidLabel(DSLLabel l)
    {
        if(l == null || l instanceof DSLLabelDC)
            return false;
        return true;
    }

    /**
     * Checks if the variable is null or an instance of {@link ch.securify.dslpatterns.util.VariableDC}
     * @param var the variable
     * @return true if it's a valid variable
     */
    protected boolean isValidVariable(Variable var)
    {
        if(var == null || var instanceof VariableDC)
            return false;
        return true;
    }

    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("AbstractDSLInstruction(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        return getStringRepresentation();
    }
}
