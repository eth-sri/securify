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

        Variable value = instr.getInput()[2];
        if (!(value.hasConstantValue() && AbstractDataflow.getInt(value.getConstantValue()) == 0))
            return false;

//        System.out.println("Checking: " + instr);
        return true;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction call : methodInstructions) {
        	if(!(call instanceof StaticCall) && (instr instanceof StaticCall)){
        		continue;
        	}

        	if(!(call instanceof Call) && (instr instanceof Call)){
        		continue;
        	}
        	
            if (call == instr)
                continue;
            
            int s = dataflow.mustPrecede(instr, call);
            if(s != Status.SATISFIABLE) {
            	continue;
            }
            
            Variable targetCall = instr.getInput()[1];
            Variable targetInstr = instr.getInput()[1];

            if (targetCall.hasConstantValue() != targetInstr.hasConstantValue()) {
            	continue;
            }
            
            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                continue;

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            boolean mismatch = false;
            while (instrMemory.hasNext()){
                Variable callMemoryVar = callMemory.next();
                Variable instrMemoryVar = instrMemory.next();

                if (callMemoryVar.hasConstantValue() != instrMemoryVar.hasConstantValue()) {
                	mismatch = true;
                    break;
                }
                if (callMemoryVar.hasConstantValue() && instrMemoryVar.hasConstantValue()) {
                    if (!Arrays.equals(callMemoryVar.getConstantValue(), instrMemoryVar.getConstantValue())) {
                    	mismatch = true;
                    	break;
                    }
                }
            }
            if(mismatch) {
            	continue;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        return !isViolation(instr, methodInstructions, contractInstructions, dataflow);
    }
}