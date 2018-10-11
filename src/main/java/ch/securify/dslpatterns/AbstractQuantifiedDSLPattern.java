package ch.securify.dslpatterns;

import ch.securify.decompiler.instructions.Instruction;

public class AbstractQuantifiedDSLPattern extends AbstractDSLPattern {

    protected Instruction quantifiedInstr;
    protected AbstractDSLPattern quantifiedPattern;

    public AbstractQuantifiedDSLPattern(Instruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
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
