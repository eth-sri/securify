package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class RepeatedCall extends AbstractInstructionPattern {

    public RepeatedCall() {
        super(new PatternDescription("RepeatedCall",
                RepeatedCall.class,
                "Repeated call to an untrusted contract",
                "Repeated call to an untrusted contract may result in different values",
                PatternDescription.Severity.High,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        if (!(instr instanceof CallingInstruction))
            return false;

        CallingInstruction call = (CallingInstruction) instr;

        if (call.isBuiltInContractCall()) {
            return false;
        }

        Variable gasAmount = call.getInput()[0];

        return !gasAmount.hasConstantValue() || AbstractDataflow.getInt(gasAmount.getConstantValue()) != 0;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        return isViolation((CallingInstruction) instr, methodInstructions, dataflow);
    }

    protected boolean isViolation(CallingInstruction instr, List<Instruction> methodInstructions, AbstractDataflow dataflow) {
        Variable callee = instr.getInput()[1];
        // If the code is from a safe source it should be fine
        if (dataflow.varMayDepOn(instr, callee, CallDataLoad.class) != Status.SATISFIABLE && dataflow.varMayDepOn(instr, callee, CallDataCopy.class) != Status.SATISFIABLE) {
            return false;
        }

        Instruction prev = instr.getPrev();
        for (Instruction mInstr : methodInstructions) {

            if (!mInstr.getClass().equals(instr.getClass()))
                continue;

            CallingInstruction call = (CallingInstruction) mInstr;

            Variable targetCall = call.getInput()[1];
            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
                continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            if (call == instr) {
                // Check if it can be reached through a loop
                if (dataflow.mayFollow(call, prev) != Status.SATISFIABLE)
                    continue;
            } else {
                // Check if the two operations are subsequent
                if (dataflow.mustPrecede(call, instr) != Status.SATISFIABLE)
                    continue;
            }

            // Only consider calls to untrusted code
            if (targetInstr.hasConstantValue()) {
                continue;
            }

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            // Skip if one has no memory info
            if (callMemory.hasNext() != instrMemory.hasNext()) {
                continue;
            }

            // In case of no memory info at least check the length
            if (!instrMemory.hasNext()) {
                Variable callMemorySize = call.getInput()[call.getInputMemorySize()];
                Variable instrMemorySize = instr.getInput()[instr.getInputMemorySize()];
                if (callMemorySize.hasConstantValue() != instrMemorySize.hasConstantValue()) {
                    continue;
                }
                if ((callMemorySize.hasConstantValue()) && (Arrays.equals(callMemorySize.getConstantValue(), instrMemorySize.getConstantValue()))) {
                    continue;
                }
            }

            boolean matched = true;
            while (instrMemory.hasNext()) {
                Variable callMemoryVar = callMemory.next();
                Variable instrMemoryVar = instrMemory.next();

                if (callMemoryVar.hasConstantValue() != instrMemoryVar.hasConstantValue()) {
                    matched = false;
                    break;
                }
                if (callMemoryVar.hasConstantValue() && instrMemoryVar.hasConstantValue()) {
                    if (!Arrays.equals(callMemoryVar.getConstantValue(), instrMemoryVar.getConstantValue())) {
                        matched = false;
                        break;
                    }
                }
            }
            // More Memory left for call
            if (callMemory.hasNext()) {
                continue;
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        return isCompliant((CallingInstruction) instr, methodInstructions, dataflow);
    }

    protected boolean isCompliant(CallingInstruction instr, List<Instruction> methodInstructions, AbstractDataflow dataflow) {
        Variable callee = instr.getInput()[1];
        // If the code is from a safe source it should be fine
        if (dataflow.varMayDepOn(instr, callee, CallDataLoad.class) == Status.UNSATISFIABLE && dataflow.varMayDepOn(instr, callee, CallDataCopy.class) == Status.UNSATISFIABLE) {
            return true;
        }

        Instruction prev = instr.getPrev();
        for (Instruction mInstr : methodInstructions) {
            if (mInstr == instr) {
                // Check if it can be reached through a loop
                if (dataflow.mayFollow(mInstr, prev) == Status.UNSATISFIABLE)
                    continue;
            } else {
                // Check if the two operations are subsequent
                if (dataflow.mayFollow(mInstr, instr) == Status.UNSATISFIABLE)
                    continue;
            }


            if (!mInstr.getClass().equals(instr.getClass()))
                continue;

            CallingInstruction call = (CallingInstruction) mInstr;

            Variable targetCall = call.getInput()[1];

            // Trusted code is considered safe
            if (targetCall.hasConstantValue()) {
                continue;
            }

            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
                continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            // In case of no memory info at least check the length
            if (!instrMemory.hasNext()) {
                Variable callMemorySize = call.getInput()[call.getInputMemorySize()];
                Variable instrMemorySize = instr.getInput()[instr.getInputMemorySize()];
                if (callMemorySize.hasConstantValue() != instrMemorySize.hasConstantValue()) {
                    continue;
                }
                if ((callMemorySize.hasConstantValue()) && (Arrays.equals(callMemorySize.getConstantValue(), instrMemorySize.getConstantValue()))) {
                    continue;
                }
            }

            boolean matched = true;
            while (instrMemory.hasNext()) {
                Variable callMemoryVar = callMemory.next();
                Variable instrMemoryVar = instrMemory.next();

                if (callMemoryVar.hasConstantValue() != instrMemoryVar.hasConstantValue()) {
                    matched = false;
                    break;
                }
                if (callMemoryVar.hasConstantValue() && instrMemoryVar.hasConstantValue()) {
                    if (!Arrays.equals(callMemoryVar.getConstantValue(), instrMemoryVar.getConstantValue())) {
                        matched = false;
                        break;
                    }
                }
            }
            if (matched) {
                return false;
            }
        }
        return true;
    }
}
