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

import java.math.BigInteger;
import java.util.List;

import ch.securify.analysis.Status;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;
import ch.securify.utils.BigIntUtil;

// TODO: buggy
public class LockedEther extends AbstractContractPattern {

    @Override
    protected boolean isSafe(List<Instruction> instructions, AbstractDataflow dataflow) {
        // Check if the contract cannot receive ether
        boolean allStopsCannotReceiveEther = true;
        for (Instruction stopInstr : instructions) {
            if (stopInstr instanceof Stop) {
                boolean stopCannotReceiveEther = false;
                for (Instruction jumpInstr : instructions) {
                    if (jumpInstr instanceof JumpI) {
                        Variable cond = ((JumpI) jumpInstr).getCondition();
                        if (dataflow.mustPrecede(jumpInstr, stopInstr) == Status.SATISFIABLE
                                && dataflow.varMustDepOn(jumpInstr, cond, CallValue.class) == Status.SATISFIABLE
                                && dataflow.varMustDepOn(jumpInstr, cond, IsZero.class) == Status.SATISFIABLE) {
                            stopCannotReceiveEther = true;
                            break;
                        }
                    }
                }
                if (!stopCannotReceiveEther) {
                    allStopsCannotReceiveEther = false;
                    break;
                }
            }
        }

        if (allStopsCannotReceiveEther)
            return true;


        // Check if the contract can send ether (has a call instruction with positive amount)
        for (Instruction callInstr : instructions) {
            if (! (callInstr instanceof Call))
                continue;

            Variable amount = callInstr.getInput()[2];
            if (dataflow.varMustDepOn(callInstr, amount, Balance.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, CallDataLoad.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, MLoad.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, SLoad.class) == Status.SATISFIABLE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isViolation(List<Instruction> instructions, AbstractDataflow dataflow) {
        boolean contractCanReceiveEther = false;

        for (Instruction stopInstr : instructions) {
            if (!(stopInstr instanceof Stop))
                continue;

            if (dataflow.instrMayDepOn(stopInstr, CallValue.class) == Status.UNSATISFIABLE) {
                contractCanReceiveEther = true;
                break;
            }
        }

        if (!contractCanReceiveEther)
            return false;

        // check if the contract can transfer ether
        for (Instruction callInstr : instructions) {
            if (!(callInstr instanceof Call))
                continue;

            Variable amount = callInstr.getInput()[2];
            if (!amount.hasConstantValue() || BigIntUtil.fromInt256(amount.getConstantValue()).compareTo(BigInteger.ZERO) != 0) {
                return false;
            }
        }
        return true;
    }

}
