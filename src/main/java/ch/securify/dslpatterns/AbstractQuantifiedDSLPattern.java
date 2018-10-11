package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

/**
 * Patterns that contain a quatifier, e.g. {@link Some}, {@link All}
 */
public class AbstractQuantifiedDSLPattern extends AbstractDSLPattern {

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

    protected String getPatternName() {
        return "AbstractQuantifiedDSLPattern";
    }


}
