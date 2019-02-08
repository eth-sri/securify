package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.SStore;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class UncheckedUntrustedReturn extends AbstractInstructionPattern {

    public UncheckedUntrustedReturn() {
        super(new PatternDescription("UncheckedUntrustedReturn",
                UncheckedUntrustedReturn.class,
                "Unchecked untrusted return",
                "Unchecked return values from a call to an untrusted contract",
                PatternDescription.Severity.High,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        if (!(instr instanceof Call))
            return false;

        return true;
    }

    @Override
    protected boolean isViolation(Instruction sstore, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        if (!(sstore instanceof SStore))
            return false;

        for (Instruction call : methodInstructions) {
            if (!(call instanceof  Call))
                continue;
            if (dataflow.mustPrecede(call, sstore) == Status.SATISFIABLE)
                continue;
        }

        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        // TODO: Add a compliance pattern
        return !isViolation(instr, methodInstructions, contractInstructions, dataflow);
    }
}
