/*
 *  Copyright 2018 Secure, Reliable, and Intelligent Systems Lab, ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


package ch.securify;

import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.DataflowFactory;
import ch.securify.decompiler.*;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions._VirtualMethodHead;
import ch.securify.decompiler.printer.DecompilationPrinter;
import ch.securify.model.Contract;
import ch.securify.model.ContractResult;
import ch.securify.model.PatternResult;
import ch.securify.patterns.*;
import ch.securify.utils.DevNullPrintStream;
import ch.securify.utils.StreamUtil;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Strings;
import com.google.gson.*;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class Main {

    private static class Args {
        @Parameter(names = {"-h", "-?", "--help"}, description = "usage", help = true)
        private boolean help;

        @Parameter(names = {"-fs", "--filesol"}, description = "smart contract as a Solidity file")
        private String filesol;

        @Parameter(names = {"-o", "--output"}, description = "json output file")
        private String outputfile;

        @Parameter(names = {"-fh", "--filehex"}, description = "contract runtime code to parse as a hex-encoded file")
        private String filehex;

        @Parameter(names = {"-fjl",
                "--filejsonlist"}, description = "JSON file containing a list of smart contracts (2d array)")
        private String filejsonlist;

        @Parameter(names = {"-ca",
                "--contractaddress"}, description = "specific contract address to search for in the provided set of contracts")
        private String contractaddress;

        @Parameter(names = {"-p", "--patterns"}, description = "csv list of patterns to be analyzed")
        private String patterns;

        @Parameter(names = {"--livestatusfile"}, description = "status output file to track analysis progress")
        private String livestatusfile;

        @Parameter(names = {"--decompoutputfile"}, description = "output file for the decompiled code")
        private String decompoutputfile;
    }

    private static List<AbstractPattern> patterns;
    private static ContractResult contractResult;
    private static boolean DEBUG = false;
    private static PrintStream log = DEBUG ? System.out : new DevNullPrintStream();
    private static Args args;


    public static HashMap<String, SolidityResult> processSolidityFile(String filesol, String livestatusfile) throws IOException, InterruptedException, NotFound {
        byte[] fileContent = Files.readAllBytes(new File(filesol).toPath());

        JsonObject compilationOutput = CompilationHelpers.compileContracts(filesol);

        Set<Map.Entry<String, JsonElement>> entries = compilationOutput.entrySet();

        HashMap<String, SolidityResult> allContractResults = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : entries) {
            log.println("Processing contract:");
            log.println(e.getKey());

            String bin = e.getValue().getAsJsonObject().get("bin-runtime").getAsString();
            String map = e.getValue().getAsJsonObject().get("srcmap-runtime").getAsString();

            List<String> lines = Arrays.asList(bin);
            File binFile = File.createTempFile("securify_binary_", ".bin.hex");
            binFile.deleteOnExit();
            Files.write(Paths.get(binFile.getPath()), lines);

            processHexFile(binFile.getPath(), null, livestatusfile);

            SolidityResult allPatternResults = CompilationHelpers.getMappingsFromStatusFile(livestatusfile, map, fileContent);
            allContractResults.put(e.getKey(), allPatternResults);
        }

        return allContractResults;

    }

    private static void processHexFile(String hexBinaryFile, String decompilationOutputFile, String livestatusfile) throws IOException, InterruptedException {
        if (!new File(hexBinaryFile).exists()) {
            throw new IllegalArgumentException("File '" + hexBinaryFile + "' not found");
        }

        // read contract binary hex file
        byte[] bin = CompilationHelpers.extractBinaryFromHexFile(hexBinaryFile);

        contractResult = new ContractResult();
        updateContractAnalysisStatus(livestatusfile);

        List<Instruction> instructions;
        try {
            // decompile
            instructions = decompileContract(bin);
            contractResult.decompiled = true;
        } catch (Exception e) {
            contractResult.finished = true;
            updateContractAnalysisStatus(livestatusfile);
            return;
        }

        if (decompilationOutputFile != null) {
            new File(decompilationOutputFile).getAbsoluteFile().getParentFile().mkdirs();

            Variable.setDebug(false);
            Files.write(Paths.get(decompilationOutputFile),
                    (Iterable<String>) instructions.stream().map(Instruction::toString)::iterator);
            Variable.setDebug(true);
            contractResult.decompiled = true;
            updateContractAnalysisStatus(livestatusfile);
        }

        checkPatterns(instructions, livestatusfile);
        contractResult.finished = true;
        updateContractAnalysisStatus(livestatusfile);

    }

    public static void main(String[] rawrgs) throws IOException, InterruptedException, NotFound {

        args = new Args();

        try {
            new JCommander(args, rawrgs);
        } catch (ParameterException e) {
            log.println(e.getMessage());
            new JCommander(args).usage();
            return;
        }

        initPatterns(args);

        File lStatusFile;
        if (args.livestatusfile != null) {
            lStatusFile = new File(args.livestatusfile);
            if (lStatusFile.getParentFile() != null) {
                lStatusFile.getParentFile().mkdirs();
            }
        } else {
            lStatusFile = File.createTempFile("securify_livestatusfile", "");
            args.livestatusfile = lStatusFile.getPath();
            lStatusFile.deleteOnExit();
        }
        String livestatusfile = lStatusFile.getPath();


        if (args.filesol != null) {
            HashMap<String, SolidityResult> allContractsResults = processSolidityFile(args.filesol, livestatusfile);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            if (args.outputfile != null) {
                try (Writer writer = new FileWriter(args.outputfile)) {
                    gson.toJson(allContractsResults, writer);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            } else {
                gson.toJson(allContractsResults, System.out);
            }
            return;
        }

        if (args.filehex != null) {
            processHexFile(args.filehex, args.decompoutputfile, livestatusfile);
        } else if (args.filejsonlist != null) {
            log.println("filejsonlist: " + args.filejsonlist);
            if (!new File(args.filejsonlist).exists()) {
                throw new IllegalArgumentException("File '" + args.filejsonlist + "' not found");
            }

            Contract[][] contracts = new Gson().fromJson(new FileReader(args.filejsonlist), Contract[][].class);

            long contractsNonUniqueCount = Arrays.stream(contracts).filter(Objects::nonNull).flatMap(Arrays::stream)
                    .filter(Objects::nonNull).count();
            log.println("contractsNonUniqueCount = " + contractsNonUniqueCount);

            List<Contract> uniqueContracts = Arrays.stream(contracts).filter(Objects::nonNull).flatMap(Arrays::stream)
                    .filter(Objects::nonNull).filter(contract -> !contract.getCode().isEmpty())
                    .filter(contract -> args.contractaddress == null
                            || args.contractaddress.equalsIgnoreCase(contract.getContractAddress()))
                    .filter(StreamUtil.distinctCustom(Contract::getCode)).collect(Collectors.toList());

            handleContractList(uniqueContracts, livestatusfile);
        } else {
            new JCommander(args).usage();
            return;
        }

    }

    private static void handleContractList(List<Contract> contracts, String livestatusfile) {
        log.println("contract count: " + contracts.size());


        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonArray resultsJSON = new JsonArray();

        for (int counter = 0; counter < contracts.size(); counter++) {
            Contract contract = contracts.get(counter);
            JsonObject resultJSON = new JsonObject();
            resultJSON.addProperty("address", contract.getContractAddress());

            contractResult = new ContractResult();
            updateContractAnalysisStatus(livestatusfile);
            /* reset patterns */
            initPatterns(args);
            patterns.forEach(pattern -> contractResult.patternResults.put(pattern.getClass().getSimpleName(), new PatternResult()));
            log.println("Contract (" + counter + "/" + contracts.size() + "): " + contract.getContractAddress());

            byte[] bin = DatatypeConverter.parseHexBinary(contract.getCode());
            // decompile
            List<Instruction> instructions;

            try {
                instructions = decompileContract(bin);
            } catch (Exception e) {
                resultJSON.addProperty("status", "decompilation-failed");
                resultsJSON.add(resultJSON);
                log.println("  decomp failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
                continue;
            }

            // analyze patterns
            try {
                checkPatterns(instructions, livestatusfile);
                resultJSON.addProperty("status", "success");
                resultJSON.add("result", gson.toJsonTree(contractResult));
                resultsJSON.add(resultJSON);
            } catch (Exception e) {
                resultJSON.addProperty("status", "pattern-checking-failed");
                resultsJSON.add(resultJSON);
                log.println("  pattern check failed: " + e.getClass().getSimpleName() + " " + e.getMessage());
                continue;
            }
        }

        writeJSONtoFile(resultsJSON, livestatusfile, gson);
    }

    private static void writeJSONtoFile(Object jsonObj, String fileName, Gson gson) {
        try (Writer writer = new FileWriter(fileName)) {
            gson.toJson(jsonObj, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Decompile a contract binary.
     *
     * @param binary contract runtime binary
     * @return decompiled instructions
     */
    public static List<Instruction> decompileContract(byte[] binary) {
        List<Instruction> instructions;
        try {
            log.println("Attempt to decompile the contract with methods...");
            instructions = Decompiler.decompile(binary, log);

            log.println("Success. Inlining methods...");
            instructions = MethodInliner.inline(instructions, log);
        } catch (Exception e1) {
            log.println(e1.getMessage());
            log.println("Failed to decompile methods. Attempt to decompile the contract without identifying methods...");

            try {
                instructions = DecompilerFallback.decompile(binary, log);
            } catch (Exception e2) {
                log.println("Decompilation failed.");
                throw e2;
            }
        }

        log.println("Propagating constants...");
        ConstantPropagation.propagate(instructions);

        log.println();
        log.println("Decompiled contract:");
        DecompilationPrinter.printInstructions(instructions, log);

        return instructions;
    }

    private static void initPatterns(Args args) {
        patterns = new LinkedList<>();

        List<AbstractPattern> allPatterns = new LinkedList<>();
        allPatterns.add(new DAO());
        allPatterns.add(new DAOConstantGas());
        // allPatterns.add(new DAOMethodCall());
        // allPatterns.add(new DelegateCallWithUserInput());
        // allPatterns.add(new DivisionBeforeCallvalue());
        // allPatterns.add(new DivisionBeforeMultiply());
        // TODO: buggy
        allPatterns.add(new LockedEther());
        // allPatterns.add(new DivisionBeforeCallvalue());
        // allPatterns.add(new DivisionBeforeMultiply());
        allPatterns.add(new MissingInputValidation());
        allPatterns.add(new TODAmount());
        allPatterns.add(new TODReceiver());
        allPatterns.add(new TODTransfer());
        allPatterns.add(new UnhandledException());
        // allPatterns.add(new UnprivilegedSelfdestruct());
        allPatterns.add(new UnrestrictedEtherFlow());
        allPatterns.add(new UnrestrictedWrite());
//        allPatterns.add(new UnsafeCallTarget());
//        allPatterns.add(new UnsafeDependenceOnBlock());
//        allPatterns.add(new UnsafeDependenceOnGas());
//        allPatterns.add(new UseOfOrigin());
//        allPatterns.add(new WriteOnly());

        if (!Strings.isNullOrEmpty(args.patterns)) {
            List<String> wantedPatterns = new LinkedList<>();
            for (String patternName : args.patterns.split(",")) {
                String tmp = patternName.trim().toLowerCase();
                wantedPatterns.add(tmp);
            }
            for (AbstractPattern pattern : allPatterns) {
                if (wantedPatterns.contains(pattern.getClass().getSimpleName().toLowerCase())) {
                    patterns.add(pattern);
                }
            }
        } else {
            patterns = allPatterns;
        }

    }

    /**
     * Analyze a contract with patterns.
     *
     * @param instructions decompiled contract instructions
     * @return Map patterns to the match result.
     */
    private static void checkPatterns(List<Instruction> instructions, String livestatusfile) throws IOException, InterruptedException {
        patterns.forEach(pattern -> contractResult.patternResults.put(pattern.getClass().getSimpleName(), new PatternResult()));
        updateContractAnalysisStatus(livestatusfile);

        boolean methodsDecompiled = (instructions.stream().anyMatch(instruction -> instruction instanceof _VirtualMethodHead));

        if (!methodsDecompiled) {
            // no methods, compute a single global dataflow fixpoint and check all patterns
            log.println("Computing global dataflow fixpoint over the entire contract...");
            AbstractDataflow dataflow = DataflowFactory.getDataflow(instructions);
            for (AbstractPattern pattern : patterns) {
                if (pattern instanceof MissingInputValidation) {
                    if (!methodsDecompiled) {
                        PatternResult status = contractResult.patternResults.get(MissingInputValidation.class.getSimpleName());
                        status.completed = true;
                        status.error = "not supported";
                        continue;
                    }
                }
                try {
                    checkInstructions(instructions, instructions, pattern, dataflow, livestatusfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            dataflow.dispose();
        } else {
            // split instructions into methods and check them independently
            for (List<Instruction> body : splitInstructionsIntoMethods(instructions)) {
                log.println("Analyzing method with " + body.size() + " instructions:");
                DecompilationPrinter.printInstructions(body, log);

                log.println("Computing dataflow fixpoint over the method body...");
                AbstractDataflow bodyDataflow = DataflowFactory.getDataflow(body);
                for (AbstractPattern pattern : patterns) {
                    if (!(pattern instanceof AbstractInstructionPattern))
                        continue;

                    try {
                        checkInstructions(body, instructions, pattern, bodyDataflow, livestatusfile);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                bodyDataflow.dispose();
            }

            log.println("Computing global dataflow fixpoint over the entire contract...");
            AbstractDataflow globalDataflow = DataflowFactory.getDataflow(instructions);
            for (AbstractPattern pattern : patterns) {
                if (!(pattern instanceof AbstractContractPattern))
                    continue;

                try {
                    checkInstructions(instructions, instructions, pattern, globalDataflow, livestatusfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            globalDataflow.dispose();
        }

    }


    private static void checkInstructions(List<Instruction> methodInstructions, List<Instruction> contractInstructions, AbstractPattern pattern, AbstractDataflow dataflow, String livestatusfile) {
        log.println();

        PatternResult status = contractResult.patternResults.get(pattern.getClass().getSimpleName());

        log.println("Checking pattern " + pattern.getClass().getSimpleName() + ": ");

        try {
            pattern.checkPattern(methodInstructions, contractInstructions, dataflow);
        } catch (Exception e) {
            status.error = e instanceof UnsupportedOperationException ? "not supported" : "analysis failed";
            log.print(e.getStackTrace());
        }

        status.completed = true;
        pattern.getViolations().stream()
                .filter(instruction -> instruction.getRawInstruction() != null)
                .forEach(instruction -> status.addViolation(instruction.getRawInstruction().instrNumber));
        pattern.getWarnings().stream()
                .filter(instruction -> instruction.getRawInstruction() != null)
                .forEach(instruction -> status.addWarning(instruction.getRawInstruction().instrNumber));
        pattern.getSafe().stream()
                .filter(instruction -> instruction.getRawInstruction() != null)
                .forEach(instruction -> status.addSafe(instruction.getRawInstruction().instrNumber));
        pattern.getConflicts().stream()
                .filter(instruction -> instruction.getRawInstruction() != null)
                .forEach(instruction -> status.addConflict(instruction.getRawInstruction().instrNumber));

        log.println("\tViolations:" + pattern.getViolations().stream().map(Object::toString).collect(Collectors.joining("\n\t\t")));
        log.println("\tWarnings: " + pattern.getWarnings().stream().map(Object::toString).collect(Collectors.joining("\n\t\t")));
        log.println("\tSafe: " + pattern.getSafe().stream().map(Object::toString).collect(Collectors.joining("\n\t\t")));
        log.println("\tConflicts: " + pattern.getConflicts().stream().map(Object::toString).collect(Collectors.joining("\n\t\t")));
        log.println();

        updateContractAnalysisStatus(livestatusfile);
    }

    public static List<List<Instruction>> splitInstructionsIntoMethods(List<Instruction> instructions) {
        List<List<Instruction>> methodBodies = new LinkedList<>();
        List<Instruction> methodBody = new LinkedList<>();
        for (Instruction instr : instructions) {
            if (instr instanceof _VirtualMethodHead) {
                if (methodBody.get(0) instanceof _VirtualMethodHead) { // && ((_VirtualMethodHead) methodBody.get(0)).getLabel().startsWith(_VirtualMethodHead.METHOD_NAME_PREFIX_ABI)) {
                    // add public method
                    methodBodies.add(methodBody);
                }
                methodBody = new LinkedList<>();
            }
            methodBody.add(instr);
        }
        methodBodies.add(methodBody);

        if (methodBodies.size() == 0) {
            // failed to find method bodies, treat as one single method
            methodBodies.add(instructions);
        }
        return methodBodies;
    }

    private static void updateContractAnalysisStatus(String livestatusfile) {
        if (livestatusfile == null)
            return;

        try (Writer writer = new FileWriter(livestatusfile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(contractResult, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
