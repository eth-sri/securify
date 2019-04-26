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


package ch.securify.analysis;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.*;
import ch.securify.utils.BigIntUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.io.Resources.copy;
import static com.google.common.io.Resources.getResource;

public abstract class AbstractDataflow {

    public static final int UNK_CONST_VAL = -1;

    abstract public int mayFollow(Instruction instr1, Instruction instr2);
    abstract public int instrMayDepOn(Instruction instr, Object type);
    abstract public int varMayDepOn(Instruction instr1, Variable lhs, Object type);
    abstract public int memoryMayDepOn(Instruction instr1, int offset, Object type);
    abstract public int memoryMayDepOn(Instruction instr, Object type);
    abstract public int mustPrecede(Instruction instr1, Instruction instr2);
    abstract public int varMustDepOn(Instruction instr1, Variable lhs, Object type);
    abstract public int memoryMustDepOn(Instruction instr1, int offset, Object type);

    abstract protected void deriveFollowsPredicates();
    abstract protected void deriveIfPredicates();
    abstract protected void createSLoadRule(Instruction instr, Variable index, Variable var);
    abstract protected void createMLoadRule(Instruction instr, Variable offset, Variable var);

    protected List<Instruction> instructions;

    protected BiMap<Variable, Integer> varToCode;
    protected BiMap<Instruction, Integer> instrToCode;
    protected BiMap<Class, Integer> typeToCode;
    protected BiMap<Integer, Integer> constToCode;

    protected BiMap<Integer, Variable> offsetToStorageVar;
    protected BiMap<Integer, Variable> offsetToMemoryVar;
    protected BiMap<String, StringBuffer> ruleToSB;
    protected Map<String, Set<Long>> fixedpoint;

    protected int bvCounter = 0; // reserve first 100 for types

    public int unk;

    protected final boolean DEBUG = false;

    // input predicates
    private static String DL_FOLDER;
    private String WORKSPACE, WORKSPACE_OUT;

    public static void setDlFolder(String folder) {
        DL_FOLDER = Objects.requireNonNull(folder);
    }

    /**
     * Extract the Soufflé binaries and store them in a temporary folder to allow them to be executed
     *
     * @throws IOException
     */
    private static void extractSouffleBinaries() throws IOException {
        String[] names = {MustExplicitDataflow.binaryName, MayImplicitDataflow.binaryName };
        String souffleDir = Files.createTempDirectory("binaries_souffle").toFile().getAbsolutePath();
        setDlFolder(souffleDir);
        for(String resourceName : names) {
            File binaryPath = Paths.get(souffleDir, resourceName).toFile();
            OutputStream os = new FileOutputStream(binaryPath);
            copy(getResource(resourceName), os);
            os.close();
            if (!binaryPath.setExecutable(true)) {
                throw new IOException("Could not set the executable bit of a souffle binary in " + souffleDir);
            }
        }
    }

    protected void initDataflow(String binaryName) throws IOException, InterruptedException {
        if (DL_FOLDER == null) {
            extractSouffleBinaries();
        }

        String DL_EXEC = DL_FOLDER + "/" + binaryName;

        varToCode = HashBiMap.create();
        instrToCode = HashBiMap.create();
        typeToCode = HashBiMap.create();
        constToCode = HashBiMap.create();
        fixedpoint = new HashMap<>();

        offsetToStorageVar = HashBiMap.create();
        offsetToMemoryVar = HashBiMap.create();

        ruleToSB = HashBiMap.create();
        ruleToSB.put("assignVar", new StringBuffer());
        ruleToSB.put("assignType", new StringBuffer());
        ruleToSB.put("taint", new StringBuffer());
        ruleToSB.put("follows", new StringBuffer());
        ruleToSB.put("jump", new StringBuffer());
        ruleToSB.put("tag", new StringBuffer());
        ruleToSB.put("oneBranchTag", new StringBuffer());
        ruleToSB.put("join", new StringBuffer());
        ruleToSB.put("endIf", new StringBuffer());
        ruleToSB.put("mload", new StringBuffer());
        ruleToSB.put("mstore", new StringBuffer());
        ruleToSB.put("sload", new StringBuffer());
        ruleToSB.put("sstore", new StringBuffer());
        ruleToSB.put("isStorageVar", new StringBuffer());
        ruleToSB.put("sha3", new StringBuffer());
        ruleToSB.put("unk", new StringBuffer());

        unk = getCode(UNK_CONST_VAL);
        appendRule("unk", unk);

        log("Souffle Analysis");

        File fWORKSPACE = (new File(System.getProperty("java.io.tmpdir"), "souffle-" + UUID.randomUUID()));
        if (!fWORKSPACE.mkdir()) {
            throw new IOException("Could not create temporary directory");
        }
        WORKSPACE = fWORKSPACE.getAbsolutePath();

        File fWORKSPACE_OUT = new File(WORKSPACE + "_OUT");
        if (!fWORKSPACE_OUT.mkdir()) {
            throw new IOException("Could not create temporary directory");
        }
        WORKSPACE_OUT = fWORKSPACE_OUT.getAbsolutePath();

        deriveAssignVarPredicates();
        deriveAssignTypePredicates();
        deriveHeapPredicates();
        deriveStorePredicates();

        deriveFollowsPredicates();
        deriveIfPredicates();

        createProgramRulesFile();
        log("Number of instructions: " + instrToCode.size());
        log("Threshold: " + Config.THRESHOLD_COMPILE);

        long start = System.currentTimeMillis();
        runCommand(new String[]{DL_EXEC, "-j", Integer.toString(Runtime.getRuntime().availableProcessors()), "-F", WORKSPACE, "-D", WORKSPACE_OUT});

        long elapsedTime = System.currentTimeMillis() - start;
        String elapsedTimeStr = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
                TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))
        );

        log(elapsedTimeStr);
    }

    public static int getInt(byte[] data) {
        byte[] bytes = new byte[4];
        System.arraycopy(data, 0, bytes, 4 - Math.min(data.length, 4), Math.min(data.length, 4));
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        return bb.getInt();
    }

    protected static long Encode(CSVRecord record) {
        assert(record.size() <= 3);
        long entry = 0;
        for (int i = 0; i < record.size(); i++) {
            //entry.add(Integer.parseInt(record.get(i)));
            entry *= 80000;
            entry += (long)Integer.parseInt(record.get(i));
        }
        assert(entry >= 0);
        return entry;
    }

    protected static long Encode(Integer... args) {
        assert(args.length <= 3);
        long entry = 0;
        for (Integer arg : args) {
            entry *= 80000;
            entry += (long)arg;
        }
        assert(entry >= 0);
        return entry;
    }

    protected void createProgramRulesFile() {
        for (String rule : ruleToSB.keySet()) {
            BufferedWriter bwr;
            try {
                bwr = new BufferedWriter(new FileWriter(new File(WORKSPACE + "/" + rule + ".facts")));
                bwr.write(ruleToSB.get(rule).toString());
                bwr.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param rootPath The directory to delete
     * @throws IOException From the walk function
     */
    private void deleteDirectory(Path rootPath) throws IOException {
        if (Files.walk(rootPath).sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .map(File::delete).anyMatch(e -> !e)) {
            throw new IOException("Failure while deleting files");
        }
    }

    public void dispose() throws IOException, InterruptedException {
        deleteDirectory(Paths.get(WORKSPACE));
        deleteDirectory(Paths.get(WORKSPACE_OUT));
    }

    protected void readFixedpoint(String ruleName) throws IOException {
        Reader in = new FileReader(WORKSPACE_OUT + "/" + ruleName + ".csv");
        /* Tab-delimited format */
        Iterable<CSVRecord> records = CSVFormat.TDF.parse(in);

        Set<Long> entries = new HashSet<>();

        records.forEach(record -> entries.add(Encode(record)));
        in.close();

        fixedpoint.put(ruleName, entries);
    }

    protected int runQuery(String ruleName, Integer... args) {
        try {
            if (!fixedpoint.containsKey(ruleName)) {
                readFixedpoint(ruleName);
            }
            if (fixedpoint.get(ruleName).contains(Encode(args))) {
                return Status.SATISFIABLE;
            } else {
                return Status.UNSATISFIABLE;
            }
        } catch (IOException e) {
            log("Souffle TIMEOUT, returns UNKNOWN");
            return Status.UNKNOWN;
        }
    }

    public static void runCommand(String[] command) throws IOException, InterruptedException {
        // Souffle works with this PATH
        String[] envp = {"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"};

        Process proc = Runtime.getRuntime().exec(command, envp);

        if (!proc.waitFor(Config.PATTERN_TIMEOUT, TimeUnit.SECONDS)){ 
            proc.destroyForcibly();
            throw new IOException("Timeout for " + String.join(" ", command));
        }
        if (proc.exitValue() != 0) {
            proc.destroyForcibly();
            throw new IOException("Exit Value " + proc.exitValue() + " for " +  String.join(" ", command));
        }
    }

    public Variable getStorageVarForIndex(int index) {
        if (!offsetToStorageVar.containsKey(index)) {
            Variable newVar = new Variable();
            offsetToStorageVar.put(index, newVar);
            appendRule("isStorageVar", getCode(newVar));
            return newVar;
        }
        return offsetToStorageVar.get(index);
    }

    protected Variable getMemoryVarForIndex(int index) {
        if (!offsetToMemoryVar.containsKey(index)) {
            Variable newVar = new Variable();
            offsetToMemoryVar.put(index, newVar);
            return newVar;
        }
        return offsetToMemoryVar.get(index);
    }

    protected void log(String msg) {
        if (DEBUG)
            System.out.println(this.getClass().getSimpleName() + ": " + msg);
    }

    protected void createMStoreRule(Instruction instr, Variable offset, Variable var) {
        int offsetCode;
        if (offset.hasConstantValue()) {
            log("Offset " + offset + ", int offset " + getInt(offset.getConstantValue()) + "memory var " + getMemoryVarForIndex(getInt(offset.getConstantValue())) + ", code " + getCode(getMemoryVarForIndex(getInt(offset.getConstantValue()))));
            offsetCode = getCode(getMemoryVarForIndex(getInt(offset.getConstantValue())));
        } else {
            offsetCode = unk;
        }

        appendRule("mstore", getCode(instr), offsetCode, getCode(var));
    }

    protected void createSStoreRule(Instruction instr, Variable index, Variable var) {
        int indexCode;
        if (index.hasConstantValue()) {
            indexCode = getCode(getStorageVarForIndex(getInt(index.getConstantValue())));
        } else {
            indexCode = unk;
        }
        appendRule("sstore", getCode(instr), indexCode, getCode(var));
    }

    protected void createAssignVarRule(Instruction instr, Variable output, Variable input) {
        appendRule("assignVar", getCode(instr), getCode(output), getCode(input));
    }

    protected void createAssignTypeRule(Instruction instr, Variable var, Class type) {
        appendRule("assignType", getCode(instr), getCode(var), getCode(type));
    }

    protected void createAssignTopRule(Instruction instr, Variable var) {
        appendRule("assignType", getCode(instr), getCode(var), unk);
    }

    protected void createEndIfRule(Instruction start, Instruction end) {
        appendRule("endIf", getCode(start), getCode(end));
    }

    protected void appendRule(String ruleName, Object... args) {
        StringBuffer sb;
        if (ruleToSB.containsKey(ruleName)) {
            sb = ruleToSB.get(ruleName);
        } else {
            throw new RuntimeException("unknown rule: " + ruleName);
        }
        for (int i = 0; i < args.length - 1; i++) {
            sb.append(args[i]);
            sb.append("\t");
        }
        sb.append(args[args.length - 1]);
        sb.append("\n");
    }

    protected int getFreshCode() {
        if (bvCounter == Integer.MAX_VALUE) {
            throw new RuntimeException("Integer overflow.");
        }
        int freshCode = bvCounter;
        bvCounter++;
        return freshCode;
    }

    protected int getCode(Variable var) {
        if (!varToCode.containsKey(var))
            varToCode.put(var, getFreshCode());
        return varToCode.get(var);
    }

    protected int getCode(Instruction instr) {
        if (!instrToCode.containsKey(instr))
            instrToCode.put(instr, getFreshCode());
        return instrToCode.get(instr);
    }

    protected int getCode(Class instructionClass) {
        if (!typeToCode.containsKey(instructionClass))
            typeToCode.put(instructionClass, getFreshCode());
        return typeToCode.get(instructionClass);
    }

    protected int getCode(Integer constVal) {
        if (!constToCode.containsKey(constVal))
            constToCode.put(constVal, getFreshCode());
        return constToCode.get(constVal);
    }

    protected int getCode(Object o) {
        if (o instanceof Instruction) {
            return getCode((Instruction) o);
        } else if (o instanceof Class) {
            return getCode((Class) o);
        } else if (o instanceof Integer) {
            return getCode((Integer) o);
        } else if (o instanceof Variable) {
            return getCode((Variable) o);
        } else {
            throw new RuntimeException("Not supported object of a bit vector");
        }
    }

    protected void deriveAssignTypePredicates() {
        log(">> Derive AssignType predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof Push
                    || instr instanceof CallValue
                    || instr instanceof Caller
                    || instr instanceof CallDataLoad
                    || instr instanceof CallDataSize
                    || instr instanceof Coinbase
                    || instr instanceof Gas
                    || instr instanceof IsZero
                    || instr instanceof Not
                    || instr instanceof BlockTimestamp
                    || instr instanceof BlockNumber
                    || instr instanceof GasLimit
                    || instr instanceof GasPrice
                    || instr instanceof Balance
                    || instr instanceof Difficulty
                    || instr instanceof SLoad
                    || instr instanceof Address) {
                createAssignTypeRule(instr, instr.getOutput()[0], instr.getClass());
            } else if (instr instanceof Div) {
                if (instr.getInput()[1].hasConstantValue() &&
                        (getInt(instr.getInput()[1].getConstantValue()) == 1
                                // X = Y / 1 , do not taint as value of X does not depend on division in this case
                                || (instr.getInput()[1].getConstantValue().length == 29 && instr.getInput()[1].getConstantValue()[0] == 1)
                                || getInt(instr.getInput()[1].getConstantValue()) == 32
                                || getInt(instr.getInput()[1].getConstantValue()) == 2
                                // X = Y / 10^29 , do not taint as value of X because div by 10^29 is often used for aligning
                        )) {

                    continue;
                }
                createAssignTypeRule(instr, instr.getOutput()[0], instr.getClass());
            } else if (instr instanceof _VirtualMethodHead) {
                for (Variable arg : instr.getOutput()) {
                    log("Type of " + arg + " is unk");
                    createAssignTopRule(instr, arg);
                    // assign the arguments as an abstract type (to check later
                    // for missing input validation)
                    appendRule("assignType", getCode(instr), getCode(arg), getCode(arg));
                    // tag the arguments to depend on user input (CallDataLoad)
                    createAssignTypeRule(instr, arg, CallDataLoad.class);
                }
            } else if (instr instanceof Call || instr instanceof StaticCall) {
                log("Type of " + instr.getOutput()[0] + " is Call");
                createAssignTopRule(instr, instr.getOutput()[0]);
                // assign the return value as an abstract type (to check later
                // for unhandled exception)
                appendRule("assignType", getCode(instr), getCode(instr.getOutput()[0]), getCode(instr.getOutput()[0]));
            } else if (instr instanceof BlockHash) {
                log("Type of " + instr.getOutput()[0] + " is BlockHash");
                createAssignTypeRule(instr, instr.getOutput()[0], instr.getClass());
                // TODO: double check whether to propagate the type of the
                // argument to the output of blockhash
                createAssignVarRule(instr, instr.getOutput()[0], instr.getInput()[0]);
            } else if (instr instanceof ReturnDataCopy) {
                // TODO: New memory-based rule here
            }
        }
    }

    protected void deriveHeapPredicates() {
        log(">> Derive MStore and MLoad predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof MStore || instr instanceof MStore8) {
                Variable var = instr.getInput()[1];
                Variable offset = instr.getInput()[0];
                log("mstore instruction: " + instr.getStringRepresentation());
                createMStoreRule(instr, offset, var);
            }
            if (instr instanceof MLoad) {
                log("mload instruction: " + instr.getStringRepresentation());
                Variable var = instr.getOutput()[0];
                Variable offset = instr.getInput()[0];
                createMLoadRule(instr, offset, var);
            }
        }
    }


    protected void deriveStorePredicates() {
        log(">> Derive SStore and SLoad predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof SStore) {
                Variable index = instr.getInput()[0];
                Variable var = instr.getInput()[1];
                log("sstore instruction: " + instr.getStringRepresentation());
                createSStoreRule(instr, index, var);
            }
            if (instr instanceof SLoad) {
                Variable var = instr.getOutput()[0];
                Variable index = instr.getInput()[0];
                log("sload instruction" + instr.getStringRepresentation());
                createSLoadRule(instr, index, var);
            }
        }
    }


    protected void deriveAssignVarPredicates() {
        log(">> Derive assign predicates <<");
        for (Instruction instr : instructions) {
            log(instr.getStringRepresentation());

            if (instr instanceof SLoad) {
                Variable storageOffset = instr.getInput()[0];
                Variable lhs = instr.getOutput()[0];
                if (storageOffset.hasConstantValue()) {
                    int storageOffsetValue = getInt(storageOffset.getConstantValue());
                    Variable storageVar = getStorageVarForIndex(storageOffsetValue);

                    // big hack: adding an assignType predicate below
                    appendRule("assignType", getCode(instr), getCode(lhs), getCode(storageVar));
                } else {
                    appendRule("assignType", getCode(instr), getCode(lhs), unk);
                }
            }

            if (instr instanceof MLoad) {
                Variable memoryOffset = instr.getInput()[0];
                Variable lhs = instr.getOutput()[0];
                if (memoryOffset.hasConstantValue()) {
                    int memoryOffsetValue = getInt(memoryOffset.getConstantValue());
                    Variable memoryVar = getMemoryVarForIndex(memoryOffsetValue);

                    // big hack: adding an assignType predicate below
                    appendRule("assignType", getCode(instr), getCode(lhs), getCode(memoryVar));
                } else {
                    appendRule("assignType", getCode(instr), getCode(lhs), unk);
                }
            }

            if (instr instanceof Call || instr instanceof StaticCall) {
                createAssignVarRule(instr, instr.getOutput()[0], instr.getInput()[2]);
            }

            if (instr instanceof Sha3) {
                if (instr.getInput()[0].hasConstantValue() && instr.getInput()[1].hasConstantValue()) {
                    int startOffset = getInt(instr.getInput()[0].getConstantValue());
                    int length = getInt(instr.getInput()[1].getConstantValue());
                    //assert(startOffset % 32 == 0);
                    for (int offset = startOffset; offset < startOffset + length; offset += 4) {
                        log("sha3: " + instr + " " + instr.getOutput()[0]);
                        log("Offset " + offset + ", memory var " + getMemoryVarForIndex(offset) + ", code " + getCode(getMemoryVarForIndex(offset)));
                        appendRule("sha3", getCode(instr), getCode(instr.getOutput()[0]), getCode(getMemoryVarForIndex(offset)));
                    }
                } else {
                    // propagate the entire heap to the output of SHA3
                }
            }


            // Skip MSTORE/MLOAD SSTORE/SLOAD as these are handled in a special
            // way
            if (instr instanceof MStore
                    || instr instanceof MLoad
                    || instr instanceof SStore
                    || instr instanceof SLoad
                    || instr instanceof Call
                    || instr instanceof StaticCall
                    || instr instanceof Sha3) {
                continue;
            }

            if (instr instanceof Or) {
                // a = b | c; if b or c is 0, do not propagate their types.
                for (Variable output : instr.getOutput()) {
                    for (Variable input : instr.getInput()) {
                        if (input.hasConstantValue()) {
                            BigInteger val = BigIntUtil.fromInt256(input.getConstantValue());
                            if (val.compareTo(BigInteger.ZERO) == 0) {
                                // Do not propagate this input
                                continue;
                            }
                        }
                        createAssignVarRule(instr, output, input);
                    }
                }
                continue;
            }

            for (Variable output : instr.getOutput()) {
                for (Variable input : instr.getInput()) {
                    createAssignVarRule(instr, output, input);
                }
            }
        }
    }

}
