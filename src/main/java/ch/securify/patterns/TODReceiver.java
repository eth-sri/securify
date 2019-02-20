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
import ch.securify.analysis.Dataflow;
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;

import java.util.List;

public class TODReceiver extends AbstractInstructionPattern {

    public TODReceiver() {
        super(new PatternDescription("TransactionReordering",
                TODReceiver.class,
                "Transaction Order Affects Ether Receiver",
               "The receiver of ether transfers must not be influenced by other transactions.",
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
        assert (call instanceof Call);

        Variable receiver = call.getInput()[1];
        if (dataflow.varMustDepOn(call, receiver, SLoad.class) == Status.SATISFIABLE) {

            for (Instruction sstore : contractInstructions) {
                if (!(sstore instanceof SStore))
                    continue;

                Variable index = sstore.getInput()[0];
                if (!index.hasConstantValue())
                    continue;

                Variable storageVar = ((Dataflow)dataflow).mustExplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                if (dataflow.varMustDepOn(call, receiver, storageVar) == Status.SATISFIABLE)
                    return true;
            }
            // TODO: Check if the SLOAD instruction loads a constant offset and that there is an SSTORE with this offset
            //return true;
        }

        return false;
    }

    @Override
    protected boolean isCompliant(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert (call instanceof Call);

        Variable receiver = call.getInput()[1];

        if (receiver.hasConstantValue())
            return true;

        if (dataflow.varMustDepOn(call, receiver, Caller.class) == Status.SATISFIABLE)
            return true;

        if (dataflow.varMustDepOn(call, receiver, CallDataLoad.class) == Status.SATISFIABLE)
            return true;

        if (dataflow.varMustDepOn(call, receiver, Address.class) == Status.SATISFIABLE)
            return true;

        if (dataflow.varMayDepOn(call, receiver, SLoad.class) == Status.SATISFIABLE) {
            if (dataflow.varMayDepOn(call, receiver, AbstractDataflow.UNK_CONST_VAL) == Status.SATISFIABLE) {
                return false;
            }

            for (Instruction sstore : contractInstructions) {
                if (!(sstore instanceof SStore))
                    continue;

                Variable index = sstore.getInput()[0];
                if (!index.hasConstantValue())
                    continue;

                Variable storageVar = ((Dataflow)dataflow).mayImplicitDataflow.getStorageVarForIndex(Dataflow.getInt(index.getConstantValue()));
                if (dataflow.varMayDepOn(call, receiver, storageVar) == Status.SATISFIABLE) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

}
