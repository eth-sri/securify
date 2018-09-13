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
import ch.securify.decompiler.instructions.Instruction;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractPattern {

    // Instructions that violate the pattern
    Collection<Instruction> violations = new LinkedList<Instruction>();

    // Instruction that MAY violate the pattern (warnings)
    Collection<Instruction> warnings = new LinkedList<Instruction>();

    // Instructions that do NOT violate the pattern (safe uses)
    Collection<Instruction> safe = new LinkedList<Instruction>();

    // Instructions that have conflicting pattern result (safe and violation)
    Collection<Instruction> conflicts = new LinkedList<Instruction>();

    /**
     * @param instructions : instructions to be checked
     * @param allInstructions
     * @param dataflow : dataflow facts
     */
	public abstract void checkPattern(List<Instruction> instructions, List<Instruction> allInstructions, AbstractDataflow dataflow);

    /**
     * @return instructions that match the pattern (violations)
     */
    public Collection<Instruction> getViolations() {
        return violations;
    }

    /**
     * @return instructions that may match the pattern (warnings)
     */
    public Collection<Instruction> getWarnings() {
        return warnings;
    }

    /**
     * @return instructions that do not match the pattern (compliance)
     */
    public Collection<Instruction> getSafe() {
        return safe;
    }

    /**
     * @return instructions with that match and do not match the pattern (conflict, should be the empty set)
     */
    public Collection<Instruction> getConflicts() {
        return conflicts;
    }

    protected void addViolation(Instruction instr) {
        violations.add(instr);
    }

    protected void addWarning(Instruction instr) {
        warnings.add(instr);
    }

    protected void addSafe(Instruction instr) {
        safe.add(instr);
    }

    protected void addConflict(Instruction instr) {
        conflicts.add(instr);
    }

    public boolean hasViolations() {
        return violations.size() > 0;
    }

    public boolean hasWarnings() {return warnings.size() > 0; }

    public boolean hasSafe() {return safe.size() > 0; }

    public boolean hasConflicts() {return conflicts.size() > 0; }
}
