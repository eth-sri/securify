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

import static ch.securify.utils.ArrayUtil.nextNonNullItem;
import static ch.securify.utils.ArrayUtil.prevNonNullItem;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ch.securify.utils.DevNullPrintStream;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import ch.securify.decompiler.evm.OpCodes;
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.instructions.BranchInstruction;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.Jump;
import ch.securify.decompiler.instructions.JumpDest;
import ch.securify.decompiler.instructions.JumpI;
import ch.securify.decompiler.instructions._NoOp;
import ch.securify.decompiler.instructions._VirtualInstruction;
import ch.securify.decompiler.instructions._VirtualMethodHead;
import ch.securify.decompiler.instructions._VirtualMethodInvoke;
import ch.securify.decompiler.instructions._VirtualMethodReturn;
import ch.securify.decompiler.printer.HexPrinter;
import ch.securify.utils.ArrayUtil;

public class Decompiler extends AbstractDecompiler {

    private static PrintStream log;
    private static final boolean DEBUG = false;

	public static List<Instruction> decompile(final byte[] bytecode, final PrintStream _log) {
        if (DEBUG)
            log = _log;
        else
            log = new DevNullPrintStream();

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


		// search jumpdests corresponding to methods' start
		log.println();
		log.println("Begin-of-methods:");
		/* List of bytecode offsets that correspond to begin-of-methods. */
		Map<Integer, MethodDetector.MethodInfo> methods = new HashMap<>();
		{
			Multimap<Integer, Integer> reversedBranches = controlFlowDetector.getBranchesReversed();
			//noinspection ConstantConditions // suppress NPE warnings
			Collection<MethodDetector.MethodInfo> methodInfos = IntStream.range(0, rawInstructions.length)
					.filter(bco -> rawInstructions[bco] != null)
					// find all JUMPDEST instructions (i.e. method heads) ..
					.filter(methodHead -> rawInstructions[methodHead].opcode == OpCodes.JUMPDEST)
					// .. that are not dead code (i.e. are reachable) ..
					.filter(reversedBranches::containsKey)
					// .. and are only reached from JUMP instructions (i.e. method calls) ..
					.filter(methodHead -> reversedBranches.get(methodHead).stream()
							.allMatch(methodCall -> (rawInstructions[methodCall].opcode == OpCodes.JUMP)
									// .. which are preceded by a PUSH instruction (i.e. are not return jumps) ..
									&& OpCodes.isPush(prevNonNullItem(methodCall, rawInstructions).opcode) != -1
									// .. and are followed by a JUMPDEST (i.e. return destination) ..
									// TODO: this doesn't hold apparently (see MetaGold example)
									&& nextNonNullItem(methodCall, rawInstructions).opcode == OpCodes.JUMPDEST
									// .. that is not dead code ..
									&& reversedBranches.containsKey(ArrayUtil.nextNonNullIndex(methodCall, rawInstructions))
									&& reversedBranches.get(ArrayUtil.nextNonNullIndex(methodCall, rawInstructions)).stream()
											// .. and is only reachable by JUMP instructions (i.e. by method return) ..
											.allMatch(methodReturn -> rawInstructions[methodReturn].opcode == OpCodes.JUMP
													// .. which come after the method head [sanity check] ..
													// TODO: check this with the CFG instead of bytecode positions ..
													// TODO: .. might fix the callstack underflow problem,
													// TODO: where we have a false-positive method return jump outside of a method
													&& methodHead < methodReturn
													// .. and do not jump directly to the next (return dest) instruction [sanity check] ..
													&& methodHead != ArrayUtil.nextNonNullIndex(methodCall, rawInstructions)
													// .. and are not preceded by a PUSH instruction (i.e. are return jumps).
													&& OpCodes.isPush(prevNonNullItem(methodReturn, rawInstructions).opcode) == -1)))
					// then collect them in a result set.
					.mapToObj(MethodDetector.MethodInfo::new)
					.sorted(Comparator.comparingInt(MethodDetector.MethodInfo::getHead))
					.collect(Collectors.toList());

			// get the method return statements of each method:
			// for each method ..
			methodInfos.forEach(method ->
					// .. find the corresponding method calls ..
					reversedBranches.get(method.head)
							.forEach(methodCall -> {
								method.calls.add(methodCall);
								// .. to find the method return destinations ..
								int returnDest = ArrayUtil.nextNonNullIndex(methodCall, rawInstructions);
								method.returnDests.add(returnDest);
								// find the method return instructions to save them
								method.returns.addAll(reversedBranches.get(returnDest));
							}));

			methodInfos.forEach(methodInfo -> methods.put(methodInfo.getHead(), methodInfo));

			log.println("(" + methodInfos.size() + " methods total)");

			methodInfos.forEach(methodInfo -> {
				log.println("detected method @" + HexPrinter.toHex(methodInfo.head) + " (" + tags.get(methodInfo.head) + ")");
				log.println("    returning from: " + HexPrinter.toHex(methodInfo.returns, ", "));
				log.println("    called from:  " + HexPrinter.toHex(methodInfo.calls, ", "));
				log.println("    returning to: " + HexPrinter.toHex(methodInfo.returnDests, ", "));
			});
		}


		// get ABI method IDs
		log.println();
		log.println("ABI Method IDs:");
		/* Map bytecode offsets of tags of branch starts to the corresponding method IDs and vice versa. */
		BiMap<Integer, byte[]> branchBcoToAbiMethodId = HashBiMap.create();
		{
			// use decompiler to parse the instructions of the first code block
			// (i.e. until the first hard jump or conditional jump to error)
			Instruction[] instructionsOfFirstBlock = new Instruction[bytecode.length];
			InstructionFactory instructionFactory = new InstructionFactory()
					.setJumpResolver(instruction -> tags.get(Iterables.getFirst(mapJumpsToDests.get(instruction.offset), -42)))
					.setLabelResolver(instruction -> tags.get(instruction.offset));

			// partial execution of first code block to get the stack variables
			int endOfFirstBlock = 0;
			Stack<Variable> stack = new Stack<>();
			for (int offset = 0; offset < rawInstructions.length; offset = ArrayUtil.nextNonNullIndex(offset, rawInstructions)) {
				RawInstruction rawInstruction = rawInstructions[offset];

				if (OpCodes.isInvalid(rawInstruction.opcode) || rawInstruction.opcode == OpCodes.JUMP
						|| rawInstruction.opcode == OpCodes.JUMPI
						&& controlFlowGraph.get(rawInstruction.offset).contains(ControlFlowDetector.DEST_ERROR)) {
					// end of first block, stop
					endOfFirstBlock = rawInstruction.offset;
					break;
				}

				instructionsOfFirstBlock[offset] = instructionFactory.createAndApply(rawInstruction, stack);
			}

			// try to detect ABI method IDs by setting up instruction/variable dependencies,
			// so we look out for conditional jumps that have a dependency on some
			// previously push'd 4-byte value (which is most likely a ABI method ID)
			for (int offset = 0; offset < instructionsOfFirstBlock.length;
				 offset = ArrayUtil.nextNonNullIndex(offset, instructionsOfFirstBlock)) {
				Instruction instruction = instructionsOfFirstBlock[offset];
				if (offset >= endOfFirstBlock) {
					// end of first block reached
					break;
				}

				// determine dependencies for this instruction
				for (Variable inputVar : instruction.getInput()) {
					backtrack:
					for (int offset2 = ArrayUtil.prevNonNullIndex(offset, instructionsOfFirstBlock);
						 offset2 >= 0; offset2 = ArrayUtil.prevNonNullIndex(offset2, instructionsOfFirstBlock)) {
						Instruction prevInstr = instructionsOfFirstBlock[offset2];
						for (Variable prevOutputVar : prevInstr.getOutput()) {
							if (prevOutputVar == inputVar) {
								instruction.addDependency(prevInstr);
								break backtrack;
							}
						}
					}
				}

				// we have a conditional jump here, so search for a 4-byte push dependency to the ABI method ID
				if (rawInstructions[offset].opcode == OpCodes.JUMPI) {
					// first call to method setup
					byte[] methodId = findMethodId(instruction, 0);
					if (methodId != null) {
						Collection<Integer> branch = mapJumpsToDests.get(rawInstructions[offset].offset);
						assert branch.size() == 1;
						branchBcoToAbiMethodId.put(branch.iterator().next(), methodId);
					}
				}
			}

			branchBcoToAbiMethodId.forEach((bco, methodId) ->
					log.println(tags.get(bco) + " belongs to branch of ABI method ID " + HexPrinter.toHex(methodId)));
		}


		// verify methods and get their arguments and return var counts
		MethodDetector methodDetector = new MethodDetector();
		methodDetector.detect(rawInstructions, mapJumpsToDests, controlFlowDetector, methods, log);

		Collection<Integer> methodHeads = methods.keySet();

		// rename tag labels of known ABI methods to include their ID
		{
			Set<Integer> renamedMethodLabels = new HashSet<>();
			// search for methods corresponding to the ABI methods
			branchBcoToAbiMethodId.forEach((bco, methodId) -> {
				Integer beginOfMethodBco = findBeginOfMethodForBranch(bco, controlFlowGraph, methodHeads);
				if (beginOfMethodBco == null) {
					throw new IllegalStateException("Method head for " + HexPrinter.toHex(methodId) +
							" could not be resolved, starting @" + HexPrinter.toHex(bco) + " " + tags.get(bco));
				}

				String methodLabel = _VirtualMethodHead.METHOD_NAME_PREFIX_ABI + HexPrinter.toHex(methodId);
				log.println(tags.get(beginOfMethodBco) + " renamed to " + methodLabel);
				// override tag name
				tags.put(beginOfMethodBco, methodLabel);

				renamedMethodLabels.add(beginOfMethodBco);
			});

			// rename other unknown methods
			methodHeads.forEach(methodHead -> {
				if (!renamedMethodLabels.contains(methodHead)) {
					String methodLabel = _VirtualMethodHead.METHOD_NAME_PREFIX_UNKNOWN + HexPrinter.toHex(methodHead);
					log.println(tags.get(methodHead) + " renamed to " + methodLabel);
					// override tag name
					tags.put(methodHead, methodLabel);
					renamedMethodLabels.add(methodHead);
				}
			});
		}


		// print method signatures
		log.println("Method signatures:");
		methodHeads.forEach(methodHead -> log.println(tags.get(methodHead)
				+ " (" + String.join(",", Collections.nCopies(methodDetector.getArgumentCountForMethod(methodHead), "a")) + ")"
				+ " -> (" + String.join(",", Collections.nCopies(methodDetector.getReturnVarCountForMethod(methodHead), "r")) + ")"));


		// Decompile the whole thing
		log.println();
		log.println("Decompiling...");
		{
			Destacker destacker = new Destacker();

			InstructionFactory instructionFactory = new InstructionFactory()
					.setJumpResolver(instruction -> tags.get(Iterables.getFirst(mapJumpsToDests.get(instruction.offset), -42)))
					.setLabelResolver(instruction -> tags.get(instruction.offset));

			destacker.decompile(rawInstructions, instructionFactory, mapJumpsToDests, controlFlowGraph, methodDetector, log);

			List<Instruction> decompiledInstructions = new LinkedList<>();

			{
				Instruction[] decompiledInstrs = destacker.getInstructions();
				// group instructions by method
				Collection<Integer> methodHeadsE = new TreeSet<>(methodHeads);
				methodHeadsE.add(0);
				methodHeadsE.forEach(methodHead -> {
					Instruction methodEntry = decompiledInstrs[methodHead];

					Queue<Instruction> branchesToProcess = new LinkedList<>();
					branchesToProcess.add(methodEntry);
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
							if (instruction instanceof BranchInstruction &&
									!(instruction instanceof _VirtualMethodReturn) &&
									!(instruction instanceof _VirtualMethodInvoke)) {
								branchesToProcess.addAll(((BranchInstruction) instruction).getOutgoingBranches());
							}
						} while ((instruction = instruction.getNext()) != null);
					}
				});
			}

			//System.out.println("RAW DEC");
			//DecompilationPrinter.printInstructions(decompiledInstructions, System.out);

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
				if (instruction instanceof JumpDest && ((JumpDest) instruction).getIncomingBranches()
						.stream().noneMatch(src -> !(src instanceof _VirtualMethodReturn))) {
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
					.filter(instruction -> !(instruction instanceof _VirtualInstruction)) // exclude method instructions
					.forEach(instruction -> instruction.getInput()[0] = null);


			// create dependency graph on instructions to detect unused/dead instructions
			DependencyResolver.resolveDependencies(decompiledInstructions);

			removeUnusedInstructions(decompiledInstructions);

			if (destacker.sawMergeWithDiffStackSize) log.println("size-mismatch merger");

			return decompiledInstructions;
		}
		// EOM
	}


	/**
	 * Find the first best begin-of-method starting on the given branch `abiMethodBranch`.
	 * @param abiMethodBranch branch (jumpdest bytecode offset) that leads to the wanted begin-of-method.
	 * @param cfg control flow graph.
	 * @param methodHeads known begin-of-methods.
	 * @return bytecode offset of the begin-of-method, null if no begin-of-method found.
	 */
	private static Integer findBeginOfMethodForBranch(Integer abiMethodBranch,
			Multimap<Integer, Integer> cfg, Collection<Integer> methodHeads) {
		Queue<Integer> bfsQueue = new LinkedList<>();
		bfsQueue.addAll(cfg.get(abiMethodBranch));

		int variant = 256;
		while (!bfsQueue.isEmpty() && variant --> 0) {
			Integer node = bfsQueue.poll();
			if (methodHeads.contains(node)) {
				return node;
			}
			else {
				bfsQueue.addAll(cfg.get(node));
			}
		}
		return null;
	}


}
