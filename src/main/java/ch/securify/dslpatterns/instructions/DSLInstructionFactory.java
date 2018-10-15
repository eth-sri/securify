package ch.securify.dslpatterns.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

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

    public static DSLCall call(DSLLabel label, Variable out, Variable in, Variable amount) {
        return new DSLCall(label, out, in, amount);
    }

    public static DSLSstore sstore(DSLLabel label, Variable offset, Variable var) {
        return new DSLSstore(label, offset, var);
    }

    public static DSLSload sload(DSLLabel label, Variable offset, Variable var) {
        return new DSLSload(label, offset, var);
    }

}
