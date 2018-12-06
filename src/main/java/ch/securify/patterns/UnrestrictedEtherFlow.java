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
import ch.securify.analysis.Status;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;

public class UnrestrictedEtherFlow extends AbstractInstructionPattern {

    public UnrestrictedEtherFlow() {
        super(new PatternDescription("InsecureCodingPatterns",
                UnrestrictedEtherFlow.class,
                "Unrestricted ether flow",
                "The execution of ether flows should be restricted to an authorized set of users.",
                PatternDescription.Severity.Critical,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        return instr instanceof Call;
    }

    @Override
    protected boolean isViolation(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert (call instanceof Call);

        if (dataflow.instrMayDepOn(call, Caller.class) == Status.SATISFIABLE) {
            // the transfer may depend on who is the caller
            return false;
        }

        Variable amount = call.getInput()[2];

        if (amount.hasConstantValue() && AbstractDataflow.getInt(amount.getConstantValue()) == 0) {
            // the amount is zero
            return false;
        }

        if (amount.hasConstantValue() && AbstractDataflow.getInt(amount.getConstantValue()) > 0) {
            // the amount is a positive constant
            return true;
        }

        if (dataflow.varMustDepOn(call, amount, CallDataLoad.class) == Status.SATISFIABLE) {
            // the amount can be influenced by the caller
            return true;
        }


        return false;
    }

    @Override
    protected boolean isCompliant(Instruction call, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        assert(call instanceof Call);

        Variable value = call.getInput()[2];

        if (value.hasConstantValue() && AbstractDataflow.getInt(value.getConstantValue()) == 0) {
            // the amount is zero
            return true;
        }

        for (Instruction jump : methodInstructions) {
            if (!(jump instanceof JumpI))
                continue;

            if (dataflow.mustPrecede(jump, call) == Status.SATISFIABLE) {
                Variable cond = ((JumpI) jump).getCondition();
                if (dataflow.varMustDepOn(jump, cond, Caller.class) == Status.SATISFIABLE) {
                    // there must be a jump instruction that preceeds the call and whose condition depends on the caller
                    return true;
                }
            }
        }

        return false;
    }

}
