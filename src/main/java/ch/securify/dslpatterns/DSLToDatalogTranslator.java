package ch.securify.dslpatterns;


import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.*;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.instructions.DSLVirtualMethodHead;
import ch.securify.dslpatterns.predicates.*;
import ch.securify.dslpatterns.tags.DSLArg;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.dslpatterns.util.VariableArg;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Translates patterns written in DSL into Datalog queries (strings)
 */
public class DSLToDatalogTranslator {

    private static int nextTmpPredId;
    static { resetLabelNameGenerator(); }

    private static synchronized String generateTmpPredName() {
        StringBuilder sb = new StringBuilder();


        int varId = nextTmpPredId;
        do {
            char letter = (char) ('A' + (varId % 26));
            sb.append(letter);
            varId /= 26;
        } while (varId > 0);
        nextTmpPredId++;
        sb.append("derPpmt");
        return sb.reverse().toString();
    }

    /**
     * Reset the naming of tmp to start again from 'a'.
     */
    public static void resetLabelNameGenerator() {
        nextTmpPredId = 0;
    }

    /**
     * These are the rules that need to be generated to handle negation and universal quantifier
     */
    private static List<DatalogRule> supportingRules;

    private static Set<Variable> encounteredVars;
    private static Set<DSLLabel> encounteredLabels;

    private static VariableArg varArg = new VariableArg();

    private static DatalogHead head;

    /**
     * Translates a pattern into a query, should be called only with a Some or an All
     * @param completePattern the pattern to be translated
     * @return the list of datalog rules representing the pattern
     */
    public static List<DatalogRule> translateInstructionPattern(AbstractDSLPattern completePattern, String ruleName) throws InvalidPatternException {
        if(!(completePattern instanceof InstructionDSLPattern))
            throw new InvalidPatternException("The outer class is not an instruction pattern class");

        supportingRules = new ArrayList<>();
        encounteredLabels = new HashSet<>();
        encounteredVars = new HashSet<>();

        List<DatalogBody> newBodies = new ArrayList<>();

        AbstractDSLInstruction instr = ((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr();

        if(instr.getLabel() instanceof DSLLabelDC) {
            instr = instr.getCopy();
            instr.setLabel(new DSLLabel());
        }

        //we create a new datalog rule with name the desired one and with label the label of the instruction which is quantified
        head = new DatalogHead(ruleName, instr.getLabel());
        encounteredLabels.addAll(instr.getLabels());
        encounteredVars.addAll(instr.getVariables());

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
        if(patt instanceof DatalogElem) {
            encounteredLabels.addAll(patt.getLabels());
            encounteredVars.addAll(patt.getVariables());

            newBodies = collapseBodies(newBodies, handleArgTag((DatalogElem) patt, false));
        }
        else if(patt instanceof Not)
            newBodies = collapseBodies(newBodies, translateNot((Not)patt));
        else if(patt instanceof And)
            newBodies = collapseBodies(newBodies, translateAnd((And)patt));
        else if(patt instanceof Some)
            newBodies = collapseBodies(newBodies, translateSome((Some) patt));
        else if(patt instanceof Implies) {
            //a => b === (!a or b)
            List<AbstractDSLPattern> newPatterns = new ArrayList<>();
            newPatterns.add(new Not(((Implies) patt).getLhs()));
            newPatterns.add(((Implies) patt).getRhs());

            newBodies = translateOr(new Or(newPatterns));
        }
        else if(patt instanceof All)
            newBodies = translateAll((All) patt);
        else if(patt instanceof Or)
            newBodies = translateOr((Or) patt);

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

    private static List<DatalogBody> translateSome(Some patt) {
        List<DatalogBody> newBodies = new ArrayList<>();
        newBodies.add(new DatalogBody(patt.getQuantifiedInstr()));
        newBodies = collapseBodies(newBodies, translateIntoBodies(patt.getQuantifiedPattern()));

        return newBodies;
    }

    private static List<DatalogBody> translateAnd(And patt) {
        List<DatalogBody> newBodies = new ArrayList<>();
        for(AbstractDSLPattern pattern : patt.getPatterns()) {
            newBodies = collapseBodies(newBodies, translateIntoBodies(pattern));
        }

        return newBodies;
    }

    private static List<DatalogBody> handleArgTag(DatalogElem patt, boolean toBeNegated) {
        List<DatalogBody> result = new ArrayList<>();
        DatalogBody newBody = new DatalogBody();

            if (patt instanceof AbstractPredicate) {
                Set<Class> tagsList = ((AbstractPredicate) patt).getTags();
                if (tagsList.contains(DSLArg.class)) {

                    AbstractPredicate newPredicate;
                    //exchange the tag predicate with the variable one
                    if(patt instanceof DetByVarTag)
                        newPredicate = new DetByVarVar(((DetByVarTag) patt).getVar(), varArg);
                    else if(patt instanceof MayDepOnVarTag)
                        newPredicate = new MayDepOnVarVar(((MayDepOnVarTag) patt).getVar(), varArg);
                    else
                        newPredicate = (AbstractPredicate) patt;

                    if(toBeNegated)
                        newBody.addElement(new DatalogNot(newPredicate));
                    else
                        newBody.addElement(newPredicate);

                    DSLLabel connectingLabel = new DSLLabel();
                    connectingLabel.setName("connLabl");

                    DSLLabel virtualMethodHeadLabel = new DSLLabel();
                    virtualMethodHeadLabel.setName("virtMethdHeadLabl");

                    newBody.addElement(new IsArg(varArg, connectingLabel));
                    newBody.addElement(new MayFollow(connectingLabel, virtualMethodHeadLabel));
                    newBody.addElement(new DSLVirtualMethodHead(virtualMethodHeadLabel));


                    //if there are no arguments then it must also be true
                    DatalogBody noArgsBody = new DatalogBody();

                    if(toBeNegated)
                        noArgsBody.addElement(new NoArgsVirtualMethodHead(virtualMethodHeadLabel));
                    else
                        noArgsBody.addElement(new DatalogNot(new NoArgsVirtualMethodHead(virtualMethodHeadLabel)));
                    noArgsBody.addElement(new MayFollow(head.getLabel(), virtualMethodHeadLabel));

                    result.add(noArgsBody);
                }
                else {
                    if(toBeNegated)
                        newBody.addElement(new DatalogNot(patt));
                    else
                        newBody.addElement(patt);
                }
            }
            else {
                if(toBeNegated)
                    newBody.addElement(new DatalogNot(patt));
                else
                    newBody.addElement(patt);
            }

            result.add(newBody);
            return result;
    }

    private static List<DatalogBody> translateNot(Not not) {

        AbstractDSLPattern negatedPattern = not.getNegatedPattern();
        List<DatalogBody> newBodies = new ArrayList<>();

        if(negatedPattern instanceof DatalogElem) {
            encounteredLabels.addAll(negatedPattern.getLabels());
            encounteredVars.addAll(negatedPattern.getVariables());

            newBodies = collapseBodies(newBodies, handleArgTag((DatalogElem) negatedPattern, true));        }
        else if(negatedPattern instanceof And) { //push negation inside
            List<AbstractDSLPattern> negatedPatterns = new ArrayList<>();
            ((And) negatedPattern).getPatterns().forEach((patt) -> negatedPatterns.add(new Not(patt)));
            newBodies = translateOr(new Or(negatedPatterns));
        }
        else if(negatedPattern instanceof Or) {
            List<AbstractDSLPattern> negatedPatterns = new ArrayList<>();
            ((And) negatedPattern).getPatterns().forEach((patt) -> negatedPatterns.add(new Not(patt)));
            newBodies = translateAnd(new And(negatedPatterns));
        }
        else if(negatedPattern instanceof Implies) {
            //a => b === (!a or b)
            //!(!a or b) === (a and !b)
            List<AbstractDSLPattern> newPatterns = new ArrayList<>();
            newPatterns.add(((Implies) negatedPattern).getLhs());
            newPatterns.add(new Not(((Implies) negatedPattern).getRhs()));

            newBodies = translateAnd(new And(newPatterns));
        }
        else if(negatedPattern instanceof Some) {
            newBodies = translateAll(new All(((Some) negatedPattern).getQuantifiedInstr(),
                    new Not(((Some) negatedPattern).getQuantifiedPattern())));
        }
        else if(negatedPattern instanceof All) {
            newBodies = translateSome(new Some(((All) negatedPattern).getQuantifiedInstr(),
                    new Not(((All) negatedPattern).getQuantifiedPattern())));
        }
        else if(negatedPattern instanceof Not){
            newBodies = translateIntoBodies(((Not) negatedPattern).getNegatedPattern());
        }

        return newBodies;
    }

    private static List<DatalogBody> translateAll(All all) {
        Set<DSLLabel> labelsInAll = new HashSet<>();
        Set<Variable> varsInAll = new HashSet<>();

        //get all the labels and vars that are part of the quantified instruction
        labelsInAll.addAll(all.getQuantifiedInstr().getLabels());
        varsInAll.addAll(all.getQuantifiedInstr().getVariables());

        //get all the labels and the vars which are part of the body of the all
        labelsInAll.addAll(all.getQuantifiedPattern().getLabels());
        varsInAll.addAll(all.getQuantifiedPattern().getVariables());

        //we are interested in the intersection of the labels we have already encountered in our translation
        //and the ones that are in the all. This intersection will consist in the predicate head

        Set<DSLLabel> labelsInters = new HashSet<>(labelsInAll); // use the copy constructor
        labelsInters.retainAll(encounteredLabels);

        Set<Variable> varsInters = new HashSet<>(varsInAll); // use the copy constructor
        varsInters.retainAll(encounteredVars);

        //build the datalog rule for the supporting / temporary predicate (we need to negate it)
        List<DatalogBody> translationOfBody = translateIntoBodies(new Not(all.getQuantifiedPattern()));

        List<Variable> varsInternsList = new ArrayList<>(varsInters);
        List<DSLLabel> labelsIntensList = new ArrayList<>(labelsInters);

        String tmpPredicateName = generateTmpPredName();

        DatalogHead tmpPredHead = new DatalogHead(tmpPredicateName, labelsIntensList, varsInternsList);

        translationOfBody.forEach((body) -> {
            body.addElementInFront(all.getQuantifiedInstr());
            supportingRules.add(new DatalogRule(tmpPredHead, body));
        });

        //create the new temporary predicate for the "upper" rule
        AdditionalTmpPredicate tmpPred = new AdditionalTmpPredicate(tmpPredicateName,
                varsInternsList,
                labelsIntensList);

        //put it into a Body negated
        DatalogBody newBody = new DatalogBody(new DatalogNot(tmpPred));
        List<DatalogBody> newBodies = new ArrayList<>(1);
        newBodies.add(newBody);

        return newBodies;
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
