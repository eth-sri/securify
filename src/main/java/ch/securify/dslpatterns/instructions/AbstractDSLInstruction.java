package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;
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

    public void setLabel(DSLLabel label) {
        this.label = label;
    }

    /**
     * @return a list of all the labels contained in the instruction
     */
    public List<DSLLabel> getAllLabels() {
        List<DSLLabel> labelsList = new ArrayList<DSLLabel>();
        if(DSLLabel.isValidLabel(label))
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

    public abstract AbstractDSLInstruction getCopy();
}
