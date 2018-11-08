package ch.securify.dslpatterns;


import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.predicates.AbstractPredicate;
import ch.securify.dslpatterns.predicates.DetByVarTag;
import ch.securify.dslpatterns.predicates.MayDepOnLabelTag;
import ch.securify.dslpatterns.predicates.MayDepOnVarTag;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.patterns.AbstractPattern;

import java.util.List;

/**
 * Translates patterns written in DSL into Datalog queries (strings)
 */
public class DSLToDatalogTranslator {

    //needed to get the right codes for the predicates
    private DSLAnalysis analyzer;

    StringBuilder sb;

    /**
     * Translates a pattern into a query, should be called only with a Some or an All
     * @param completePattern the pattern to be translated
     * @return teh String representing the pattern
     */
    public String translateInstructionPattern(AbstractDSLPattern completePattern) throws InvalidPatternException {
        if(!(completePattern instanceof All) && !(completePattern instanceof Some))
            throw new InvalidPatternException("Not an All or a Some");

        sb = new StringBuilder();

        //first of all we require that instruction to be present
        sb.append(translateInstruction(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr()));

        sb.append(" , ");

        //then we append the body of the pattern, on which it quantifies
        sb.append(dispatchCorrectTranslator(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedPattern()));


        return sb.toString();
    }

    private String translateInstruction(AbstractDSLInstruction instr) {
        return instr.getStringRepresentation();
    }

    private String dispatchCorrectTranslator(AbstractDSLPattern patt) {
        if(patt instanceof Not)
            return translNot((Not)patt);
        else if(patt instanceof And)
            return transAnd((And)patt);
        else if(patt instanceof EqWithNumber)
            return transEqWithNumber((EqWithNumber)patt);
        else if(patt instanceof AbstractPredicate)
            return dispatchCorrectPredicateTranslator((AbstractPredicate) patt);

        return null;
    }

    private String dispatchCorrectPredicateTranslator(AbstractPredicate pred)
    {
        StringBuilder sba = new StringBuilder();
        if(pred instanceof MayDepOnVarTag) {
             sba.append("mayDepOn(");
             sba.append(((MayDepOnVarTag) pred).getVariable().getName());
             sba.append(" , ");
             sba.append(analyzer.getCode(((MayDepOnVarTag) pred).getClass()));
             sba.append(")");

             return sba.toString();
        }
        else if(pred instanceof MayDepOnLabelTag) {
            sba.append("mayDepOn(");
            sba.append(((MayDepOnLabelTag) pred).getLabel().getName());
            sba.append(" , ");
            sba.append(analyzer.getCode(((MayDepOnLabelTag) pred).getClass()));
            sba.append(")");

            return sba.toString();
        }

        return null;
    }

    private String transEqWithNumber(EqWithNumber eq) {
        return eq.getStringRepresentation();
    }

    private String transAnd(And patt) {
        StringBuilder sba = new StringBuilder();

        List<AbstractDSLPattern> patterns = patt.getPatterns();

        sba.append(dispatchCorrectTranslator(patterns.get(0)));

        for(int i = 1; i < patterns.size(); i++) {
            sba.append(" , ");
            sba.append(dispatchCorrectTranslator(patterns.get(i)));
        }

        return sba.toString();
    }

    private String translNot(Not not) {
        return "!(" + dispatchCorrectTranslator(not.getNegatedPattern()) + ")";
    }

    public void setAnalyzer(DSLAnalysis analyzer) {
        this.analyzer = analyzer;
    }
}
