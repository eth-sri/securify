package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
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

        Variable gasAmount = instr.getInput()[2];
        if (gasAmount.hasConstantValue() && AbstractDataflow.getInt(gasAmount.getConstantValue()) == 0)
            return false;

		System.out.println("Checking instruction: " + instr);
        return true;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction call : methodInstructions) {
            if (call == instr)
                continue;
            
        	if(!call.getClass().equals(instr.getClass()))
        		continue;
        	
            Variable targetCall = instr.getInput()[1];
            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
            	continue;
            
            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            if(dataflow.mustPrecede(call, instr) != Status.SATISFIABLE)
            	continue;
            
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

            Variable targetCall = instr.getInput()[1];
            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue())
                continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            if(dataflow.mayFollow(call, instr) == Status.UNSATISFIABLE)
                continue;

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
