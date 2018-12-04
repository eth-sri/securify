package ch.securify.dslpatterns;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.patterns.AbstractPattern;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class DSLPatternResult {

    private String name;

    // Instructions that violate the pattern
    Collection<Instruction> violations = new LinkedList<Instruction>();

    // Instruction that MAY violate the pattern (warnings)
    Collection<Instruction> warnings = new LinkedList<Instruction>();

    // Instructions that do NOT violate the pattern (safe uses)
    Collection<Instruction> safe = new LinkedList<Instruction>();

    // Instructions that have conflicting pattern result (safe and violation)
    Collection<Instruction> conflicts = new LinkedList<Instruction>();

    public DSLPatternResult(String name, Collection<Instruction> violations, Collection<Instruction> warnings,
                            Collection<Instruction> safe, Collection<Instruction> conflicts) {
        this.name = name;
        this.violations = violations;
        this.warnings = warnings;
        this.safe = safe;
        this.conflicts = conflicts;
    }

    public String getName() {
        return name;
    }

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
