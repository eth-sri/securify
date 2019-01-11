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


package ch.securify.decompiler;

import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.instructions.*;
import ch.securify.utils.DevNullPrintStream;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import java.io.PrintStream;
import java.util.*;

public class DecompilerFallback extends AbstractDecompiler {

    private static PrintStream log;
    private static final boolean DEBUG = false;

    public static List<Instruction> decompile(final byte[] bytecode, final PrintStream _log) {

        if (DEBUG) {
            log = _log;
        } else {
            log = new DevNullPrintStream();
        }

        // raw EVM instructions
        RawInstruction[] rawInstructions = new RawInstruction[bytecode.length + 1];
        /* Ordered list of bytecode offsets of JUMPDEST instructions. */
        List<Integer> jumpDestinations = new ArrayList<>();

        parseRawInstructions(bytecode, rawInstructions, jumpDestinations);

        BiMap<Integer, String> tags = findTags(log, jumpDestinations);

        ControlFlowDetector controlFlowDetector = new ControlFlowDetector();
        /* Map bytecode offsets of JUMP/JUMPI instructions to known bytecode offsets of JUMPDEST instructions.
         * These are the edges of the control flow graph the correspond to explicit jumps.
         * assumption: there are no dynamic jumps that may have multiple jump targets. */
        Multimap<Integer, Integer> mapJumpsToDests = HashMultimap.create();

        Multimap<Integer, Integer> controlFlowGraph = dectectControlFlow(log, rawInstructions, jumpDestinations, tags,
                controlFlowDetector, mapJumpsToDests);

        // Decompile the whole thing
        log.println();
        log.println("Decompiling...");
        {
            DestackerFallback destacker = new DestackerFallback();

            InstructionFactory instructionFactory = new InstructionFactory()
                    .setJumpResolver(instruction -> tags.get(Iterables.getFirst(mapJumpsToDests.get(instruction.offset), -42)))
                    .setLabelResolver(instruction -> tags.get(instruction.offset));

            destacker.decompile(rawInstructions, instructionFactory, mapJumpsToDests, controlFlowGraph, log, tags);

            List<Instruction> decompiledInstructions = new LinkedList<>();

            {
                Instruction[] decompiledInstrs = destacker.getInstructions();

                Queue<Instruction> branchesToProcess = new LinkedList<>();
                branchesToProcess.add(decompiledInstrs[0]);
                Set<Instruction> processedBranches = new HashSet<>();

                while (!branchesToProcess.isEmpty()) {
                    Instruction instruction = branchesToProcess.poll();
                    if (processedBranches.contains(instruction)) {
                        // branch already processed
                        continue;
                    }
                    do {
                        if (instruction instanceof JumpDest) {
                            if (processedBranches.contains(instruction)) {
                                // reached merger through linear flow
                                break;
                            }
                            processedBranches.add(instruction);
                        }
                        decompiledInstructions.add(instruction);
                        if (instruction instanceof BranchInstruction) {
                            branchesToProcess.addAll(((BranchInstruction) instruction).getOutgoingBranches());
                        }
                    } while ((instruction = instruction.getNext()) != null);
                }
            }

            log.println("Remove unused instructions...");

            // removing bytecode ops that have been noop'd (dup, swap, pop)
            decompiledInstructions.removeIf(instruction -> {
                if (instruction instanceof _NoOp) {
                    instruction.getPrev().setNext(instruction.getNext());
                    instruction.getNext().setPrev(instruction.getPrev());
                    return true;
                }
                return false;
            });

            // remove unused jumpdests (labels)
            decompiledInstructions.removeIf(instruction -> {
                if (instruction instanceof JumpDest && ((JumpDest) instruction).getIncomingBranches().size() == 0) {
                    instruction.getPrev().setNext(instruction.getNext());
                    instruction.getNext().setPrev(instruction.getPrev());
                    return true;
                }
                return false;
            });

            // remove dependencies of jump instructions on the variables that hold the jump destination address,
            // as they are no longer used, so we can optimize them away below
            decompiledInstructions.stream()
                    .filter(instruction -> (instruction instanceof Jump || instruction instanceof JumpI))
                    .forEach(instruction -> instruction.getInput()[0] = null);

            // create dependency graph on instructions to detect unused/dead instructions
            {
                boolean removedSomething;
                do {
                    removedSomething = decompiledInstructions.removeIf(instruction -> {
                                // determine dependencies for this instruction
                                return Arrays.stream(instruction.getInput()).filter(Objects::nonNull)
                                        // determine dependencies for this variable
                                        .anyMatch(inputVar -> {
                                            Queue<Instruction> branchesToProcess = new LinkedList<>();
                                            Set<Instruction> processedInstructions = new HashSet<>();
                                            Instruction prevInstr = instruction.getPrev();
                                            while (prevInstr != null || (prevInstr = branchesToProcess.poll()) != null) {
                                                if (processedInstructions.contains(prevInstr)) {
                                                    prevInstr = null;
                                                    continue;
                                                }
                                                processedInstructions.add(prevInstr);

                                                boolean ioMatch =
                                                        Arrays.stream(prevInstr.getOutput()).anyMatch(outputVar -> outputVar == inputVar);
                                                if (ioMatch) {
                                                    return false;
                                                }

                                                if (prevInstr instanceof JumpDest && !(prevInstr instanceof _VirtualInstruction)) {
                                                    branchesToProcess.addAll(((JumpDest) prevInstr).getIncomingBranches());
                                                }

                                                prevInstr = prevInstr.getPrev();
                                            }
                                            // !foundDependency
                                            if (destacker.isVirtualCanonicalVar(inputVar)) {
                                                // this variable was a result of a stack merger and was never defined, i.e. it's unused anyway.
                                                // can happen if one virtual variable got reassigned to another virtual variable
                                                // and the right one was never initialized
                                                // so remove this instruction that depend on this variable
                                                instruction.getNext().setPrev(instruction.getPrev());
                                                instruction.getPrev().setNext(instruction.getNext());

                                                // sanity check: should only be virtual assignment instructions
                                                if (!(instruction instanceof _VirtualAssignment)) {
                                                    throw new IllegalStateException(
                                                            "uninitialized variable used by non-assignment instruction");
                                                }

                                                return true; // remove from instruction list
                                            }
                                            else {
                                                throw new IllegalStateException("Dependency resolver reached method head. " +
                                                        "Should have resolved all dependencies by now (but didn't for '" + inputVar +
                                                        "'). " +
                                                        "Possible use of undeclared variable in this scope (would be decompiler bug).");
                                            }
                                        });
                            }
                    );
                } while (removedSomething);
            }

            // create dependency graph on instructions to detect unused/dead instructions
            decompiledInstructions.forEach(instruction -> {
                        // determine dependencies for this instruction
                        Arrays.stream(instruction.getInput()).filter(Objects::nonNull)
                                // determine dependencies for this variable
                                .forEach(inputVar -> {
                                    Queue<Instruction> branchesToProcess = new LinkedList<>();
                                    Set<Instruction> processedInstructions = new HashSet<>();
                                    boolean foundDependency = false;

                                    Instruction prevInstr = instruction.getPrev();
                                    while (prevInstr != null || (prevInstr = branchesToProcess.poll()) != null) {
                                        if (processedInstructions.contains(prevInstr)) {
                                            prevInstr = null;
                                            continue;
                                        }
                                        processedInstructions.add(prevInstr);

                                        boolean ioMatch = Arrays.stream(prevInstr.getOutput()).anyMatch(outputVar -> outputVar == inputVar);
                                        if (ioMatch) {
                                            instruction.addDependency(prevInstr);
                                            foundDependency = true;
                                            prevInstr = null;
                                            continue;
                                        }

                                        if (prevInstr instanceof JumpDest && !(prevInstr instanceof _VirtualInstruction)) {
                                            branchesToProcess.addAll(((JumpDest) prevInstr).getIncomingBranches());
                                        }

                                        prevInstr = prevInstr.getPrev();
                                    }
                                    if (!foundDependency) {
                                        if (destacker.isVirtualCanonicalVar(inputVar)) {
                                            throw new IllegalStateException("Uninitialized variable " + inputVar + " should not exist anymore");
                                        }
                                        else {
                                            throw new IllegalStateException("Dependency resolver reached method head. " +
                                                    "Should have resolved all dependencies by now (but didn't for '" + inputVar + "'). " +
                                                    "Possible use of undeclared variable in this scope (would be decompiler bug).");
                                        }
                                    }
                                });
                    }
            );

            removeUnusedInstructions(decompiledInstructions);

            if (destacker.sawMergeWithDiffStackSize) log.println("size-mismatch merger");
            if (destacker.sawPlaceholderVarsAtStackBottom) log.println("placeholder vars at stack bottom");

            return decompiledInstructions;
        }
        // EOM
    }
}
