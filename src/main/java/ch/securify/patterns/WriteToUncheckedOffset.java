package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.CallDataLoad;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.SStore;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class WriteToUncheckedOffset extends AbstractInstructionPattern {

    public WriteToUncheckedOffset() {
        super(new PatternDescription("WriteToUncheckedOffset",
                WriteToUncheckedOffset.class,
                "Write to unchecked offset",
                "Write to an offset value originating from an untrusted input",
                PatternDescription.Severity.High,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        if (!(instr instanceof SStore))
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
            if (dataflow.mustPrecede(call, sstore) == Status.UNSATISFIABLE)
                continue;

            Variable callee = call.getInput()[1];
            if (dataflow.varMustDepOn(call, callee, CallDataLoad.class) == Status.UNSATISFIABLE)
                continue;

            Variable index = sstore.getInput()[0];
            if (dataflow.varMustDepOn(sstore, index, call) == Status.SATISFIABLE)
                return true;

        }

        return false;
    }

    @Override
    protected boolean isCompliant(Instruction sstore, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        if (!(sstore instanceof SStore))
            return false;

        for (Instruction call : methodInstructions) {
            if (!(call instanceof Call))
                continue;

            if (dataflow.mayFollow(call, sstore) == Status.UNSATISFIABLE)
                continue;

            //Variable callee = call.getInput()[1];
            //if (dataflow.varMayDepOn(call, callee, CallDataLoad.class) == Status.UNSATISFIABLE)
            //    continue;

            Variable index = sstore.getInput()[0];
            if (dataflow.varMayDepOn(sstore, index, call) == Status.UNSATISFIABLE)
                continue;

            return false;

        }

        return true;
    }
}