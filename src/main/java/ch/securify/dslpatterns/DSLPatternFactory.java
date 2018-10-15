package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Balance;
import ch.securify.decompiler.instructions.CallValue;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.instructions.DSLInstructionFactory;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.predicates.PredicateFactory;
import ch.securify.dslpatterns.tags.DSLMsgdata;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.VariableDC;
import com.sun.org.apache.xpath.internal.Arg;

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

    /**
     * Just a test method, todo: remove
     * Some know issues / things to be discussed:
     * Still a bit of a mixture:
     * - Variable class is from decompiler
     * - SLoad.class is taken from the decompiler, but the DSLSLoad class is recreated
     * - added the Arg class
     */
    private static void testPatterns() {

        DSLLabel l1 = new DSLLabel();
        DSLLabel l2 = new DSLLabel();
        DSLLabel l3 = new DSLLabel();
        DSLLabel l4 = new DSLLabel();
        DSLLabelDC dcLabel = new DSLLabelDC();
        VariableDC dcVar = new VariableDC();
        Variable X = new Variable();
        Variable Y = new Variable();
        Variable amount = new Variable();


        System.out.println(" *** LQ - Ether liquidity");
        AbstractDSLPattern patternComplianceOneLQ = all(instrFct.stop(l1),
                some(instrFct.dslgoto(l2, X, l3),
                        and(
                                eq(X, CallValue.class),
                                prdFct.follow(l2, l4),
                                not(eq(l3, l4)),
                                prdFct.mustFollow(l4, l1))));

        System.out.println(patternComplianceOneLQ.getStringRepresentation());

        AbstractDSLPattern patternComplianceTwoLQ = some(instrFct.call(l1, dcVar, dcVar, amount),
                or(not(eq(amount, 0)), prdFct.detBy(amount, DSLMsgdata.class)));

        System.out.println(patternComplianceTwoLQ.getStringRepresentation());

        AbstractDSLPattern patternViolationLQ = and(
                some(instrFct.stop(l1),
                        not(prdFct.mayDepOn(l1, CallValue.class))),
                all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                        eq(amount, 0)));

        System.out.println(patternViolationLQ.getStringRepresentation());

        System.out.println(" *** NW - No writes after call");
        AbstractDSLPattern patternComplianceNW = all(
          instrFct.call(l1, dcVar, dcVar, dcVar),
          all(instrFct.sstore(l2, dcVar, dcVar),
                  not(prdFct.mayFollow(l1, l2)))
        );

        System.out.println(patternComplianceNW.getStringRepresentation());

        AbstractDSLPattern patternViolationNW = some(
                instrFct.call(l1, dcVar, dcVar, dcVar),
                some(instrFct.sstore(l2, dcVar, dcVar),
                        not(prdFct.mayFollow(l1, l2)))
        );

        System.out.println(patternViolationNW.getStringRepresentation());

        System.out.println(" *** RW - restricted write");
        AbstractDSLPattern patternComplianceRW = all(instrFct.sstore(dcLabel, X, dcVar),
                prdFct.detBy(X, Caller.class));
        System.out.println(patternComplianceRW.getStringRepresentation());

        AbstractDSLPattern patternViolationRW = some(instrFct.sstore(l1, X, dcVar),
                and(not(prdFct.mayDepOn(X, Caller.class)), not(prdFct.mayDepOn(l1, Caller.class))));
        System.out.println(patternViolationRW.getStringRepresentation());

        System.out.println(" *** RT - restricted write");
        AbstractDSLPattern patternComplianceRT = all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                eq(amount, 0));
        System.out.println(patternComplianceRT.getStringRepresentation());

        AbstractDSLPattern patternViolationRT = some(instrFct.call(l1, dcVar, dcVar, amount),
                and(prdFct.detBy(amount, DSLMsgdata.class),
                        not(prdFct.mayDepOn(l1, Caller.class)),
                        not(prdFct.mayDepOn(l1, DSLMsgdata.class))));
        System.out.println(patternViolationRT.getStringRepresentation());

        System.out.println(" *** HE - handled exception");
        AbstractDSLPattern patternComplianceHE = all(instrFct.call(l1, Y, dcVar, dcVar),
                some(instrFct.dslgoto(l2, X, dcLabel),
                        and(prdFct.mustFollow(l1, l2), prdFct.detBy(X, Y))));
        System.out.println(patternComplianceHE.getStringRepresentation());

        AbstractDSLPattern patternViolationHE = some(instrFct.call(l1, Y, dcVar, dcVar),
                all(instrFct.dslgoto(l2, X, dcLabel),
                        implies(prdFct.mayFollow(l1, l2), not(prdFct.mayDepOn(X, Y)))));
        System.out.println(patternViolationHE.getStringRepresentation());

        System.out.println(" *** TOD - transaction ordering dependency");
        AbstractDSLPattern patternComplianceTOD = all(instrFct.call(dcLabel, dcVar, dcVar, amount),
               and(not(prdFct.mayDepOn(amount, SLoad.class)), not(prdFct.mayDepOn(amount, Balance.class))));
        System.out.println(patternComplianceTOD.getStringRepresentation());

        AbstractDSLPattern patternViolationTOD = some(instrFct.call(dcLabel, dcVar, dcVar, amount),
                some(instrFct.sload(dcLabel, Y, X),
                        some(instrFct.sstore(dcLabel, X, dcVar),
                                and(prdFct.detBy(amount, Y), prdFct.isConst(X)))));
        System.out.println(patternViolationTOD.getStringRepresentation());

        System.out.println(" *** VA - validated arguments");
        AbstractDSLPattern patternComplianceVA = all(instrFct.sstore(l1, dcVar, X),
                implies(prdFct.mayDepOn(X, Arg.class),
                        some(instrFct.dslgoto(l2, Y, dcLabel),
                                and(prdFct.mustFollow(l2, l1),
                                        prdFct.detBy(Y, Arg.class)))));
        System.out.println(patternComplianceVA.getStringRepresentation());

        AbstractDSLPattern patternViolationVA = some(instrFct.sstore(l1, dcVar, X),
                implies(prdFct.mayDepOn(X, Arg.class),
                        not(some(instrFct.dslgoto(l2, Y, dcLabel),
                                and(prdFct.mustFollow(l2, l1),
                                        prdFct.mayDepOn(Y, Arg.class))))));
        System.out.println(patternViolationVA.getStringRepresentation());
    }

    public static void main(String[] args) {
        testPatterns();
    }
}
