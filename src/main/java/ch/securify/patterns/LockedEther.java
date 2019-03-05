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

public class LockedEther extends AbstractContractPattern {

    public LockedEther() {
        super(new PatternDescription("LockedEther",
                LockedEther.class,
                "Locked Ether",
                "Contracts that may receive ether must also allow users to extract the deposited ether from the contract.",
                PatternDescription.Severity.Medium,
                PatternDescription.Type.Security));

    }

    private boolean allStopsCannotReceiveEther(List<Instruction> instructions, AbstractDataflow dataflow) {
        for (Instruction haltInstr : instructions) {
            if (haltInstr instanceof Stop || haltInstr instanceof Return) {
                boolean stopCannotReceiveEther = false;
                for (Instruction jumpInstr : instructions) {
                    if (jumpInstr instanceof JumpI) {
                        Variable cond = ((JumpI) jumpInstr).getCondition();
                        if (dataflow.mustPrecede(jumpInstr, haltInstr) == Status.SATISFIABLE
                                && dataflow.varMustDepOn(jumpInstr, cond, CallValue.class) == Status.SATISFIABLE
                                && dataflow.varMustDepOn(jumpInstr, cond, IsZero.class) == Status.SATISFIABLE) {
                            stopCannotReceiveEther = true;
                            break;
                        }
                    }
                }
                if (!stopCannotReceiveEther) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected boolean isSafe(List<Instruction> instructions, AbstractDataflow dataflow) {
        if (allStopsCannotReceiveEther(instructions, dataflow)) {
            return true;
        }

        // Check if the contract can send ether (has a call instruction with positive amount or a selfdestruct)
        for (Instruction instr : instructions) {
            if (instr instanceof SelfDestruct) {
                return true;
            }

            if (!(instr instanceof CallingInstruction))
                continue;

            if (instr instanceof DelegateCall) {
                return true;
            }

            CallingInstruction callInstr = (CallingInstruction) instr;

            Variable amount = callInstr.getValue();
            if (amount.hasConstantValue() && AbstractDataflow.getInt(amount.getConstantValue()) != 0
                    || dataflow.varMustDepOn(callInstr, amount, Balance.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, CallDataLoad.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, CallValue.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, MLoad.class) == Status.SATISFIABLE
                    || dataflow.varMustDepOn(callInstr, amount, SLoad.class) == Status.SATISFIABLE) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isViolation(List<Instruction> instructions, AbstractDataflow dataflow) {
        if (allStopsCannotReceiveEther(instructions, dataflow)) {
            return false;
        }

        // check if the contract can transfer ether
        for (Instruction callInstr : instructions) {
            if (callInstr instanceof CallingInstruction) {
                if (callInstr instanceof DelegateCall) {
                    return false;
                } else {
                    Variable amount = ((CallingInstruction) callInstr).getValue();
                    if (!amount.hasConstantValue() || BigIntUtil.fromInt256(amount.getConstantValue()).compareTo(BigInteger.ZERO) != 0) {
                        return false;
                    }
                }
            } else if (callInstr instanceof SelfDestruct) {
                return false;
            }
        }
        return true;
    }

}
