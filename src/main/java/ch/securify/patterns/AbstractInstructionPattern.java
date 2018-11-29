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

import ch.securify.analysis.AbstractDataflow;
import ch.securify.decompiler.instructions.Instruction;

import java.util.List;

public abstract class AbstractInstructionPattern extends AbstractPattern {

    public AbstractInstructionPattern(PatternDescription patternDescription) {
        super(patternDescription);
    }

    @Override
    public void checkPattern(List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction instr: methodInstructions) {
            if (!applicable(instr, dataflow))
                continue;

            boolean match = isViolation(instr, methodInstructions, contractInstructions, dataflow);
            boolean nonMatch = isCompliant(instr, methodInstructions, contractInstructions, dataflow);

            if (match && !nonMatch) {
                addViolation(instr);
            } else if (!match && nonMatch) {
                addSafe(instr);
            } else if (!match && !nonMatch) {
                addWarning(instr);
            } else {
                addConflict(instr);
            }
        }
    }


    protected abstract boolean applicable(Instruction instr, AbstractDataflow dataflow);
    protected abstract boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow);
    protected abstract boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow);
}
