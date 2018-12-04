package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

/**
 * This class is the most external container for an instruction pattern,
 * it contains the quantified instruction and the body of the pattern
 */
public class InstructionDSLPattern extends AbstractQuantifiedDSLPattern {
    public InstructionDSLPattern(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "";
    }
}
