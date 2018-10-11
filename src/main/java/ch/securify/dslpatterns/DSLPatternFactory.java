package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.CallValue;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.instructions.DSLInstructionFactory;
import ch.securify.dslpatterns.instructions.DSLLabel;
import ch.securify.dslpatterns.predicates.PredicateFactory;

import java.util.ArrayList;
import java.util.List;

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

    private static void testPatterns() {

        DSLLabel l1 = new DSLLabel();
        DSLLabel l2 = new DSLLabel();
        DSLLabel l3 = new DSLLabel();
        DSLLabel l4 = new DSLLabel();
        Variable X = new Variable();

        AbstractDSLPattern pattern = all(instrFct.stop(l1),
                some(instrFct.dslgoto(l2, X, l3),
                        and(
                                eq(X, CallValue.class),
                                prdFct.follow(l2, l4),
                                not(eq(l3, l4)),
                                prdFct.mustFollow(l4, l1))));

        System.out.println(pattern.getStringRepresentation());
    }

    public static void main(String[] args) {
        testPatterns();
    }
}
