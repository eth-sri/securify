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
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Gas;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.SStore;

import java.util.List;

public class DAO extends AbstractInstructionPattern {

    public DAO() {
        super(new PatternDescription("RecursiveCalls",
                DAO.class,
                "Gas-dependent Reentrancy",
                "Calls into external contracts that receive all remaining gas and are followed by state changes may be reentrant.",
                PatternDescription.Severity.Critical,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        if (!(instr instanceof Call) || ((Call) instr).isBuiltInContractCall())
            return false;

        Variable value = instr.getInput()[2];
        if (value.hasConstantValue() && AbstractDataflow.getInt(value.getConstantValue()) == 0)
            return false;

        Variable gasVar = instr.getInput()[0];
        if (dataflow.varMayDepOn(instr, gasVar, Gas.class) == Status.UNSATISFIABLE)
            return false;

        return true;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction otherInstr : methodInstructions) {
            if (otherInstr instanceof SStore) {
                int s = dataflow.mustPrecede(instr, otherInstr);
                if (s == Status.SATISFIABLE) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        for (Instruction otherInstr : methodInstructions) {
            if (otherInstr instanceof SStore) {
                int s = dataflow.mayFollow(instr, otherInstr);
                if (s == Status.SATISFIABLE) {
                    return false;
                }
            }
        }
        return true;
    }

}
