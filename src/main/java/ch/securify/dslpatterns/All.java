package ch.securify.dslpatterns;

import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;

public class All extends AbstractQuantifiedDSLPattern{

    public All(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "all";
    }
}
