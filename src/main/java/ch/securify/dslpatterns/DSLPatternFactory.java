package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.instructions.*;
import ch.securify.dslpatterns.predicates.*;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.List;


/**
 * The factory to create Patterns, can be called to generate the right objects
 */
public class DSLPatternFactory {

    public static All all(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern){
        return new All(quantifiedInstr, quantifiedPattern);
    }

    public static And and(List<AbstractDSLPattern> patterns) {
        return new And(patterns);
    }

    public static And and(AbstractDSLPattern... patterns) {
        ArrayList<AbstractDSLPattern> pattList = new ArrayList<>();

        for (AbstractDSLPattern pattIt : patterns) {
            pattList.add(pattIt);
        }

        return new And(pattList);
    }

    public static Eq eq(Variable v1, Variable v2) {
        return new Eq(v1, v2);
    }
    public static EqWithClass eq(Variable v1, Class classtype) {
        return new EqWithClass(v1, classtype);
    }
    public static EqWithLabel eq(DSLLabel l1, DSLLabel l2) {
        return new EqWithLabel(l1, l2);
    }
    public static EqWithNumber eq(Variable v1, Integer n) {
        return new EqWithNumber(v1, n);
    }


    public static Implies implies(AbstractDSLPattern lhs, AbstractDSLPattern rhs) {
        return new Implies(lhs, rhs);
    }

    public static Not not(AbstractDSLPattern negatedPattern) {
        return new Not(negatedPattern);
    }

    public static Or or(List<AbstractDSLPattern> patterns) {
        return new Or(patterns);
    }

    public static Or or(AbstractDSLPattern... patterns) {
        ArrayList<AbstractDSLPattern> pattList = new ArrayList<>();

        for (AbstractDSLPattern pattIt : patterns) {
            pattList.add(pattIt);
        }

        return new Or(pattList);
    }

    public static Some some(AbstractDSLInstruction quantifiedInstr, AbstractDSLPattern quantifiedPattern){
        return new Some(quantifiedInstr, quantifiedPattern);
    }

    public static InstructionDSLPattern instructionPattern(AbstractDSLInstruction quantifiedInstr,
                                                           AbstractDSLPattern quantifiedPattern) {
        return new InstructionDSLPattern(quantifiedInstr, quantifiedPattern);
    }

    public static GreaterThanComparison greaterThan(Variable amount, int i) {
        return new GreaterThanComparison(amount, i);
    }


    //Predicates

    public static Follow follow(DSLLabel l1, DSLLabel l2) {
        return new Follow(l1, l2);
    }

    public static MustFollow mustFollow(DSLLabel l1, DSLLabel l2) {
        return new MustFollow(l1, l2);
    }

    public static DetByVarTag detBy(Variable var, Class tag) {
        return new DetByVarTag(var, tag);
    }

    public static DetByVarVar detBy(Variable var1, Variable var2) {
        return new DetByVarVar(var1, var2);
    }

    public static MayDepOnLabelTag mayDepOn(DSLLabel l, Class tag) {
        return new MayDepOnLabelTag(l, tag);
    }

    public static MayDepOnVarVar mayDepOn(Variable var1, Variable var2) {
        return new MayDepOnVarVar(var1, var2);
    }

    public static MayDepOnVarTag mayDepOn(Variable v, Class tag) {
        return new MayDepOnVarTag(v, tag);
    }

    public static MayFollow mayFollow(DSLLabel l1, DSLLabel l2) {
        return new MayFollow(l1, l2);
    }

    public static IsConst isConst(Variable x) {
        return new IsConst(x);
    }

    public static IsArg isArg(Variable x, DSLLabel label) {
        return new IsArg(x, label);
    }

    public static HasValue hasValue(Variable var, Variable constant) {
        return new HasValue(var, constant);
    }


    //instructions

    public static DSLGoto dslgoto(DSLLabel label, Variable var, DSLLabel secondBranchLabel) {
        return new DSLGoto(label, var, secondBranchLabel);
    }

    public static DSLStop stop(DSLLabel label) {
        return new DSLStop(label);
    }

    public static DSLVirtualMethodHead virtualMethodHead(DSLLabel label) {
        return new DSLVirtualMethodHead(label);
    }

    public static NoArgsVirtualMethodHead noArgsVirtualMethodHead(DSLLabel label) {
        return new NoArgsVirtualMethodHead(label);
    }

    public static DSLCall call(DSLLabel label, Variable out, Variable gas, Variable amount) {
        return new DSLCall(label, out, gas, amount);
    }

    public static DSLSstore sstore(DSLLabel label, Variable offset, Variable var) {
        return new DSLSstore(label, offset, var);
    }

    public static DSLSload sload(DSLLabel label, Variable offset, Variable var) {
        return new DSLSload(label, offset, var);
    }

    public static OffsetToMemoryVar offsetToMemoryVar(Variable offsetVar, Variable memoryVar) {
        return new OffsetToMemoryVar(offsetVar, memoryVar);
    }

    public static OffsetToStorageVar offsetToStorageVar(Variable offsetVar, Variable storageVar) {
        return new OffsetToStorageVar(offsetVar, storageVar);
    }
}
