package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.AbstractDSLPattern;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract supercalass for the objects that represent instructions in DSL patterns,
 * like {@link DSLGoto}, {@link DSLStop}...
 * */
public abstract class AbstractDSLInstruction extends AbstractDSLPattern implements DatalogElem {
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
    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labelsList = new HashSet<>();
        if(DSLLabel.isValidLabel(label))
            labelsList.add(label);
        return labelsList;
    }

    /**
     * @return a list of all the variables contained in the instruction
     */
    @Override
    public Set<Variable> getVariables() {
        Set<Variable> varsList = new HashSet<>();
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

    //Returns the string representation of the instruction, but with only the label name, all other fields are set
    //to don't care ("_")
    public abstract String getDatalogStringRepDC(DSLAnalysis analyzer);

    public abstract AbstractDSLInstruction getCopy();
}
