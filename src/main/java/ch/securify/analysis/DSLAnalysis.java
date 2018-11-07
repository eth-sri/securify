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
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DSLAnalysis {

    public static final int UNK_CONST_VAL = -1;

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

    protected String WORKSPACE, WORKSPACE_OUT;
    protected final String SOUFFLE_COMPILE_FLAG = "-c", SOUFFLE_BIN = "souffle";
    protected String SOUFFLE_RULES;
    protected final String TIMEOUT_COMMAND = System.getProperty("os.name").toLowerCase().startsWith("mac") ? "gtimeout" : "timeout";

    public DSLAnalysis(List<Instruction> decompiledInstructions) throws IOException, InterruptedException {
        SOUFFLE_RULES = "smt_files/allInOneAnalysis.dl";
        instructions = decompiledInstructions;
        initDataflow();
    }

    protected boolean isSouffleInstalled() {
        try {
            Process process = new ProcessBuilder(SOUFFLE_BIN).start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    protected void initDataflow() throws IOException, InterruptedException {
        if(! isSouffleInstalled()){
            System.err.println("Soufflé does not seem to be installed.");
            System.exit(7);
        }


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
        ruleToSB.put("jumpDest", new StringBuffer());
        ruleToSB.put("oneBranchJumpDest", new StringBuffer());
        ruleToSB.put("join", new StringBuffer());
        ruleToSB.put("endIf", new StringBuffer());
        ruleToSB.put("mload", new StringBuffer());
        ruleToSB.put("mstore", new StringBuffer());
        ruleToSB.put("sload", new StringBuffer());
        ruleToSB.put("sstore", new StringBuffer());
        //ruleToSB.put("isStorageVar", new StringBuffer());
        ruleToSB.put("sha3", new StringBuffer());
        ruleToSB.put("unk", new StringBuffer());

        unk = getCode(UNK_CONST_VAL);
        appendRule("unk", unk);

        log("Souffle Analysis");

        // create workspace
        Random rnd = new Random();
        WORKSPACE = (new File(System.getProperty("java.io.tmpdir"), "souffle-" + UUID.randomUUID())).getAbsolutePath();
        WORKSPACE_OUT = WORKSPACE + "_OUT";
        runCommand("mkdir " + WORKSPACE);
        runCommand("mkdir " + WORKSPACE_OUT);

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
        if (instructions.size() > Config.THRESHOLD_COMPILE) {
            /* compile Souffle program and run */
            String cmd = TIMEOUT_COMMAND + " " + Config.PATTERN_TIMEOUT + "s " + SOUFFLE_BIN + " -w -F " + WORKSPACE + " -D " + WORKSPACE_OUT + " " + SOUFFLE_COMPILE_FLAG + " "
                    + SOUFFLE_RULES;
            log(cmd);
            runCommand(cmd);
        } else {
            /* run interpreter */
            String cmd = TIMEOUT_COMMAND + " " + Config.PATTERN_TIMEOUT + "s " + SOUFFLE_BIN + " -w -F " + WORKSPACE + " -D " + WORKSPACE_OUT + " " + SOUFFLE_RULES;
            log(cmd);
            runCommand(cmd);
        }
        long elapsedTime = System.currentTimeMillis() - start;
        String elapsedTimeStr = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(elapsedTime),
                TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime))
        );
        log(elapsedTimeStr);
    }

    public static int getInt(byte[] data) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < Math.min(data.length, 4); ++i) {
            bytes[i + 4 - Math.min(data.length, 4)] = data[i];
        }
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
        for (int i = 0; i < args.length; i++) {
            entry *= 80000;
            entry += (long)args[i];
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

    public void dispose() throws IOException, InterruptedException {
        runCommand("rm -r " + WORKSPACE);
        runCommand("rm -r " + WORKSPACE_OUT);
    }

    protected void readFixedpoint(String ruleName) throws IOException {

        Reader in = new FileReader(WORKSPACE_OUT + "/" + ruleName + ".csv");
        /* Tab-delimited format */
        Iterable<CSVRecord> records = CSVFormat.TDF.parse(in);
        Set<Long> entries = new HashSet<Long>(100000000);

        long count = 0;
        for (CSVRecord record : records) {
            count += 1;
            entries.add(Encode(record));
        }
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
        } catch (FileNotFoundException e) {
            log("Souffle TIMEOUT, returns UNKNOWN");
            return Status.UNKNOWN;
        } catch (IOException e) {
            log("Souffle TIMEOUT, returns UNKNOWN");
            return Status.UNKNOWN;
        }
    }

    protected String runCommand(String command) throws IOException, InterruptedException {
        Process proc;
        String result = "";
        log("CMD: " + command);

        // Souffle works with this PATH
        String[] envp = {"PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/home/dani/bin"};
        proc = Runtime.getRuntime().exec(command, envp);

        // Read the output
        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        // Output of the command
        {
            String line;
            while ((line = reader.readLine()) != null) {
                result = result + line + "\n";
                log(line);
            }
        }

        // Display the errors
        {
            String line;
            reader = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

            while ((line = reader.readLine()) != null) {
                result = result + line + "\n";
                log(line);
            }
        }

        proc.waitFor();
        if (proc.exitValue() != 0) {
            throw new IOException();
        }
        return result;
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

    protected void createAssignVarMayImplicitRule(Instruction instr, Variable output, Variable input) {
        appendRule("assignVarMayImplicit", getCode(instr), getCode(output), getCode(input));
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
            } else if (instr instanceof Call) {
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

            if (instr instanceof Call) {
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

    protected void deriveFollowsPredicates() {
        log(">> Derive follows predicates <<");
        for (Instruction instr : instructions) {

            if (instr instanceof JumpDest) {
                if (((JumpDest) instr).getIncomingBranches().size() == 1 && instr.getPrev() == null) {
                    log("One-Branch Tag fact: " + instr);
                    appendRule("oneBranchJumpDest", getCode(instr));
                }
                log("Tag fact (jumpDest): " + instr);
                appendRule("jumpDest", getCode(instr));
            }

            if (instr instanceof BranchInstruction) {
                BranchInstruction branchInstruction = (BranchInstruction) instr;
                for (Instruction outgoingInstruction : branchInstruction.getOutgoingBranches()) {
                    if (!(outgoingInstruction instanceof _VirtualMethodHead)) {
                        createFollowsRule(instr, outgoingInstruction);
                    }
                }
            }
            Instruction nextInstruction = instr.getNext();

            if (nextInstruction != null) {
                createFollowsRule(instr, nextInstruction);
            }
        }
    }

    private void createFollowsRule(Instruction from, Instruction to) {
        if (from instanceof JumpI) {
            Instruction mergeInstruction = ((JumpI)from).getMergeInstruction();
            if (mergeInstruction == null) {
                mergeInstruction = new JumpDest("BLACKHOLE");
            }
            if (!(to instanceof JumpDest)) {
                appendRule("follows", getCode(from), getCode(to));
            }
            appendRule("jump", getCode(from), getCode(to), getCode(mergeInstruction));
        } else if (from instanceof Jump) {
            // need to use a jump, not follows because follows ignores the TO if it is of type Tag, see Datalog rules
            appendRule("jump", getCode(from), getCode(to), getCode(to));
        } else {
            appendRule("follows", getCode(from), getCode(to));
        }

        if (to instanceof JumpDest) {
            //appendRule("join", getCode(from), getCode(to));
            List<Instruction> incomingBranches = new ArrayList<Instruction>(((JumpDest) to).getIncomingBranches());
            if (to.getPrev() != null) {
                incomingBranches.add(to.getPrev());
            }
            log("JumpDest: " + to + " with incoming branches: " + incomingBranches);

//            if (incomingBranches.size() == 1) {
//                //
//                appendRule("jump", getCode(incomingBranches.get(0)), getCode(to), getCode(new JumpDest("BLACKHOLE"))    );
//            } else {
            Instruction lastJoinInstruction = incomingBranches.get(0);
            for (int i = 1; i < incomingBranches.size() - 1; ++i) {
                Instruction tmpJoinInstruction = new JumpDest(to.toString() + "_tmp_" + i);
                appendRule("join", getCode(lastJoinInstruction),
                        getCode(incomingBranches.get(i)),
                        getCode(tmpJoinInstruction));
                lastJoinInstruction = tmpJoinInstruction;
            }
            appendRule("join", getCode(lastJoinInstruction),
                    getCode(incomingBranches.get(incomingBranches.size()-1)),
                    getCode(to));
//            }
        }
    }

    protected void deriveIfPredicates() {
        log(">> Derive TaintElse and TaintThen predicates <<");
        for (Instruction instr : instructions) {
            if (instr instanceof JumpI) {
                JumpI ifInstr = (JumpI) instr;
                Variable condition = ifInstr.getCondition();
                Instruction thenInstr = ifInstr.getTargetInstruction();
                Instruction elseInstr = ifInstr.getNext();
                Instruction mergeInstr = ifInstr.getMergeInstruction();

                if (thenInstr != null && thenInstr != mergeInstr) {
                    log("then instruction: " + thenInstr.getStringRepresentation());
                    createTaintRule(instr, thenInstr, condition);
                }

                if (elseInstr != null && elseInstr != mergeInstr ) {
                    log("else instruction: " + elseInstr.getStringRepresentation());
                    createTaintRule(instr, elseInstr, condition);
                }

                if (mergeInstr != null) {
                    log("merge instruction: " + mergeInstr.getStringRepresentation());
                    createEndIfRule(instr, mergeInstr);
                }
            }
        }
    }

    private void createTaintRule(Instruction labStart, Instruction lab, Variable var) {
        appendRule("taint", getCode(labStart), getCode(lab), getCode(var));
    }

    protected void createSLoadRule(Instruction instr, Variable index, Variable var) {
        int indexCode;
        if (index.hasConstantValue()) {
            indexCode = getCode(getInt(index.getConstantValue()));
        } else {
            indexCode = unk;
            // if you have "var = sload(index)", to propagate labels from index to var we add "var = index"
            createAssignVarMayImplicitRule(instr, var, index);
        }
        appendRule("sload", getCode(instr), indexCode, getCode(var));
    }

    protected void createMLoadRule(Instruction instr, Variable offset, Variable var) {
        int offsetCode;
        if (offset.hasConstantValue()) {
            offsetCode = getCode(getMemoryVarForIndex(getInt(offset.getConstantValue())));
        } else {
            offsetCode = unk;
            createAssignVarMayImplicitRule(instr, var, offset);
        }
        appendRule("mload", getCode(instr), offsetCode, getCode(var));
    }
}
