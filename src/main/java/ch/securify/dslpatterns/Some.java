package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

/**
 * The some quantifier
 */
public class Some extends AbstractQuantifiedDSLPattern {

    /**
     * @param quantifiedInstr the instruction to quantify over
     * @param quantifiedPattern the body of the quantifier
     */
    public Some(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "some";
    }
}
