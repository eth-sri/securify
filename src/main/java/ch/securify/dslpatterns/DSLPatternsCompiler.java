package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Balance;
import ch.securify.decompiler.instructions.CallValue;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.dslpatterns.datalogpattern.DatalogRule;
import ch.securify.dslpatterns.instructions.DSLInstructionFactory;
import ch.securify.dslpatterns.predicates.PredicateFactory;
import ch.securify.dslpatterns.tags.DSLMsgdata;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.dslpatterns.util.VariableDC;
import com.sun.org.apache.xpath.internal.Arg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DSLPatternsCompiler {

    private static final boolean debug = true;

    private static class CompletePattern {
        private AbstractDSLPattern DSLPattern;
        private List<DatalogRule> translatedRules;
        private String name;

        public CompletePattern(AbstractDSLPattern DSLPattern, String name) {
            this.DSLPattern = DSLPattern;
            this.name = name;
        }

        public void setDSLPattern(AbstractDSLPattern DSLPattern) {
            this.DSLPattern = DSLPattern;
        }

        public void setTranslatedRules(List<DatalogRule> translatedRules) {
            this.translatedRules = translatedRules;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AbstractDSLPattern getDSLPattern() {
            return DSLPattern;
        }

        public List<DatalogRule> getTranslatedRules() {
            return translatedRules;
        }

        public String getName() {
            return name;
        }
    }

    public static void main(String args[]) {
        List<CompletePattern> patterns = createPatterns();
        translatePatterns(patterns);
        try {
            writePatternsToFile(patterns);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<CompletePattern> createPatterns() {

        List<CompletePattern> patterns = new ArrayList<>(12);

        DSLInstructionFactory instrFct = new DSLInstructionFactory();
        PredicateFactory prdFct = new PredicateFactory();
        DSLPatternFactory pattFct = new DSLPatternFactory();

        DSLLabel l1 = new DSLLabel();
        DSLLabel l2 = new DSLLabel();
        DSLLabel l3 = new DSLLabel();
        DSLLabel l4 = new DSLLabel();
        DSLLabelDC dcLabel = new DSLLabelDC();
        VariableDC dcVar = new VariableDC();
        Variable X = new Variable();
        Variable Y = new Variable();
        Variable amount = new Variable();

        DSLToDatalogTranslator transl = new DSLToDatalogTranslator();
        DSLAnalysis analyzer;
        try {
            analyzer = new DSLAnalysis();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        /*log(" *** LQ - Ether liquidity");
        AbstractDSLPattern patternComplianceOneLQ = pattFct.all(instrFct.stop(l1),
                pattFct.some(instrFct.dslgoto(l2, X, l3),
                        pattFct.and(
                                pattFct.eq(X, CallValue.class),
                                prdFct.follow(l2, l4),
                                pattFct.not(pattFct.eq(l3, l4)),
                                prdFct.mustFollow(l4, l1))));

        log(patternComplianceOneLQ.getStringRepresentation());

        patterns.add(patternComplianceOneLQ);

        AbstractDSLPattern patternComplianceTwoLQ = pattFct.some(instrFct.call(l1, dcVar, dcVar, amount),
                pattFct.or(pattFct.not(pattFct.eq(amount, 0)), prdFct.detBy(amount, DSLMsgdata.class)));

        log(patternComplianceTwoLQ.getStringRepresentation());

        AbstractDSLPattern patternViolationLQ = pattFct.and(
                pattFct.some(instrFct.stop(l1),
                        pattFct.not(prdFct.mayDepOn(l1, CallValue.class))),
                pattFct.all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                        pattFct.eq(amount, 0)));

        log(patternViolationLQ.getStringRepresentation());*/

        log(" *** NW - No writes after call");
        AbstractDSLPattern patternComplianceNW = pattFct.all(
                instrFct.call(l1, dcVar, dcVar, dcVar),
                pattFct.all(instrFct.sstore(l2, dcVar, dcVar),
                        pattFct.not(prdFct.mayFollow(l1, l2)))
        );

        log(patternComplianceNW.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceNW, "patternComplianceNW"));

        AbstractDSLPattern patternViolationNW = pattFct.some(
                instrFct.call(l1, dcVar, dcVar, dcVar),
                pattFct.some(instrFct.sstore(l2, dcVar, dcVar),
                        pattFct.not(prdFct.mustFollow(l1, l2)))
        );

        log(patternViolationNW.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationNW, "patternViolationNW"));

        log(" *** RW - restricted write");
        AbstractDSLPattern patternComplianceRW = pattFct.all(instrFct.sstore(dcLabel, X, dcVar),
                prdFct.detBy(X, Caller.class));
        log(patternComplianceRW.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceRW, "patternComplianceRW"));

        AbstractDSLPattern patternViolationRW = pattFct.some(instrFct.sstore(l1, X, dcVar),
                pattFct.and(pattFct.not(prdFct.mayDepOn(X, Caller.class)), pattFct.not(prdFct.mayDepOn(l1, Caller.class))));
        log(patternViolationRW.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationRW, "patternViolationRW"));

        log(" *** RT - restricted transfer");
        AbstractDSLPattern patternComplianceRT = pattFct.all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.eq(amount, 0));
        log(patternComplianceRT.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceRT, "patternComplianceRT"));

        AbstractDSLPattern patternViolationRT = pattFct.some(instrFct.call(l1, dcVar, dcVar, amount),
                pattFct.and(prdFct.detBy(amount, DSLMsgdata.class),
                        pattFct.not(prdFct.mayDepOn(l1, Caller.class)),
                        pattFct.not(prdFct.mayDepOn(l1, DSLMsgdata.class))));
        log(patternViolationRT.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationRT, "patternViolationRT"));

        log(" *** HE - handled exception");
        AbstractDSLPattern patternComplianceHE = pattFct.all(instrFct.call(l1, Y, dcVar, dcVar),
                pattFct.some(instrFct.dslgoto(l2, X, dcLabel),
                        pattFct.and(prdFct.mustFollow(l1, l2), prdFct.detBy(X, Y))));
        log(patternComplianceHE.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceHE, "patternComplianceHE"));

        AbstractDSLPattern patternViolationHE = pattFct.some(instrFct.call(l1, Y, dcVar, dcVar),
                pattFct.all(instrFct.dslgoto(l2, X, dcLabel),
                        pattFct.implies(prdFct.mayFollow(l1, l2), pattFct.not(prdFct.mayDepOn(X, Y)))));
        log(patternViolationHE.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationHE, "patternViolationHE"));

        log(" *** TOD - transaction ordering dependency");
        AbstractDSLPattern patternComplianceTOD = pattFct.all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.and(pattFct.not(prdFct.mayDepOn(amount, SLoad.class)), pattFct.not(prdFct.mayDepOn(amount, Balance.class))));
        log(patternComplianceTOD.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceTOD, "patternComplianceTOD"));

        AbstractDSLPattern patternViolationTOD = pattFct.some(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.some(instrFct.sload(dcLabel, Y, X),
                        pattFct.some(instrFct.sstore(dcLabel, X, dcVar),
                                pattFct.and(prdFct.detBy(amount, Y), prdFct.isConst(X)))));
        log(patternViolationTOD.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationTOD, "patternViolationTOD"));

        log(" *** VA - validated arguments");
        AbstractDSLPattern patternComplianceVA = pattFct.all(instrFct.sstore(l1, dcVar, X),
                pattFct.implies(prdFct.mayDepOn(X, Arg.class),
                        pattFct.some(instrFct.dslgoto(l2, Y, dcLabel),
                                pattFct.and(prdFct.mustFollow(l2, l1),
                                        prdFct.detBy(Y, Arg.class)))));
        log(patternComplianceVA.getStringRepresentation());
        patterns.add(new CompletePattern(patternComplianceVA, "patternComplianceVA"));

        AbstractDSLPattern patternViolationVA = pattFct.some(instrFct.sstore(l1, dcVar, X),
                pattFct.implies(prdFct.mayDepOn(X, Arg.class),
                        pattFct.not(pattFct.some(instrFct.dslgoto(l2, Y, dcLabel),
                                pattFct.and(prdFct.mustFollow(l2, l1),
                                        prdFct.mayDepOn(Y, Arg.class))))));
        log(patternViolationVA.getStringRepresentation());
        patterns.add(new CompletePattern(patternViolationVA, "patternComplianceVA"));

        return patterns;

    }
    
    private static void log(String str) {
        if(debug)
            System.out.println(str);
    }

    private static void translatePatterns(List<CompletePattern> patterns) {
        patterns.forEach(completePattern -> {
            try {
                completePattern.setTranslatedRules(
                        DSLToDatalogTranslator.translateInstructionPattern(
                                completePattern.getDSLPattern(), completePattern.getName()));
            } catch (InvalidPatternException e) {
                e.printStackTrace();
            }
        });
    }

    private static void writePatternsToFile(List<CompletePattern> patterns) throws IOException {
        DSLAnalysis analyzer;

        try {
            analyzer = new DSLAnalysis();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        BufferedWriter bwr;
        bwr = new BufferedWriter(new FileWriter(new File("smt_files/CompiledPatterns.dl")));
        for (CompletePattern patt : patterns) {
            for (DatalogRule rule : patt.getTranslatedRules()) {
                bwr.write(rule.getStingRepresentation(analyzer));
                bwr.newLine();
            }
            bwr.newLine();
        }
        bwr.close();

    }
}
