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


package ch.securify.analysis;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.BranchInstruction;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.JumpI;
import ch.securify.decompiler.instructions._VirtualMethodHead;

import java.io.IOException;
import java.util.List;

public class MayImplicitDataflow extends AbstractDataflow {
    static final public String binaryName = "mayImplicit";

    public MayImplicitDataflow(List<Instruction> decompiledInstructions) throws IOException, InterruptedException {
        instructions = decompiledInstructions;
        initDataflow(binaryName);
    }

    @Override
    public int mustPrecede(Instruction instr1, Instruction instr2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int mayFollow(Instruction instr1, Instruction instr2) {
        return runQuery("isAfter", getCode(instr1), getCode(instr2));
    }

    @Override
    public int varMayDepOn(Instruction instr1, Variable lhs, Object type) {
        return runQuery("reach", getCode(lhs), getCode(type));
    }

    @Override
    public int varMustDepOn(Instruction instr1, Variable lhs, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int memoryMustDepOn(Instruction instr1, int offset, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int memoryMayDepOn(Instruction instr, int offset, Object type) {
        return runQuery("memory", getCode(instr), getCode(getMemoryVarForIndex(offset)), getCode(type));
    }

    @Override
    public int memoryMayDepOn(Instruction instr, Object type) {
        return runQuery("memoryTags", getCode(instr), getCode(type));
    }

    @Override
    public int instrMayDepOn(Instruction instr, Object type) {
        return runQuery("reachInstr", getCode(instr), getCode(type));
    }

    @Override
    protected void createSLoadRule(Instruction instr, Variable index, Variable var) {
        int indexCode;
        if (index.hasConstantValue()) {
            indexCode = getCode(getInt(index.getConstantValue()));
        } else {
            indexCode = unk;
            // if you have "var = sload(index)", to propagate labels from index to var we add "var = index"
            createAssignVarRule(instr, var, index);
        }
        appendRule("sload", getCode(instr), indexCode, getCode(var));
    }

    @Override
    protected void createMLoadRule(Instruction instr, Variable offset, Variable var) {
        int offsetCode;
        if (offset.hasConstantValue()) {
            offsetCode = getCode(getMemoryVarForIndex(getInt(offset.getConstantValue())));
        } else {
            offsetCode = unk;
            createAssignVarRule(instr, var, offset);
        }
        appendRule("mload", getCode(instr), offsetCode, getCode(var));
    }

    @Override
    protected void deriveFollowsPredicates() {
        log(">> Derive follows predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof BranchInstruction) {
                BranchInstruction branchInstruction = (BranchInstruction) instr;
                for (Instruction outgoingInstruction : branchInstruction.getOutgoingBranches()) {
                    if (!(outgoingInstruction instanceof _VirtualMethodHead)) {
                        createFollowsRule(instr, outgoingInstruction);
                    }
                }
            }
            Instruction nextInstruction = instr.getNext();

            if (nextInstruction != null) {
                createFollowsRule(instr, nextInstruction);
            }
        }
    }

    @Override
    protected void deriveIfPredicates() {
        log(">> Derive TaintElse and TaintThen predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof JumpI) {
                JumpI ifInstr = (JumpI) instr;
                Variable condition = ifInstr.getCondition();
                Instruction thenInstr = ifInstr.getTargetInstruction();
                Instruction elseInstr = ifInstr.getNext();
                Instruction mergeInstr = ifInstr.getMergeInstruction();

                if (thenInstr != null && thenInstr != mergeInstr) {
                    log("then instruction: " + thenInstr.getStringRepresentation());
                    createTaintRule(instr, thenInstr, condition);
                }

                if (elseInstr != null && elseInstr != mergeInstr ) {
                    log("else instruction: " + elseInstr.getStringRepresentation());
                    createTaintRule(instr, elseInstr, condition);
                }

                if (mergeInstr != null) {
                    log("merge instruction: " + mergeInstr.getStringRepresentation());
                    createEndIfRule(instr, mergeInstr);
                }
            }
        }
    }

    private void createFollowsRule(Instruction from, Instruction to) {
        appendRule("follows", getCode(from), getCode(to));
    }

    private void createTaintRule(Instruction labStart, Instruction lab, Variable var) {
        appendRule("taint", getCode(labStart), getCode(lab), getCode(var));
    }

}
