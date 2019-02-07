package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.CallDataCopy;
import ch.securify.decompiler.instructions.CallDataLoad;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.StaticCall;
import ch.securify.decompiler.printer.HexPrinter;

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
        if (!(instr instanceof Call) && !(instr instanceof StaticCall))
            return false;

        Variable gasAmount = instr.getInput()[0];
        if (gasAmount.hasConstantValue() && AbstractDataflow.getInt(gasAmount.getConstantValue()) == 0)
            return false;

//		System.out.println("Checking instruction: " + instr);
        return true;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction call : methodInstructions) {
            if (call == instr)
                continue;

        	if(!call.getClass().equals(instr.getClass()))
        		continue;

            Variable targetCall = call.getInput()[1];
            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
            	continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            if(dataflow.mustPrecede(call, instr) != Status.SATISFIABLE)
            	continue;


            // Only consider calls to untrusted code
            if(targetInstr.hasConstantValue()) {
            	continue;
            }

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            // Skip if one has no memory info
            if(callMemory.hasNext() != instrMemory.hasNext()) {
            	continue;
            }

            // In case of no memory info at least check the length
            if(!instrMemory.hasNext()) {
            	int memoryOffset = -1;
            	if(instr instanceof Call)
            		memoryOffset = 3;
            	else if(instr instanceof StaticCall)
            		memoryOffset = 2;
            	else
            		assert(false);
            	Variable callMemorySize = call.getInput()[memoryOffset];
                Variable instrMemorySize = instr.getInput()[memoryOffset];
                if(callMemorySize.hasConstantValue() != instrMemorySize.hasConstantValue()) {
                	continue;
                }
                if((callMemorySize.hasConstantValue()) && (Arrays.equals(callMemorySize.getConstantValue(), instrMemorySize.getConstantValue()))) {
                	continue;
                }
            }

            boolean matched = true;
            while (instrMemory.hasNext()){
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
            if(callMemory.hasNext()) {
            	continue;
            }
            if(matched) {
            	return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction call : methodInstructions) {
            if (call == instr)
                continue;

            if(!call.getClass().equals(instr.getClass()))
                continue;

            Variable targetCall = call.getInput()[1];

            // Trusted code is considered safe
            if (targetCall.hasConstantValue()) {
            	return true;
            }

            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
                continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            if(dataflow.mayFollow(call, instr) == Status.UNSATISFIABLE)
                continue;

            Variable callee = instr.getInput()[1];
            // If the code is from a safe source it should be fine
            if(dataflow.varMayDepOn(instr, callee, CallDataLoad.class) == Status.UNSATISFIABLE && dataflow.varMayDepOn(instr, callee, CallDataCopy.class) == Status.UNSATISFIABLE) {
            	continue;
            }

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            boolean matched = true;
            while (instrMemory.hasNext()){
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
            if(matched) {
                return false;
            }
        }
        return true;
    }
}
