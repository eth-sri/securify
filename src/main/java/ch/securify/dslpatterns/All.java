package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

/**
 * The all quatifier
 */
public class All extends AbstractQuantifiedDSLPattern{

    /**
     * @param quantifiedInstr the instruction to quantify over
     * @param quantifiedPattern the body of the quantifier
     */
    public All(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "all";
    }
}