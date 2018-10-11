package ch.securify.dslpatterns;

import ch.securify.decompiler.instructions.Instruction;

public class All extends AbstractQuantifiedDSLPattern{

    public All(Instruction quantifiedInstr, AbstractDSLPattern quantifiedPattern) {
        super(quantifiedInstr, quantifiedPattern);
    }

    @Override
    protected String getPatternName() {
        return "all";
    }
}
