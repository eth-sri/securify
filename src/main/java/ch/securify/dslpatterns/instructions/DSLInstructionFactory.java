package ch.securify.dslpatterns.instructions;

import ch.securify.decompiler.Variable;

/**
 * The factory to create DSL instructions
 */
public class DSLInstructionFactory {

    public static DSLGoto dslgoto(DSLLabel label, Variable var, DSLLabel secondBranchLabel) {
        return new DSLGoto(label, var, secondBranchLabel);
    }

    public static DSLStop stop(DSLLabel label) {
        return new DSLStop(label);
    }
}
