package ch.securify.dslpatterns;


import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.datalogpattern.DatalogElem;
import ch.securify.dslpatterns.datalogpattern.DatalogRule;
import ch.securify.dslpatterns.predicates.*;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.InvalidPatternException;

import java.util.ArrayList;
import java.util.List;

/**
 * Translates patterns written in DSL into Datalog queries (strings)
 */
public class DSLToDatalogTranslator {

    //needed to get the right codes for the predicates
    private static DSLAnalysis analyzer;

    private static List<DatalogRule> rules;

    //this is the index of the rule we are currently working on
    private static int indexOfCurrWorkingRule;

    /**
     * Translates a pattern into a query, should be called only with a Some or an All
     * @param completePattern the pattern to be translated
     * @return teh String representing the pattern
     */
    public List<DatalogRule> translateInstructionPattern(AbstractDSLPattern completePattern, String ruleName) throws InvalidPatternException {
        if(!(completePattern instanceof All) && !(completePattern instanceof Some))
            throw new InvalidPatternException("Not an All or a Some");

        indexOfCurrWorkingRule = 0;

        rules = new ArrayList<>();
        //we create a new datalog rule with name the desired one and with label the label of the instruction which is quantified
        rules.add(new DatalogRule(ruleName, ((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr().getLabel()));

        //first of all we insert the quantified instruction in the datalog rule
        addElemToCurrRule(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr());

        //then we add the body of the pattern, on which it quantifies
        translateAndAdd(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedPattern());


        return rules;
    }

    /**
     * Adds one element to the rule we are currently working on
     * @param elem the elem to add
     */
    private void addElemToCurrRule(DatalogElem elem) {
        rules.get(indexOfCurrWorkingRule).getBody().addElement(elem);
    }

    private void translateAndAdd(AbstractDSLPattern patt) {

        if(patt instanceof DatalogElem)
            addElemToCurrRule((DatalogElem) patt);
        else if(patt instanceof Not)
            translNot((Not)patt);
        else if(patt instanceof And)
            transAnd((And)patt);
        else if(patt instanceof Some)
            transSome((Some) patt);
    }

    private void transSome(Some patt) {
        addElemToCurrRule(patt.getQuantifiedInstr());
        translateAndAdd(patt.getQuantifiedPattern());
    }

    private void transAnd(And patt) {
        patt.getPatterns().forEach((pattern) -> translateAndAdd(pattern));
    }

    private void translNot(Not not) {
        //todo sometimes the not is a full expression, need to do something more complicated and push it inside
        addElemToCurrRule((DatalogElem) not.getNegatedPattern());
    }

    public void setAnalyzer(DSLAnalysis analyzer) {
        this.analyzer = analyzer;
    }
}
