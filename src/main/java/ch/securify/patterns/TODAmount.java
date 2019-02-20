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

public class TODAmount extends AbstractInstructionPattern {

    public TODAmount(){
        super(new PatternDescription("TransactionReordering",
                TODAmount.class,
                "Transaction Order Affects Ether Amount",
                "The amount of ether transferred must not be influenced by other transactions.",
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
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        if (instr instanceof Call) {
            Variable amount = instr.getInput()[2];
            if (dataflow.varMustDepOn(instr, amount, SLoad.class) == Status.SATISFIABLE) {

                for (Instruction sstore : contractInstructions) {
                    if (!(sstore instanceof SStore))
                        continue;

                    Variable index = sstore.getInput()[0];
                    if (!index.hasConstantValue())
                        continue;

                    Variable storageVar = ((Dataflow)dataflow).mustExplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                    if (dataflow.varMustDepOn(instr, amount, storageVar) == Status.SATISFIABLE)
                        return true;
                }

                // TODO: Check if the SLOAD instruction loads a constant offset and that there is an SSTORE with this offset
                //return true;
            }
            if (dataflow.varMustDepOn(instr, amount, Balance.class) == Status.SATISFIABLE) {
                // TODO: Assumes balance is not constant across transactions
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(instr instanceof Call);

        Variable amount = instr.getInput()[2];

        if (amount.hasConstantValue())
            return true;

        if (dataflow.varMustDepOn(instr, amount, Caller.class) == Status.SATISFIABLE)
            return true;

        if (dataflow.varMustDepOn(instr, amount, CallDataLoad.class) == Status.SATISFIABLE)
            return true;

        if (dataflow.varMayDepOn(instr, amount, Balance.class) == Status.SATISFIABLE) {
            return false;
        }

        if (dataflow.varMayDepOn(instr, amount, SLoad.class) == Status.SATISFIABLE) {
            if (dataflow.varMayDepOn(instr, amount, AbstractDataflow.UNK_CONST_VAL) == Status.SATISFIABLE) {
                return false;
            }

            for (Instruction sstore : contractInstructions) {
                if (!(sstore instanceof SStore))
                    continue;

                Variable index = sstore.getInput()[0];
                if (!index.hasConstantValue())
                    continue;

                Variable storageVar = ((Dataflow)dataflow).mayImplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                if (dataflow.varMayDepOn(instr, amount, storageVar) == Status.SATISFIABLE) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

}
