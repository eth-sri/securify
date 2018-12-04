package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Balance;
import ch.securify.decompiler.instructions.CallValue;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.dslpatterns.datalogpattern.DatalogRule;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.instructions.DSLInstructionFactory;
import ch.securify.dslpatterns.tags.DSLArg;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.predicates.PredicateFactory;
import ch.securify.dslpatterns.tags.DSLMsgdata;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.dslpatterns.util.VariableDC;
import com.sun.org.apache.xpath.internal.Arg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * The factory to create Patterns, can be called to generate the right objects
 */
public class DSLPatternFactory {

    private static DSLInstructionFactory instrFct = new DSLInstructionFactory();
    private static PredicateFactory prdFct = new PredicateFactory();

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
}
