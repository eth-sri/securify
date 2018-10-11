package ch.securify.dslpatterns;

import ch.securify.decompiler.instructions.Instruction;

public class Some extends AbstractQuantifiedDSLPattern {

    public Some(Instruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "some";
    }
}
