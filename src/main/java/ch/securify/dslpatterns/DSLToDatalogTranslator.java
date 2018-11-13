package ch.securify.dslpatterns;


import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.datalogpattern.*;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import com.sun.istack.internal.Pool;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Translates patterns written in DSL into Datalog queries (strings)
 */
public class DSLToDatalogTranslator {

    /**
     * These are the rules that need to be generated to handle negation and universal quantifier
     */
    private static List<DatalogRule> supportingRules;

    /**
     * Translates a pattern into a query, should be called only with a Some or an All
     * @param completePattern the pattern to be translated
     * @return teh String representing the pattern
     */
    public static List<DatalogRule> translateInstructionPattern(AbstractDSLPattern completePattern, String ruleName) throws InvalidPatternException {
        if(!(completePattern instanceof All) && !(completePattern instanceof Some))
            throw new InvalidPatternException("Not an All or a Some");

        supportingRules = new ArrayList<>();

        List<DatalogBody> newBodies = new ArrayList<>();

        AbstractDSLInstruction instr = ((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr();

        if(instr.getLabel() instanceof DSLLabelDC) {
            instr = instr.getCopy();
            instr.setLabel(new DSLLabel());
        }

        //we create a new datalog rule with name the desired one and with label the label of the instruction which is quantified
        DatalogHead head = new DatalogHead(ruleName, instr.getLabel());

        //first of all we insert the quantified instruction in the datalog rule
        newBodies.add(new DatalogBody(instr));

        //then we add the body of the pattern, on which it quantifies
        newBodies = collapseBodies(newBodies, translateIntoBodies(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedPattern()));

        List<DatalogRule> translatedRules = new ArrayList<>(newBodies.size() + supportingRules.size());

        newBodies.forEach((body) -> translatedRules.add(new DatalogRule(head, body)));

        translatedRules.addAll(supportingRules);

        return translatedRules;
    }

    private static List<DatalogBody> translateIntoBodies(AbstractDSLPattern patt) {
        List<DatalogBody> newBodies = new ArrayList<>();
        if(patt instanceof DatalogElem)
           newBodies.add(new DatalogBody((DatalogElem) patt));
        else if(patt instanceof Not)
            newBodies = collapseBodies(newBodies, translNot((Not)patt));
        else if(patt instanceof And)
            newBodies = collapseBodies(newBodies, transAnd((And)patt));
        else if(patt instanceof Some)
            newBodies = collapseBodies(newBodies, transSome((Some) patt));
        else if(patt instanceof Implies) {
            //a => b === (!a or b)
            List<AbstractDSLPattern> newPatterns = new ArrayList<>();
            newPatterns.add(new Not(((Implies) patt).getLhs()));
            newPatterns.add(((Implies) patt).getRhs());

            newBodies = translateOr(new Or(newPatterns));
        }

        return newBodies;
    }

    /**
     * Unifies the bodies into only one, usually useful where there is an or
     * @param oldBodies the bodies before the call
     * @param toBeAddedBodies the bodies to be added that are returned by the call
     * @return the collapsed bodies
     */
    private static List<DatalogBody> collapseBodies(List<DatalogBody> oldBodies, List<DatalogBody> toBeAddedBodies) {
        List<DatalogBody> newBodies = new ArrayList<>(oldBodies.size()*toBeAddedBodies.size());
        if(oldBodies.isEmpty()) {
            newBodies.addAll(toBeAddedBodies);
            return newBodies;
        }
        if(toBeAddedBodies.isEmpty()) {
            newBodies.addAll(oldBodies);
            return newBodies;
        }
        for(DatalogBody body : oldBodies) {
            for(DatalogBody toBeAdded : toBeAddedBodies) {
                newBodies.add(DatalogBody.collapseTwo(body, toBeAdded));
            }
        }

        return newBodies;
    }

    private static List<DatalogBody> transSome(Some patt) {
        List<DatalogBody> newBodies = new ArrayList<>();
        newBodies.add(new DatalogBody(patt.getQuantifiedInstr()));
        newBodies = collapseBodies(newBodies, translateIntoBodies(patt.getQuantifiedPattern()));

        return newBodies;
    }

    private static List<DatalogBody> transAnd(And patt) {
        List<DatalogBody> newBodies = new ArrayList<>();
        for(AbstractDSLPattern pattern : patt.getPatterns()) {
            newBodies = collapseBodies(newBodies, translateIntoBodies(pattern));
        }

        return newBodies;
    }

    private static List<DatalogBody> translNot(Not not) {

        AbstractDSLPattern negatedPattern = not.getNegatedPattern();
        List<DatalogBody> newBodies = new ArrayList<>();

        if(negatedPattern instanceof DatalogElem)
            newBodies.add(new DatalogBody(new DatalogNot((DatalogElem) negatedPattern)));
        else if(negatedPattern instanceof And) { //push negation inside
            List<AbstractDSLPattern> negatedPatterns = new ArrayList<>();
            ((And) negatedPattern).getPatterns().forEach((patt) -> negatedPatterns.add(new Not(patt)));
            newBodies = translateOr(new Or(negatedPatterns));
        }
        else if(negatedPattern instanceof Or) {
            List<AbstractDSLPattern> negatedPatterns = new ArrayList<>();
            ((And) negatedPattern).getPatterns().forEach((patt) -> negatedPatterns.add(new Not(patt)));
            newBodies = transAnd(new And(negatedPatterns));
        }
        else if(negatedPattern instanceof Implies) {
            //a => b === (!a or b)
            //!(!a or b) === (a and !b)
            List<AbstractDSLPattern> newPatterns = new ArrayList<>();
            newPatterns.add(((Implies) negatedPattern).getLhs());
            newPatterns.add(new Not(((Implies) negatedPattern).getRhs()));

            newBodies = transAnd(new And(newPatterns));
        }
        else if(negatedPattern instanceof Some) {
            newBodies = transAll(new All(((Some) negatedPattern).getQuantifiedInstr(),
                    new Not(((Some) negatedPattern).getQuantifiedPattern())));
        }
        else if(negatedPattern instanceof All) {
            newBodies = transSome(new Some(((All) negatedPattern).getQuantifiedInstr(),
                    new Not(((All) negatedPattern).getQuantifiedPattern())));
        }

        return newBodies;
    }

    private static List<DatalogBody> transAll(All all) {



        return null;
    }

    private static List<DatalogBody> translateOr(Or or) {
        List<AbstractDSLPattern> patterns = or.getPatterns();
        List<DatalogBody> newBodies = new ArrayList<>(patterns.size());
        for(AbstractDSLPattern patt : patterns) {
            newBodies.addAll(translateIntoBodies(patt));
        }

        return newBodies;
    }
}
