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

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.Dataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;

public class TODTransfer extends AbstractInstructionPattern {

    public TODTransfer() {
        super(new PatternDescription("TransactionReordering",
                TODTransfer.class,
                "Transaction Order Affects Execution of Ether Transfer",
                "Ether transfers whose execution can be manipulated by other transactions must be inspected for unintended behavior.",
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

        return true;
    }

    @Override
    protected boolean isViolation(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(call instanceof Call);

        for (Instruction jump : methodInstructions) {
            if (!(jump instanceof JumpI))
                continue;

            if (dataflow.mustPrecede(jump, call) == Status.UNSATISFIABLE)
                continue;

            Variable cond = ((JumpI)jump).getCondition();
            if (dataflow.varMustDepOn(jump, cond, SLoad.class) == Status.SATISFIABLE) {

                for (Instruction sstore : contractInstructions) {
                    if (!(sstore instanceof SStore))
                        continue;

                    Variable index = sstore.getInput()[0];
                    if (!index.hasConstantValue())
                        continue;

                    Variable storageVar = ((Dataflow)dataflow).mustExplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                    if (dataflow.varMustDepOn(jump, cond, storageVar) == Status.SATISFIABLE)
                        return true;
                }

                //return true;
            }
        }

        return false;
    }

    @Override
    protected boolean isCompliant(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(call instanceof Call);

        if (dataflow.instrMayDepOn(call, SLoad.class) == Status.UNSATISFIABLE) {
            if (dataflow.instrMayDepOn(call, Balance.class) == Status.UNSATISFIABLE) {
                return true;
            }
        } else {
            for (Instruction jump : methodInstructions) {
                if (!(jump instanceof JumpI))
                    continue;

                if (dataflow.mayFollow(jump, call) == Status.UNSATISFIABLE)
                    continue;

                Variable cond = ((JumpI) jump).getCondition();

                if (dataflow.varMayDepOn(jump, cond, AbstractDataflow.UNK_CONST_VAL) == Status.SATISFIABLE) {
                    return false;
                }

                for (Instruction sstore : contractInstructions) {
                    if (!(sstore instanceof SStore))
                        continue;

                    Variable index = sstore.getInput()[0];
                    if (!index.hasConstantValue())
                        continue;

                    Variable storageVar = ((Dataflow)dataflow).mayImplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                    if (dataflow.varMayDepOn(jump, cond, storageVar) == Status.SATISFIABLE) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}
