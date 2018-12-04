package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Balance;
import ch.securify.decompiler.instructions.CallDataLoad;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.dslpatterns.datalogpattern.DatalogRule;
import ch.securify.dslpatterns.instructions.DSLInstructionFactory;
import ch.securify.dslpatterns.predicates.PredicateFactory;
import ch.securify.dslpatterns.tags.DSLArg;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.dslpatterns.util.VariableDC;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DSLPatternsCompiler {

    private static final boolean debug = true;

    public static final String DATALOG_PATTERNS_FILE = "smt_files/CompiledPatterns.dl";
    public static final String PATTERN_NAMES_CSV = "smt_files/CompiledPatternsNames.csv";

    private static class CompletePattern {
        private AbstractDSLPattern DSLCompliancePattern;
        private AbstractDSLPattern DSLViolationPattern;
        private List<DatalogRule> translatedRules;
        private String name;

        public CompletePattern(String name, AbstractDSLPattern DSLCompliancePattern, AbstractDSLPattern DSLViolationPattern) {
            this.DSLCompliancePattern = DSLCompliancePattern;
            this.DSLViolationPattern = DSLViolationPattern;
            this.name = name;
            translatedRules = new ArrayList<>();
        }

        public AbstractDSLPattern getDSLCompliancePattern() {
            return DSLCompliancePattern;
        }

        public AbstractDSLPattern getDSLViolationPattern() {
            return DSLViolationPattern;
        }

        public void addTranslatedRules(List<DatalogRule> translatedRules) {
            this.translatedRules.addAll(translatedRules);
        }

        public List<DatalogRule> getTranslatedRules() {
            return translatedRules;
        }

        public String getName() {
            return name;
        }

        public String getComplianceName() {
            return name + "Compliance";
        }

        public String getViolationName() {
            return name + "Violation";
        }
    }

    public static void main(String args[]) {
        List<CompletePattern> patterns = createPatterns();
        translatePatterns(patterns);
        try {
            writePatternsToFile(patterns);
            writePatternsNameToCSV(patterns);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
    DAO = no writes after call

     */

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
                pattFct.or(pattFct.not(pattFct.eq(amount, 0)), prdFct.detBy(amount, CallDataLoad.class)));

        log(patternComplianceTwoLQ.getStringRepresentation());

        AbstractDSLPattern patternViolationLQ = pattFct.and(
                pattFct.some(instrFct.stop(l1),
                        pattFct.not(prdFct.mayDepOn(l1, CallValue.class))),
                pattFct.all(instrFct.call(dcLabel, dcVar, dcVar, amount),
                        pattFct.eq(amount, 0)));

        log(patternViolationLQ.getStringRepresentation());*/

        log(" *** NW - No writes after call - DAO");
        AbstractDSLPattern patternComplianceNW = pattFct.instructionPattern(
                instrFct.call(l1, dcVar, dcVar, dcVar),
                pattFct.all(instrFct.sstore(l2, dcVar, dcVar),
                        pattFct.not(prdFct.mayFollow(l1, l2)))
        );

        log(patternComplianceNW.getStringRepresentation());

        AbstractDSLPattern patternViolationNW = pattFct.instructionPattern(
                instrFct.call(l1, dcVar, dcVar, dcVar),
                pattFct.some(instrFct.sstore(l2, dcVar, dcVar),
                        pattFct.not(prdFct.mustFollow(l1, l2)))
        );

        log(patternViolationNW.getStringRepresentation());
        patterns.add(new CompletePattern("NW", patternComplianceNW, patternViolationNW));

        log(" *** RW - restricted write");
        AbstractDSLPattern patternComplianceRW = pattFct.instructionPattern(instrFct.sstore(dcLabel, X, dcVar),
                prdFct.detBy(X, Caller.class));
        log(patternComplianceRW.getStringRepresentation());

        AbstractDSLPattern patternViolationRW = pattFct.instructionPattern(instrFct.sstore(l1, X, dcVar),
                pattFct.and(pattFct.not(prdFct.mayDepOn(X, Caller.class)), pattFct.not(prdFct.mayDepOn(l1, Caller.class))));
        log(patternViolationRW.getStringRepresentation());
        patterns.add(new CompletePattern("RW", patternComplianceRW, patternViolationRW));

        log(" *** RT - restricted transfer");
        AbstractDSLPattern patternComplianceRT = pattFct.instructionPattern(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.eq(amount, 0));
        log(patternComplianceRT.getStringRepresentation());

        AbstractDSLPattern patternViolationRT = pattFct.instructionPattern(instrFct.call(l1, dcVar, dcVar, amount),
                pattFct.and(prdFct.detBy(amount, CallDataLoad.class),
                        pattFct.not(prdFct.mayDepOn(l1, Caller.class)),
                        pattFct.not(prdFct.mayDepOn(l1, CallDataLoad.class))));
        log(patternViolationRT.getStringRepresentation());
        patterns.add(new CompletePattern("RT", patternComplianceRT, patternViolationRT));

        log(" *** HE - handled exception");
        AbstractDSLPattern patternComplianceHE = pattFct.instructionPattern(instrFct.call(l1, Y, dcVar, dcVar),
                pattFct.some(instrFct.dslgoto(l2, X, dcLabel),
                        pattFct.and(prdFct.mustFollow(l1, l2), prdFct.detBy(X, Y))));
        log(patternComplianceHE.getStringRepresentation());

        AbstractDSLPattern patternViolationHE = pattFct.instructionPattern(instrFct.call(l1, Y, dcVar, dcVar),
                pattFct.all(instrFct.dslgoto(l2, X, dcLabel),
                        pattFct.implies(prdFct.mayFollow(l1, l2), pattFct.not(prdFct.mayDepOn(X, Y)))));
        log(patternViolationHE.getStringRepresentation());
        patterns.add(new CompletePattern("HE", patternComplianceHE, patternViolationHE));

        log(" *** TOD - transaction ordering dependency");
        AbstractDSLPattern patternComplianceTOD = pattFct.instructionPattern(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.and(pattFct.not(prdFct.mayDepOn(amount, SLoad.class)), pattFct.not(prdFct.mayDepOn(amount, Balance.class))));
        log(patternComplianceTOD.getStringRepresentation());

        AbstractDSLPattern patternViolationTOD = pattFct.instructionPattern(instrFct.call(dcLabel, dcVar, dcVar, amount),
                pattFct.some(instrFct.sload(dcLabel, Y, X),
                        pattFct.some(instrFct.sstore(dcLabel, X, dcVar),
                                pattFct.and(prdFct.detBy(amount, Y), prdFct.isConst(X)))));
        log(patternViolationTOD.getStringRepresentation());
        patterns.add(new CompletePattern("TOD", patternComplianceTOD, patternViolationTOD));

        log(" *** VA - validated arguments");
        AbstractDSLPattern patternComplianceVA = pattFct.instructionPattern(instrFct.sstore(l1, dcVar, X),
                pattFct.implies(prdFct.mayDepOn(X, DSLArg.class),
                        pattFct.some(instrFct.dslgoto(l2, Y, dcLabel),
                                pattFct.and(prdFct.mustFollow(l2, l1),
                                        prdFct.detBy(Y, DSLArg.class)))));
        log(patternComplianceVA.getStringRepresentation());

        AbstractDSLPattern patternViolationVA = pattFct.instructionPattern(instrFct.sstore(l1, dcVar, X),
                pattFct.implies(prdFct.mayDepOn(X, DSLArg.class),
                        pattFct.not(pattFct.some(instrFct.dslgoto(l2, Y, dcLabel),
                                pattFct.and(prdFct.mustFollow(l2, l1),
                                        prdFct.mayDepOn(Y, DSLArg.class))))));
        log(patternViolationVA.getStringRepresentation());
        patterns.add(new CompletePattern("VA", patternComplianceVA, patternViolationVA));
        
        return patterns;

    }
    
    private static void log(String str) {
        if(debug)
            System.out.println(str);
    }

    private static void translatePatterns(List<CompletePattern> patterns) {
        patterns.forEach(completePattern -> {
            try {
                completePattern.addTranslatedRules(
                        DSLToDatalogTranslator.translateInstructionPattern(
                                completePattern.getDSLCompliancePattern(), completePattern.getComplianceName()));
                completePattern.addTranslatedRules(
                        DSLToDatalogTranslator.translateInstructionPattern(
                                completePattern.getDSLViolationPattern(), completePattern.getViolationName()));
            } catch (InvalidPatternException e) {
                e.printStackTrace();
            }
        });
    }

    private static void writePatternsToFile(List<CompletePattern> patterns) throws IOException {
        DSLAnalysis analyzer;

        Set<String> alreadyDeclaredRules = new HashSet<>(patterns.size());

        try {
            analyzer = new DSLAnalysis();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        BufferedWriter bwr;
        bwr = new BufferedWriter(new FileWriter(new File(DATALOG_PATTERNS_FILE)));
        for (CompletePattern patt : patterns) {
            for (DatalogRule rule : patt.getTranslatedRules()) {

                String name = rule.getHead().getName();
                if(!alreadyDeclaredRules.contains(name)) {
                    alreadyDeclaredRules.add(name);
                    bwr.write(rule.getDeclaration());
                    bwr.newLine();
                }
                bwr.write(rule.getStingRepresentation(analyzer));
                bwr.newLine();
            }
            bwr.newLine();
        }
        bwr.close();

    }

    private static void writePatternsNameToCSV(List<CompletePattern> patterns) throws IOException {
        BufferedWriter bwr;
        bwr = new BufferedWriter(new FileWriter(new File(PATTERN_NAMES_CSV)));
        if(!patterns.isEmpty())
            bwr.write(patterns.get(0).getName());
        for (int i = 1; i < patterns.size(); ++i) {
            bwr.write(" , " + patterns.get(i).getName());
        }
        bwr.close();
    }

}
