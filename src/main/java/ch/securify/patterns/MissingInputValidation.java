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
import ch.securify.decompiler.instructions.*;

import java.util.*;

public class MissingInputValidation extends AbstractInstructionPattern {

    public MissingInputValidation() {
        super(new PatternDescription("InsecureCodingPatterns",
                MissingInputValidation.class,
                "Missing Input Validation",
                "Method arguments must be sanitized before they are used in computations.",
                PatternDescription.Severity.Medium,
                PatternDescription.Type.Trust));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        return instr instanceof _VirtualMethodHead;
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        Variable[] args = instr.getOutput();

        for (Variable arg : args) {
            if (!arg.getValueTypes().contains(CallDataLoad.class)) {
                continue;
            }

            for (Instruction useInstr : methodInstructions) {
                if (!(useInstr instanceof SStore
                        || useInstr instanceof SLoad
                        || useInstr instanceof MStore
                        || useInstr instanceof MLoad
                        || useInstr instanceof Sha3
                        || useInstr instanceof Call))
                    continue;

                for (Variable var : useInstr.getInput()) {
                    if (dataflow.varMustDepOn(useInstr, var, arg) == Status.SATISFIABLE) {
                        boolean varMayBeChecked = false;
                        for (Instruction checkInstr : methodInstructions) {
                            if (checkInstr instanceof JumpI) {
                                if (dataflow.mayFollow(checkInstr, useInstr) == Status.SATISFIABLE) {
                                    Variable cond = ((JumpI) checkInstr).getCondition();
                                    if (dataflow.varMayDepOn(checkInstr, cond, arg) == Status.SATISFIABLE) {
                                        varMayBeChecked = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!varMayBeChecked) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        Variable[] args = instr.getOutput();

        for (Variable arg : args) {
            if (!arg.getValueTypes().contains(CallDataLoad.class)) {
                continue;
            }

            for (Instruction useInstr : methodInstructions) {
                if (!(useInstr instanceof SStore
                        || useInstr instanceof SLoad
                        || useInstr instanceof MStore
                        || useInstr instanceof MLoad
                        || useInstr instanceof Sha3
                        || useInstr instanceof Call))
                    continue;

                for (Variable var : useInstr.getInput()) {
                    if (dataflow.varMayDepOn(useInstr, var, arg) == Status.SATISFIABLE) {
                        boolean varChecked = false;
                        for (Instruction checkInstr : methodInstructions) {
                            if (checkInstr instanceof JumpI) {
                                if (dataflow.mustPrecede(checkInstr, useInstr) == Status.SATISFIABLE) {
                                    Variable cond = ((JumpI) checkInstr).getCondition();
                                    if (dataflow.varMustDepOn(checkInstr, cond, arg) == Status.SATISFIABLE) {
                                        varChecked = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!varChecked)
                            return false;
                    }
                }
            }
        }
        return true;
    }
}
