package ch.securify.dslpatterns;


import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
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
    private DSLAnalysis analyzer;

    /**
     * It's the return type of many methods and contains the string representation,
     * the labels contained in the pattern and the variables
     */
    private class RepLabelsAndVars {
        private StringBuilder rep;
        private List<Variable> vars;
        private List<DSLLabel> labels;

        public RepLabelsAndVars() {
            this.rep = new StringBuilder();
            this.vars = new ArrayList<>();
            this.labels = new ArrayList<>();
        }

        public String getRep() {
            return rep.toString();
        }

        public List<Variable> getVars() {
            return vars;
        }

        public List<DSLLabel> getLabels() {
            return labels;
        }

        public void appendToRep(String str) {
            rep.append(str);
        }

        public void appendToRep(int str) {
            rep.append(str);
        }

        public void addLabel(DSLLabel l) {
            labels.add(l);
        }

        public void addVar(Variable var) {
            vars.add(var);
        }

        public void addAllVar(List<Variable> vars) {
            vars.addAll(vars);
        }

        public void addAllLabel(List<DSLLabel> l) {
            labels.addAll(l);
        }

        public void appendAll(RepLabelsAndVars toAppend) {
            rep.append(toAppend.getRep());
            addAllVar(toAppend.getVars());
            addAllLabel(toAppend.getLabels());
        }
    }

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

        //firs of all we insert the quantified instruction in the datalog rule
        sb.append(translateInstruction(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedInstr()).getRep());

        sb.append(" , ");

        //then we append the body of the pattern, on which it quantifies
        sb.append(dispatchCorrectTranslator(((AbstractQuantifiedDSLPattern) completePattern).getQuantifiedPattern()));


        return sb.toString();
    }

    private RepLabelsAndVars translateInstruction(AbstractDSLInstruction instr) {
        RepLabelsAndVars returnVal = new RepLabelsAndVars();
        returnVal.appendToRep(instr.getStringRepresentation());
        returnVal.addAllLabel(instr.getAllLabels());
        returnVal.addAllVar(instr.getAllVars());

        return returnVal;
    }

    private RepLabelsAndVars dispatchCorrectTranslator(AbstractDSLPattern patt) {
        if(patt instanceof Not)
            return translNot((Not)patt);
        else if(patt instanceof And)
            return transAnd((And)patt);
        else if(patt instanceof EqWithNumber)
            return transEqWithNumber((EqWithNumber)patt);
        else if(patt instanceof AbstractPredicate)
            return dispatchCorrectPredicateTranslator((AbstractPredicate) patt);
        else if(patt instanceof Some)
            return transSome((Some) patt);

        return null;
    }

    private RepLabelsAndVars transSome(Some patt) {
        RepLabelsAndVars returnVal = new RepLabelsAndVars();

        returnVal.appendAll(translateInstruction(patt.getQuantifiedInstr()));
        returnVal.appendToRep(" , ");
        returnVal.appendAll(dispatchCorrectTranslator(patt.getQuantifiedPattern()));

        return returnVal;
    }

    private RepLabelsAndVars dispatchCorrectPredicateTranslator(AbstractPredicate pred)
    {
        RepLabelsAndVars returnVal = new RepLabelsAndVars();
        if(pred instanceof MayDepOnVarTag) {
            Variable var = ((MayDepOnVarTag) pred).getVariable();

            returnVal.appendToRep("mayDepOn(");
            returnVal.appendToRep(var.getName());
            returnVal.appendToRep(" , ");
            returnVal.appendToRep(analyzer.getCode(((MayDepOnVarTag) pred).getTag()));
            returnVal.appendToRep(")");

            returnVal.addVar(var);
            return returnVal;
        }
        else if(pred instanceof MayDepOnLabelTag) {
            DSLLabel l = ((MayDepOnLabelTag) pred).getLabel();

            returnVal.appendToRep("mayDepOn(");
            returnVal.appendToRep(l.getName());
            returnVal.appendToRep(" , ");
            returnVal.appendToRep(analyzer.getCode(((MayDepOnLabelTag) pred).getTag()));
            returnVal.appendToRep(")");

            returnVal.addLabel(l);
            return returnVal;
        }
        else if(pred instanceof MustFollow) {
            //todo: is this really ok? is mustPrecede ok?
            DSLLabel l1 = ((MustFollow) pred).getL1();
            DSLLabel l2 = ((MustFollow) pred).getL2();
            returnVal.appendToRep("mustPrecede(");
            returnVal.appendToRep(l1.getName());
            returnVal.appendToRep(" , ");
            returnVal.appendToRep(l2.getName());
            returnVal.appendToRep(")");

            returnVal.addLabel(l1);
            returnVal.addLabel(l2);

            return returnVal;
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
