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

public abstract class AbstractContractPattern extends AbstractPattern {

    public AbstractContractPattern(PatternDescription description) {
        super(description);
    }

    @Override
    public void checkPattern(List<Instruction> instructions, List<Instruction> allInstructions, AbstractDataflow dataflow) {
        if (instructions.size() < 1)
            return;

        boolean isViolation = isViolation(allInstructions, dataflow);
        boolean isSafe = isSafe(allInstructions, dataflow);

        Instruction firstInstr = instructions.get(0);

        if (isViolation && !isSafe) {
            addViolation(firstInstr);
        } else if (!isViolation && isSafe) {
            addSafe(firstInstr);
        } else if (!isViolation && !isSafe) {
            addWarning(firstInstr);
        } else {
            addConflict(firstInstr);
        }
    }

    protected abstract boolean isSafe(List<Instruction> instructions, AbstractDataflow dataflow);
    protected abstract boolean isViolation(List<Instruction> instructions, AbstractDataflow dataflow);
}
