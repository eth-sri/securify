package ch.securify.patterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Instruction;
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
        if (!(instr instanceof Call))
            return false;

        Variable value = instr.getInput()[2];
        if (!(value.hasConstantValue() && AbstractDataflow.getInt(value.getConstantValue()) == 0))
            return false;

        System.out.println("Checking: " + instr);
        return true;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction call : methodInstructions) {
            if (!(call instanceof Call) || call == instr)
                continue;

            if (call.getMemoryInputs().size() != instr.getMemoryInputs().size())
                return false;

            Iterator<Variable> callMemory = call.getMemoryInputs().iterator();
            Iterator<Variable> instrMemory = instr.getMemoryInputs().iterator();

            while (instrMemory.hasNext()){
                Variable callMemoryVar = callMemory.next();
                Variable instrMemoryVar = instrMemory.next();

                if (callMemoryVar.hasConstantValue() != instrMemoryVar.hasConstantValue())
                    return false;
                if (callMemoryVar.hasConstantValue() && instrMemoryVar.hasConstantValue()) {
                    if (!Arrays.equals(callMemoryVar.getConstantValue(), instrMemoryVar.getConstantValue())) {
                        return false;
                    }
                }
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


/*
 *  Copyright 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/***
package ch.securify.patterns;

        import ch.securify.analysis.AbstractDataflow;
        import ch.securify.analysis.Status;
        import ch.securify.decompiler.Variable;
        import ch.securify.decompiler.instructions.*;

        import java.util.List;

public class UnsafeCallTarget extends AbstractInstructionPattern {
    public UnsafeCallTarget(){
        super(new PatternDescription("DependenceOnUnsafeInputs",
                UnsafeCallTarget.class,
                "Unsafe Call to Untrusted Contract",
                "The target of a call instruction can be manipulated by an attacker.",
                PatternDescription.Severity.High,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        return instr instanceof Call;
    }

    @Override
    protected boolean isViolation(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(call instanceof  Call);
        assert(!call.getInput()[0].hasConstantValue());

        Variable target = call.getInput()[1];

        if (dataflow.varMustDepOn(call, target, CallDataLoad.class) == Status.UNSATISFIABLE)
            return false;

        Variable dataOffsetVar = call.getInput()[3];
        Variable dataLengthVar = call.getInput()[4];
        if (!dataOffsetVar.hasConstantValue() || !dataLengthVar.hasConstantValue())
            return false;

        int dataOffset = AbstractDataflow.getInt(dataOffsetVar.getConstantValue());
        int dataLength = AbstractDataflow.getInt(dataLengthVar.getConstantValue());

        if (dataLength <= 0)
            return false;

        for (int curOffset = dataOffset; curOffset < dataOffset + dataLength; curOffset += 4) {
            if (dataflow.memoryMustDepOn(call, curOffset, CallDataLoad.class) == Status.SATISFIABLE) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean isCompliant(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(call instanceof  Call);

        Variable target = call.getInput()[1];

        if (target.hasConstantValue())
            return true;

        Variable gas = call.getInput()[0];
        Variable dataLengthVar = call.getInput()[4];
        if (!dataLengthVar.hasConstantValue())
            return false;
        int dataLength = AbstractDataflow.getInt(dataLengthVar.getConstantValue());
        if (gas.hasConstantValue() && dataLength == 0)
            return true;

        return false;
    }

}



***/