package ch.securify.dslpatterns;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Balance;
import ch.securify.decompiler.instructions.CallDataLoad;
import ch.securify.decompiler.instructions.Caller;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.dslpatterns.datalogpattern.DatalogRule;
import ch.securify.dslpatterns.instructions.AbstractDSLInstruction;
import ch.securify.dslpatterns.tags.DSLArg;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.DSLLabelDC;
import ch.securify.dslpatterns.util.InvalidPatternException;
import ch.securify.dslpatterns.util.VariableDC;
import ch.securify.utils.CommandRunner;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//static import lets us call static methods from DSLPatternFactory without using the class name identifier in front of them
//and avoids the Constant Interface Antipattern
import static ch.securify.dslpatterns.DSLPatternFactory.*;


/**
 * Class that contains the patterns written in DSL, translates them into datalog, joins them with the inference rules
 * contained in the allInOneAnalysis.dl and compiles the resulting datalog file creating the datalog executable
 */
public class DSLPatternsCompiler {

    private static final String SOUFFLE_RULES = "smt_files/allInOneAnalysis.dl";
    private static final String TMP_DL_FILE = "smt_files/finalTmpFile.dl";
    public static final String FINAL_EXECUTABLE = "build/dslSolver";

    private static final String SOUFFLE_BIN = "souffle";

    private static final boolean debug = true;

    public static final String DATALOG_PATTERNS_FILE = "smt_files/CompiledPatterns.dl";
    public static final String PATTERN_NAMES_CSV = "smt_files/CompiledPatternsNames.csv";

    /**
     * Internal representation needed to collect patterns and translations
     */
    private static class CompletePattern {
        private InstructionDSLPattern DSLCompliancePattern;
        private InstructionDSLPattern DSLViolationPattern;
        private List<DatalogRule> translatedRules;
        private String name;

        CompletePattern(String name, InstructionDSLPattern DSLCompliancePattern, InstructionDSLPattern DSLViolationPattern) {
            this.DSLCompliancePattern = DSLCompliancePattern;
            this.DSLViolationPattern = DSLViolationPattern;
            this.name = name;
            translatedRules = new ArrayList<>();
        }

        InstructionDSLPattern getDSLCompliancePattern() {
            return DSLCompliancePattern;
        }

        InstructionDSLPattern getDSLViolationPattern() {
            return DSLViolationPattern;
        }

        void addTranslatedRules(List<DatalogRule> translatedRules) {
            this.translatedRules.addAll(translatedRules);
        }

        List<DatalogRule> getTranslatedRules() {
            return translatedRules;
        }

        public String getName() {
            return name;
        }

        String getComplianceName() {
            return name + "Compliance";
        }

        String getViolationName() {
            return name + "Violation";
        }

        String getWaringRuleDeclaration() {
            StringBuilder sb = new StringBuilder();

            //example of declaration
            //.decl jump		(l1: Label, l2: Label, l3: Label)
            // .output jump
            sb.append(".decl ");
            sb.append(name);
            sb.append("Warnings(L: Label)\n");
            sb.append(".output ");
            sb.append(name);
            sb.append("Warnings");

            return sb.toString();
        }

        String getWarningRule(DSLAnalysis analyzer) {
            StringBuilder sb = new StringBuilder();

            AbstractDSLInstruction quantifiedInst = DSLCompliancePattern.getQuantifiedInstr();
            if(quantifiedInst.getLabel() instanceof DSLLabelDC) {
                quantifiedInst = quantifiedInst.getCopy();
                quantifiedInst.setLabel(new DSLLabel());
            }
            String label = quantifiedInst.getLabel().getName();

            sb.append(name);
            sb.append("Warnings(");
            sb.append(label);
            sb.append(") :- ");
            sb.append(quantifiedInst.getDatalogStringRepDC(analyzer));
            sb.append(" , !");
            sb.append(name);
            sb.append("Compliance(");
            sb.append(label);
            sb.append("), !");
            sb.append(name);
            sb.append("Violation(");
            sb.append(label);
            sb.append(").");

            return sb.toString();
        }
    }

    private static boolean isSouffleInstalled() {
        try {
            Process process = new ProcessBuilder(SOUFFLE_BIN).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Translates patterns into datalog, joins them with the inference rules
     *  contained in the allInOneAnalysis.dl and compiles the resulting datalog file creating the datalog executable
     * @throws IOException
     * @throws InterruptedException
     */
    public static void generateDatalogExecutable() throws IOException, InterruptedException {

        if(!isSouffleInstalled()){
            System.err.println("Soufflé does not seem to be installed.");
            System.exit(7);
        }

        List<CompletePattern> patterns = createPatterns();
        translatePatterns(patterns);
        try {
            writePatternsToFile(patterns);
            writePatternsNameToCSV(patterns);

        } catch (IOException e) {
            e.printStackTrace();
        }

        collapseSouffleRulesAndQueries();

        String cmd = SOUFFLE_BIN + " --dl-program=" + FINAL_EXECUTABLE + " " + TMP_DL_FILE;
        log(cmd);
        CommandRunner.runCommand(cmd);
    }

    /**
     * Joins the two files, AllInOneAnalysis.dl and the one containing the translated patterns
     * @throws IOException
     */
    private static void collapseSouffleRulesAndQueries() throws IOException {
        PrintWriter pw = new PrintWriter( TMP_DL_FILE);
        BufferedReader br = new BufferedReader(new FileReader(SOUFFLE_RULES));

        String line = br.readLine();
        while (line != null) {
            pw.println(line);
            line = br.readLine();
        }
        br = new BufferedReader(new FileReader(DSLPatternsCompiler.DATALOG_PATTERNS_FILE));
        line = br.readLine();

        while(line != null)
        {
            pw.println(line);
            line = br.readLine();
        }

        pw.flush();
        br.close();
        pw.close();
    }

    /**
     * Method that contains the patters in dsl language
     * @return a list of patterns in dsl
     */
    private static List<CompletePattern> createPatterns() {

        List<CompletePattern> patterns = new ArrayList<>(6);

        DSLLabel l1 = new DSLLabel();
        DSLLabel l2 = new DSLLabel();
        DSLLabel l3 = new DSLLabel();
        DSLLabel l4 = new DSLLabel();
        DSLLabelDC dcLabel = new DSLLabelDC();
        VariableDC dcVar = new VariableDC();
        Variable X = new Variable();
        Variable Y = new Variable();
        Variable amount = new Variable();
        Variable constVar = new Variable();

        DSLToDatalogTranslator transl = new DSLToDatalogTranslator();

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
        InstructionDSLPattern patternComplianceNW = instructionPattern(
                call(l1, dcVar, dcVar, dcVar),
               all(sstore(l2, dcVar, dcVar),
                        not(mayFollow(l1, l2)))
        );

        log(patternComplianceNW.getStringRepresentation());

        InstructionDSLPattern patternViolationNW = instructionPattern(
               call(l1, dcVar, dcVar, dcVar),
                some(sstore(l2, dcVar, dcVar),
                        mustFollow(l1, l2))
        );

        log(patternViolationNW.getStringRepresentation());
        patterns.add(new CompletePattern("NoWritesAfterCallDAO", patternComplianceNW, patternViolationNW));

        log(" *** RW - restricted write");
        InstructionDSLPattern patternComplianceRW = instructionPattern(sstore(dcLabel, X, dcVar),
                detBy(X, Caller.class));
        log(patternComplianceRW.getStringRepresentation());

        InstructionDSLPattern patternViolationRW = instructionPattern(sstore(l1, X, dcVar),
                and(not(mayDepOn(X, Caller.class)), not(mayDepOn(l1, Caller.class))));
        log(patternViolationRW.getStringRepresentation());
        patterns.add(new CompletePattern("unRestrictedWrite", patternComplianceRW, patternViolationRW));

        log(" *** RWUP - restricted write updated");
        InstructionDSLPattern patternComplianceRWUP = instructionPattern(sstore(l1, X, dcVar),
                or(detBy(X, Caller.class), and(dslgoto(l2, Y, dcLabel), mustFollow(l2,l1), detBy(Y, Caller.class))));
        log(patternComplianceRW.getStringRepresentation());

        InstructionDSLPattern patternViolationRWUP = instructionPattern(sstore(l1, X, dcVar),
                and(not(mayDepOn(X, Caller.class)), not(mayDepOn(l1, Caller.class))));
        log(patternViolationRW.getStringRepresentation());
        patterns.add(new CompletePattern("unRestrictedWriteUP", patternComplianceRWUP, patternViolationRWUP));

        log(" *** RT - restricted transfer");
        InstructionDSLPattern patternComplianceRT = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                eq(amount, 0));
        log(patternComplianceRT.getStringRepresentation());

        InstructionDSLPattern patternViolationRT = instructionPattern(call(l1, dcVar, dcVar, amount),
                or(and(detBy(amount, CallDataLoad.class),
                        not(mayDepOn(l1, Caller.class)),
                        not(mayDepOn(l1, CallDataLoad.class))), and(isConst(amount), greaterThan(amount, 0))));
        log(patternViolationRT.getStringRepresentation());
        CompletePattern unrestTrans = new CompletePattern("unRestrictedTransferEtherFlow", patternComplianceRT, patternViolationRT);
        patterns.add(unrestTrans);

        log(" *** HE - handled exception");
        InstructionDSLPattern patternComplianceHE = instructionPattern(call(l1, Y, dcVar, dcVar),
                some(dslgoto(l2, X, dcLabel),
                        and(mustFollow(l1, l2), detBy(X, Y))));
        log(patternComplianceHE.getStringRepresentation());

        InstructionDSLPattern patternViolationHE =
                instructionPattern(call(l1, Y, dcVar, dcVar),
                                    all(dslgoto(l2, X, dcLabel),
                                            implies(mayFollow(l1, l2), not(mayDepOn(X, Y)))));

        log(patternViolationHE.getStringRepresentation());
        patterns.add(new CompletePattern("unHandledException", patternComplianceHE, patternViolationHE));

        log(" *** TOD - transaction ordering dependency");
        InstructionDSLPattern patternComplianceTOD = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                    and(not(mayDepOn(amount, SLoad.class)), not(mayDepOn(amount, Balance.class))));
        log(patternComplianceTOD.getStringRepresentation());

        InstructionDSLPattern patternViolationTOD = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                some(sload(dcLabel, Y, X),
                        some(sstore(dcLabel, X, dcVar),
                                and(detBy(amount, Y), isConst(X)))));
        log(patternViolationTOD.getStringRepresentation());
        patterns.add(new CompletePattern("TOD", patternComplianceTOD, patternViolationTOD));

        log(" *** TODII - transaction ordering dependency");
        InstructionDSLPattern patternComplianceTODII = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                and(not(mayDepOn(amount, SLoad.class)), not(mayDepOn(amount, Balance.class))));
        log(patternComplianceTOD.getStringRepresentation());

        InstructionDSLPattern patternViolationTODII = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                        or(detBy(amount, Balance.class), and(detBy(amount, SLoad.class),
                                sstore(dcLabel, X, dcVar), isConst(X), hasValue(X, constVar),
                                offsetToStorageVar(constVar, Y), detBy(amount, Y))));
        log(patternViolationTOD.getStringRepresentation());
        patterns.add(new CompletePattern("TODIIAmount", patternComplianceTODII, patternViolationTODII));

        /*log(" *** TODIII - transaction ordering dependency");
        InstructionDSLPattern patternComplianceTODIII = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                or(and(isConst(amount), hasValue(amount, 0), and(not(mayDepOn(amount, SLoad.class)), not(mayDepOn(amount, Balance.class))));
        log(patternComplianceTOD.getStringRepresentation());

        InstructionDSLPattern patternViolationTODIII = instructionPattern(call(dcLabel, dcVar, dcVar, amount),
                and(or(detBy(amount, SLoad.class),  detBy(amount, Balance.class)),
                        sstore(dcLabel, X, dcVar), isConst(X), hasValue(X, Y), detBy(amount, Y)));
        log(patternViolationTOD.getStringRepresentation());
        patterns.add(new CompletePattern("TODIIIAmount", patternComplianceTODIII, patternViolationTODIII));*/

        log(" *** VA - validated arguments");
        InstructionDSLPattern patternComplianceVA = instructionPattern(sstore(l1, dcVar, X),
                implies(mayDepOn(X, DSLArg.class),
                        some(dslgoto(l2, Y, dcLabel),
                                and(mustFollow(l2, l1),
                                        detBy(Y, DSLArg.class)))));
        log(patternComplianceVA.getStringRepresentation());

        InstructionDSLPattern patternViolationVA = instructionPattern(sstore(l1, dcVar, X),
                and(detBy(X, DSLArg.class),
                        not(some(dslgoto(l2, Y, dcLabel),
                                and(mustFollow(l2, l1),
                                        mayDepOn(Y, DSLArg.class))))));
        log(patternViolationVA.getStringRepresentation());
        patterns.add(new CompletePattern("ValidatedArgumentsMissingInputValidation", patternComplianceVA, patternViolationVA));

        log(" *** CALL - callTest");
        InstructionDSLPattern callTestCompliance = instructionPattern(call(l1, dcVar, dcVar, dcVar),
                call(l1, dcVar, dcVar, dcVar));
        log(callTestCompliance.getStringRepresentation());

        InstructionDSLPattern callTestViolation = instructionPattern(call(l1, dcVar, dcVar, dcVar),
                not(call(l1, dcVar, dcVar, dcVar)));
        log(callTestViolation.getStringRepresentation());
        patterns.add(new CompletePattern("callTest", callTestCompliance, callTestViolation));
        
        return patterns;

    }
    
    private static void log(String str) {
        if(debug)
            System.out.println(str);
    }

    /**
     * Translates the patterns from dsl to datalog representation
     * @param patterns the patterns to be translated, the translated patterns will also be saved inside here
     */
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

    /**
     * Creates the tmp datalog file containing the translated patterns
     * @param patterns
     * @throws IOException
     */
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
            bwr.write(patt.getWaringRuleDeclaration());
            bwr.newLine();
            bwr.write(patt.getWarningRule(analyzer));
            bwr.newLine();
            bwr.newLine();
        }
        bwr.close();

    }

    /**
     * Creates a CSV file with the pattern names, necessary when reading the pattern results, in order to read the
     * names of the output datalog rules
     * @param patterns
     * @throws IOException
     */
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
