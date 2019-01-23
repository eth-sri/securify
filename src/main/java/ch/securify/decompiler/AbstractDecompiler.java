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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import ch.securify.decompiler.evm.OpCodes;
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.Push;
import ch.securify.decompiler.instructions._VirtualAssignment;
import ch.securify.decompiler.printer.HexPrinter;

public class AbstractDecompiler {

	private static boolean removeInstruction(Instruction instruction) {
		if (instruction.getPrev() == null || instruction.getNext() == null)
			return false;
		instruction.getPrev().setNext(instruction.getNext());
		instruction.getNext().setPrev(instruction.getPrev());
		return true;
	}

	static void removeUnusedInstructions(List<Instruction> decompiledInstructions) {
		// search for instructions on which no other instruction has a dependency on, i.e. insrtrs whose results are not used
		boolean removedSomething;
		do {
			Set<Instruction> dependencies = new HashSet<>();
			decompiledInstructions.forEach(instruction -> dependencies.addAll(instruction.getDependencies()));

			removedSomething = decompiledInstructions.removeIf(instruction -> {
				if (dependencies.contains(instruction)) {
					return false; // keep used instructions
				}
				if (instruction instanceof Push) {
					return removeInstruction(instruction); // remove unused pushed variables
				}
				if (instruction instanceof _VirtualAssignment) {
					return removeInstruction(instruction); // remove unused reassignments
				}
				if (instruction.getOutput().length >= 1 && instruction.getRawInstruction() != null) {
					int opcode = instruction.getRawInstruction().opcode;
					if ((OpCodes.ADD <= opcode && opcode <= OpCodes.BYTE)) {
						return removeInstruction(instruction); // remove unused arithmetic instructions
					}
				}
				return false; // keep the rest
			});
		} while (removedSomething);
	}

	protected static BiMap<Integer, String> findTags(final PrintStream log, List<Integer> jumpDestinations) {
		// print tags (jumpdests)
		log.println();
		log.println("Tags:");
		/* Map bytecode offsets to tags/labels and vice versa. */
		BiMap<Integer, String> tags = HashBiMap.create();
		{
			for (int i = 0; i < jumpDestinations.size(); i++) {
				Integer bytecodeOffset = jumpDestinations.get(i);
				String tagLabel = "tag_" + (i + 1);
				tags.put(bytecodeOffset, tagLabel);
				log.println(tagLabel + " @" + HexPrinter.toHex(bytecodeOffset));
			}
			// add virtual tags (error and exit)
			tags.put(ControlFlowDetector.DEST_ERROR, "ERROR");
			tags.put(ControlFlowDetector.DEST_EXIT, "EXIT");
		}
		return tags;
	}

	protected static void parseRawInstructions(final byte[] bytecode, RawInstruction[] rawInstructions, List<Integer> jumpDestinations) {
		rawInstructions[rawInstructions.length - 1] =
				new RawInstruction(OpCodes.getInvalid(), null, rawInstructions.length - 1, -1);

		// parse raw instructions
		EvmParser.parse(bytecode, (offset, instrNumber, opcode, payload) -> {
			rawInstructions[offset] = new RawInstruction(opcode, payload, offset, instrNumber);
			if (opcode == OpCodes.JUMPDEST) {
				jumpDestinations.add(offset);
			}
		});
	}

	protected static Multimap<Integer, Integer> dectectControlFlow(final PrintStream log, RawInstruction[] rawInstructions, List<Integer> jumpDestinations, BiMap<Integer, String> tags, ControlFlowDetector controlFlowDetector,
			Multimap<Integer, Integer> mapJumpsToDests) {
				// scan for branches, generate a control flow graph
				log.println();
				log.println("Control Flow (Branches):");

				controlFlowDetector.computeBranches(rawInstructions, log);
				/* Control flow graph: maps from jumps to possible jump destinations and
				 * from jump destinations to the next jump instruction. */
				Multimap<Integer, Integer> controlFlowGraph = controlFlowDetector.getBranches();

				{
					List<Integer> branchSrcs = new ArrayList<>(controlFlowGraph.asMap().keySet());
					Collections.sort(branchSrcs);

					for (Integer branchSrc : branchSrcs) {
						for (int target : controlFlowGraph.get(branchSrc)) {
							String targetName;
							if (target == ControlFlowDetector.DEST_ERROR)
								targetName = "ERROR";
							else if (target == ControlFlowDetector.DEST_EXIT)
								targetName = "OUT";
							else if (jumpDestinations.indexOf(target) == -1)
								targetName = "local" + " @" + HexPrinter.toHex(target);
							else
								targetName = tags.get(target) + " @" + HexPrinter.toHex(target);
							log.println(HexPrinter.toHex(branchSrc) + " -> " + targetName);
						}
					}

					// map jumps to jump destinations
					// jumps with ambiguous destinations are not supported
					for (Integer branchSrc : branchSrcs) {
						Collection<Integer> jumpTargets = controlFlowGraph.get(branchSrc);
						if (rawInstructions[branchSrc].opcode == OpCodes.JUMP) {
							if (jumpTargets.size() != 1 && !ControlFlowDetector.isJumpMethodReturn(branchSrc, rawInstructions)) {
								// disallow jumps with multiple targets (except method returns)
								throw new IllegalArgumentException("Jumps with ambiguous jump targets are not supported: " +
										"Jumping from " + HexPrinter.toHex(branchSrc) + " to " + HexPrinter.toHex(jumpTargets, ","));
							}
							mapJumpsToDests.putAll(branchSrc, jumpTargets);
						}
						else if (rawInstructions[branchSrc].opcode == OpCodes.JUMPI) {
							if (jumpTargets.size() > 2) {
								throw new IllegalArgumentException("Jumps with ambiguous jump targets are not supported: " +
										"Jumping from " + HexPrinter.toHex(branchSrc) + " to " + HexPrinter.toHex(jumpTargets, ","));
							}
							else if (jumpTargets.size() == 1) {
								log.println("Warning: Conditional jump @" + HexPrinter.toHex(branchSrc) + " with both paths " +
										"leading to same destination @" + HexPrinter.toHex(Iterables.get(jumpTargets, 0)) + ". " +
										"Please check if this is true.");
							}
							// contitional jumps we get both the jump destination and the linear execution branch
							int jumpDest1 = Iterables.get(jumpTargets, 0);
							int jumpDest2 = Iterables.get(jumpTargets, 1, jumpDest1);
							// find the conditional jump destination,
							// which is either before the current instruction or further down the line than the other.
							int conditionalTarget;
							if (jumpDest1 < branchSrc) conditionalTarget = jumpDest1;
							else if (jumpDest2 < branchSrc) conditionalTarget = jumpDest2;
							else if (jumpDest1 < jumpDest2) conditionalTarget = jumpDest2;
							else conditionalTarget = jumpDest1;
							mapJumpsToDests.put(branchSrc, conditionalTarget);
						}
					}

					log.println("Jumps:");
					mapJumpsToDests.asMap().forEach(
							(src, dsts) -> dsts.forEach(
									dst -> log.println(HexPrinter.toHex(src) + " -> " + tags.get(dst))
							)
					);
				}
				return controlFlowGraph;
			}

	/**
	 * Search a method ID that is used with the specified instruction (e.g. JUMPI instruction).
	 * @param instruction Instruction instance of which we scan through the dependencies.
	 * @return 4-byte ABI method ID.
	 */
	protected static byte[] findMethodId(Instruction instruction, int recursionDepth) {
		for (Instruction dependencyInstr : instruction.getDependencies()) {
			// assume all 4-byte pushes are method IDs
			if (dependencyInstr instanceof Push && ((Push)dependencyInstr).getData().length == 4) {
				return ((Push)dependencyInstr).getData();
			}
			else {
				byte[] methodId = findMethodId(dependencyInstr, recursionDepth + 1);
				if (methodId != null) {
					return methodId;
				}
			}
		}
		return null;
	}

}
