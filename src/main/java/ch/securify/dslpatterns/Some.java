package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

public class Some extends AbstractQuantifiedDSLPattern {

    public Some(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "some";
    }
}
