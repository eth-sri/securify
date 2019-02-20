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
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.JumpI;

public class UnhandledException extends AbstractInstructionPattern {

    public UnhandledException() {
        super(new PatternDescription("InsecureCodingPatterns",
                UnhandledException.class,
                "Unhandled Exception",
                "The return value of statements that may return error values must be explicitly checked.",
                PatternDescription.Severity.High,
                PatternDescription.Type.Security));
    }

    @Override
    protected boolean applicable(Instruction instr, AbstractDataflow dataflow) {
        return instr instanceof Call && !((Call) instr).isBuiltInContractCall();
    }

    @Override
    protected boolean isViolation(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        if (instr instanceof Call) {
            for (Instruction otherInstr : methodInstructions) {
                if (otherInstr instanceof JumpI) {
                    if (dataflow.mayFollow(instr, otherInstr) == Status.SATISFIABLE) {
                        if (dataflow.varMayDepOn(otherInstr, ((JumpI) otherInstr).getCondition(), instr.getOutput()[0]) == Status.SATISFIABLE) {
                            return false;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean isCompliant(Instruction instr, List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractDataflow dataflow) {
        if (instr instanceof Call) {
            for (Instruction otherInstr : methodInstructions) {
                if (otherInstr instanceof JumpI) {
                    if (dataflow.mustPrecede(instr, otherInstr) == Status.SATISFIABLE) {
                        // Assumes we tag output variables of Calls with themselves
                        if (dataflow.varMustDepOn(otherInstr, ((JumpI) otherInstr).getCondition(), instr.getOutput()[0]) == Status.SATISFIABLE) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

}
