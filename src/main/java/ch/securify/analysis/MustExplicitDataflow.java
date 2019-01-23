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

import java.io.IOException;
import java.util.*;

import ch.securify.decompiler.instructions.*;

import ch.securify.decompiler.Variable;

public class MustExplicitDataflow extends AbstractDataflow {
    static final public String binaryName = "mustExplicit";

    public MustExplicitDataflow(List<Instruction> decompiledInstructions) throws IOException, InterruptedException {
        instructions = decompiledInstructions;
        initDataflow(binaryName);
    }

    @Override
    public int mustPrecede(Instruction instr1, Instruction instr2) {
        return runQuery("mustPrecede", getCode(instr1), getCode(instr2));
    }

    @Override
    public int mayFollow(Instruction instr1, Instruction instr2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int varMustDepOn(Instruction instr1, Variable lhs, Object type) {
        return runQuery("reach", getCode(instr1), getCode(lhs), getCode(type));
    }

    public int varMayDepOn(Instruction instr1, Variable lhs, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int memoryMayDepOn(Instruction instr1, int offset, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int memoryMayDepOn(Instruction instr, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int memoryMustDepOn(Instruction instr, int offset, Object type) {
        return runQuery("memory", getCode(instr), getCode(getMemoryVarForIndex(offset)), getCode(type));
    }

    @Override
    public int instrMayDepOn(Instruction instr, Object type) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void deriveFollowsPredicates() {
        log(">> Derive follows predicates <<");
        for (Instruction instr : instructions) {

            if (instr instanceof JumpDest) {
                if (((JumpDest) instr).getIncomingBranches().size() == 1 && instr.getPrev() == null) {
                    log("One-Branch Tag fact: " + instr);
                    appendRule("oneBranchTag", getCode(instr));
                }
                log("Tag fact: " + instr);
                appendRule("tag", getCode(instr));
            }

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
    protected void createSLoadRule(Instruction instr, Variable index, Variable var) {
        int indexCode;
        if (index.hasConstantValue()) {
            indexCode = getCode(getInt(index.getConstantValue()));
        } else {
            indexCode = unk;
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
        }
        appendRule("mload", getCode(instr), offsetCode, getCode(var));
    }

    @Override
    protected void deriveIfPredicates() {
        log(">> Derive TaintElse and TaintThen predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof JumpI) {
                JumpI ifInstr = (JumpI) instr;
                Instruction mergeInstr = ifInstr.getMergeInstruction();

                if (mergeInstr != null) {
                    log("merge instruction: " + mergeInstr.getStringRepresentation());
                    createEndIfRule(instr, mergeInstr);
                }
            }
        }
    }

    private void createFollowsRule(Instruction from, Instruction to) {
        if (from instanceof JumpI) {
            Instruction mergeInstruction = ((JumpI)from).getMergeInstruction();
            if (mergeInstruction == null) {
                mergeInstruction = new JumpDest("BLACKHOLE");
            }
            if (!(to instanceof JumpDest)) {
                appendRule("follows", getCode(from), getCode(to));
            }
            appendRule("jump", getCode(from), getCode(to), getCode(mergeInstruction));
        } else if (from instanceof Jump) {
            // need to use a jump, not follows because follows ignores the TO if it is of type Tag, see Datalog rules
            appendRule("jump", getCode(from), getCode(to), getCode(to));
        } else {
            appendRule("follows", getCode(from), getCode(to));
        }

        if (to instanceof JumpDest) {
            //appendRule("join", getCode(from), getCode(to));
            List<Instruction> incomingBranches = new ArrayList<>(((JumpDest) to).getIncomingBranches());
            if (to.getPrev() != null) {
                incomingBranches.add(to.getPrev());
            }
            log("JumpDest: " + to + " with incoming branches: " + incomingBranches);

//            if (incomingBranches.size() == 1) {
//                //
//                appendRule("jump", getCode(incomingBranches.get(0)), getCode(to), getCode(new JumpDest("BLACKHOLE"))    );
//            } else {
                Instruction lastJoinInstruction = incomingBranches.get(0);
                for (int i = 1; i < incomingBranches.size() - 1; ++i) {
                    Instruction tmpJoinInstruction = new JumpDest(to.toString() + "_tmp_" + i);
                    appendRule("join", getCode(lastJoinInstruction),
                            getCode(incomingBranches.get(i)),
                            getCode(tmpJoinInstruction));
                    lastJoinInstruction = tmpJoinInstruction;
                }
                appendRule("join", getCode(lastJoinInstruction),
                        getCode(incomingBranches.get(incomingBranches.size()-1)),
                        getCode(to));
//            }
        }
    }

}
