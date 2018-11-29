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


package ch.securify.patterns;

import java.util.List;

import ch.securify.analysis.Status;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.JumpI;
import ch.securify.decompiler.instructions.SStore;

public class UnrestrictedWrite extends AbstractInstructionPattern {

    public UnrestrictedWrite() {
        super(new PatternDescription("InsecureCodingPatterns",
                UnrestrictedWrite.class,
                "Unrestricted write to storage",
                "Contract fields that can be modified by any user must be inspected.",
                PatternDescription.Severity.Critical,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        return instr instanceof SStore;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(instr instanceof SStore);

        if (dataflow.varMayDepOn(instr, instr.getInput()[0], Caller.class) == Status.SATISFIABLE)
            return false;

        if (dataflow.instrMayDepOn(instr, Caller.class) == Status.SATISFIABLE)
            return false;

        return true;
    }

    @Override
    protected boolean isCompliant(Instruction sstore, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(sstore instanceof SStore);
        if (dataflow.varMustDepOn(sstore, sstore.getInput()[0], Caller.class) == Status.SATISFIABLE) {
            return true;
        }

        for (Instruction jump : methodInstructions) {
            if (!(jump instanceof JumpI))
                continue;

            if (dataflow.mustPrecede(jump, sstore) == Status.SATISFIABLE) {
                Variable cond = ((JumpI) jump).getCondition();
                if (dataflow.varMustDepOn(jump, cond, Caller.class) == Status.SATISFIABLE) {
                    return true;
                }
            }
        }

        return false;
    }

}
