package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Patterns that contain a quatifier, e.g. {@link Some}, {@link All}
 */
public abstract class AbstractQuantifiedDSLPattern extends AbstractDSLPattern {

    protected AbstractDSLInstruction quantifiedInstr;
    protected AbstractDSLPattern quantifiedPattern;

    public AbstractQuantifiedDSLPattern(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        this.quantifiedInstr = quantifiedInstr;
        this.quantifiedPattern = quantifiedPattern;
    }

    /**
     * @return a string description of the quantification
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();

        sb.append(getPatternName());
        sb.append(" ");
        sb.append(quantifiedInstr.getStringRepresentation());
        sb.append(" . ");
        sb.append(quantifiedPattern.getStringRepresentation());
        return sb.toString();
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>();
        vars.addAll(quantifiedInstr.getAllVars());
        vars.addAll(quantifiedPattern.getVariables());
        return vars;
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>();
        labels.addAll(quantifiedInstr.getAllLabels());
        labels.addAll(quantifiedPattern.getLabels());
        return labels;
    }

    protected String getPatternName() {
        return "AbstractQuantifiedDSLPattern";
    }

    public AbstractDSLInstruction getQuantifiedInstr() {
        return quantifiedInstr;
    }

    public AbstractDSLPattern getQuantifiedPattern() {
        return quantifiedPattern;
    }
}
