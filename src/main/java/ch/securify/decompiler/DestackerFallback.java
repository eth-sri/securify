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

import ch.securify.decompiler.evm.OpCodes;
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.printer.HexPrinter;
import ch.securify.utils.DevNullPrintStream;
import ch.securify.utils.Pair;
import ch.securify.utils.Resolver;
import ch.securify.decompiler.instructions.BranchInstruction;
import ch.securify.decompiler.instructions.Eq;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.Invalid;
import ch.securify.decompiler.instructions.Jump;
import ch.securify.decompiler.instructions.JumpDest;
import ch.securify.decompiler.instructions.JumpI;
import ch.securify.decompiler.instructions.Push;
import ch.securify.decompiler.instructions.Return;
import ch.securify.decompiler.instructions.Stop;
import ch.securify.decompiler.instructions.SelfDestruct;
import ch.securify.decompiler.instructions._VirtualAssignment;
import ch.securify.utils.StackUtil;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Ints;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static ch.securify.decompiler.instructions.Instruction.NO_VARIABLES;
import static ch.securify.utils.ArrayUtil.nextNonNullIndex;
import static ch.securify.utils.ArrayUtil.nextNonNullItem;
import static ch.securify.utils.ArrayUtil.prevNonNullIndex;
import static ch.securify.utils.ArrayUtil.prevNonNullItem;

public class DestackerFallback {

	private PrintStream log;

	private RawInstruction[] rawInstructions;
	private Multimap<Integer, Integer> jumps;
	private Multimap<Integer, Integer> jumpsInv;
	private Multimap<Integer, Integer> controlFlowGraph;
	private InstructionFactory instructionFactory;
	private Collection<Integer> methodHeads;
	private MethodDetector methodDetector;

	private Instruction[] instructions;
	private Map<Integer, Variable[]> argumentsForMethod, returnVarsForMethod;
	private Map<Integer, Variable[]> argumentsForMethodCall, returnVarsForMethodCall;

	private Map<Integer, Stack<Variable>> canonicalStackForBranchJoinJumpdest;
	private Map<Pair<Instruction, Boolean>, Map<Variable, Variable>> variableReassignments;
	private Map<Instruction, Map<Variable, Variable>> variableReassignmentsInline;
	private Map<Integer, List<Instruction>> dynamicJumpReplacement;
	private Map<Instruction, Integer> dynamicJumpReplacementTargets;

	private Set<Variable> virtualCanonicalVars;

	public boolean sawMergeWithDiffStackSize = false;
	public boolean sawPlaceholderVarsAtStackBottom = false;

	private final boolean DEBUG = false;

	private BiMap<Integer, String> tags;


	/**
	 * Decompile the bytecode.
	 * @param rawInstructions EVM instructions.
	 * @param instructionFactory InstructionFactory to create instances for decompiled instructions.
	 * @param jumps maps jump instructions to their jump destinations.
	 */
	public void decompile(RawInstruction[] rawInstructions, InstructionFactory instructionFactory, Multimap<Integer, Integer> jumps,
			Multimap<Integer, Integer> controlFlowGraph, final PrintStream log, BiMap<Integer, String> tags) {


	    if (DEBUG)
		    this.log = log;
	    else
	        this.log = new DevNullPrintStream();

		this.rawInstructions = rawInstructions;
		this.jumps = jumps;
		this.controlFlowGraph = controlFlowGraph;
		this.instructionFactory = instructionFactory;
		this.tags = tags;

		this.instructions = new Instruction[rawInstructions.length];
		this.argumentsForMethod = new HashMap<>();
		this.returnVarsForMethod = new HashMap<>();
		this.argumentsForMethodCall = new HashMap<>();
		this.returnVarsForMethodCall = new HashMap<>();

		this.jumpsInv = HashMultimap.create();
		jumps.asMap().forEach((src, dsts) -> dsts.forEach(dst -> jumpsInv.put(dst, src)));

		canonicalStackForBranchJoinJumpdest = new HashMap<>();
		variableReassignments = new HashMap<>();
		variableReassignmentsInline = new HashMap<>();
		dynamicJumpReplacement = new HashMap<>();
		dynamicJumpReplacementTargets = new HashMap<>();

		virtualCanonicalVars = new HashSet<>();

		Variable.resetVarNameGenerator();

		decompile(0, new Stack<>());
	}


	/**
	 * Partially executes the EVM code.
	 * @param branchStartOffset bytecode offset where the branch starts.
	 * @param evmStack Variable stack at the beginning of the current branch.
	 */
	private void decompile(int branchStartOffset, Stack<Variable> evmStack) {
		for (int pc = branchStartOffset; ; pc = nextNonNullIndex(pc, rawInstructions)) {
			RawInstruction rawInstruction = rawInstructions[pc];
			final int opcode = rawInstruction.opcode;

			if (instructions[pc] != null) {
				int prevInstrOffset = prevNonNullIndex(pc, rawInstructions);
				int prevInstrOpcode = rawInstructions[prevInstrOffset].opcode;
				if (pc != branchStartOffset && opcode == OpCodes.JUMPDEST && prevInstrOpcode != OpCodes.JUMP) {
					//log.println("linearly reached code that was already processed @" + HexPrinter.toHex(pc));
					// need to do stack merging
					handleStackMerging(evmStack, prevInstrOffset, instructions[prevInstrOffset], pc);
					return;
				}

				throw new IllegalStateException("Instruction @" + HexPrinter.toHex(pc) + " was already processed");
			}

			//log.print("[DS] decompiling @" + toHex(pc) + " " + OpCodes.getOpName(rawInstructions[pc].opcode));

			instructions[pc] = instructionFactory.createAndApply(rawInstruction, evmStack);

			//log.println(" > " + evmStack.size());

			if (opcode == OpCodes.JUMP) {
				if (jumps.get(pc).size() != 1) {
					// zero, or 2+ targets
					Variable jumpDestVariable = instructions[pc].getInput()[0];
					Resolver<RawInstruction, String> labelResolver = instructionFactory.getLabelResolver();
					if (!dynamicJumpReplacement.containsKey(pc)) {
						dynamicJumpReplacement.put(pc, new ArrayList<>());
					}
					else {
						throw new IllegalStateException("dynamicJumpReplacement should not exist yet");
					}
					List<Instruction> replacementInstrs = dynamicJumpReplacement.get(pc);
					for (int jumpdest : jumps.get(pc)) {
						// create variable containing jump destination offset
						Push push = new Push(Ints.toByteArray(jumpdest));
						push.setInput(NO_VARIABLES);
						Variable pushVar = new Variable();
						push.setOutput(pushVar);
						// create jump condition
						Eq eq = new Eq();
						eq.setInput(jumpDestVariable, pushVar);
						Variable cond = new Variable();
						eq.setOutput(cond);
						// create jump instruction
						JumpI jumpI = new JumpI(labelResolver.resolve(new RawInstruction(0, null, jumpdest, -1)));
						jumpI.setInput(null, cond);
						jumpI.setOutput(NO_VARIABLES);
						dynamicJumpReplacementTargets.put(jumpI, jumpdest);
						// save new instrucitons
						replacementInstrs.add(push);
						replacementInstrs.add(eq);
						replacementInstrs.add(jumpI);
						// check each jump for unique canonical stack / stack merging
						ensureUniqueCanonicalStack(jumpI, jumpdest, StackUtil.copyStack(evmStack));
						if (instructions[jumpdest] == null) {
							Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
							ensureUniqueCanonicalStack(jumpI, jumpdest, branchedStack);
							decompile(jumpdest, branchedStack);
						}
						else {
							// destination already destacked, may need to map current stack
							handleStackMerging(evmStack, pc, jumpI, jumpdest);
						}
					}
					replacementInstrs.add(new Invalid().setInput(NO_VARIABLES).setOutput(NO_VARIABLES));
				}
				else {
					int jumpdest = jumps.get(pc).iterator().next();
					if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
						if (instructions[jumpdest] == null) {
							Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
							ensureUniqueCanonicalStack(instructions[pc], jumpdest, branchedStack);
							decompile(jumpdest, branchedStack);
						}
						else {
							// destination already destacked, may need to map current stack
							handleStackMerging(evmStack, pc, instructions[pc], jumpdest);
						}
					}
				}
				// end of current branch
				return;
			}
			else if (opcode == OpCodes.JUMPI) {
				if (jumps.get(pc).size() != 1) {
					// TODO: create virtual hard jumps for this dynamic jump
					// are there any dynamic conditional jumps? never seen such a thing...
					throw new AssumptionViolatedException("conditional jump @" + HexPrinter.toHex(pc) + " has multiple jump targets");
				}
				int jumpdest = jumps.get(pc).iterator().next();
				if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
					if (instructions[jumpdest] == null) {
						Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
						ensureUniqueCanonicalStack(instructions[pc], jumpdest, branchedStack);
						decompile(jumpdest, branchedStack);
					}
					else {
						// destination already destacked, may need to map current stack
						handleStackMerging(evmStack, pc, instructions[pc], jumpdest);
					}
				}
				// continue on current branch
				continue;
			}
			else if (OpCodes.endsExecution(opcode)) {
				return;
			}
			else if (opcode == OpCodes.JUMPDEST && !canonicalStackForBranchJoinJumpdest.containsKey(pc)) {
				// set canonical stack for jumpdest
				Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
				ensureUniqueCanonicalStack(instructions[pc], branchedStack);
				canonicalStackForBranchJoinJumpdest.put(pc, branchedStack);
			}
		}
	}


	private void ensureUniqueCanonicalStack(Instruction jumpsrc, int jumpdest, Stack<Variable> stack) {
		ensureUniqueCanonicalStack(jumpsrc, jumpdest, null, stack);
	}


	private void ensureUniqueCanonicalStack(Instruction inlineDest, Stack<Variable> stack) {
		ensureUniqueCanonicalStack(null, inlineDest.getRawInstruction().offset, inlineDest, stack);
	}


	/**
	 * Ensure that there exists a canonical stack with unique variables at the given instruction.
	 * @param jumpsrc jump source where some variable reassignments may be made, null if we're in local execution (i.e. it's not a jump)
	 * @param jumpdest jump destination where the canonical stack should be defined
	 * @param stack canonical stack candidate
	 */
	private void ensureUniqueCanonicalStack(Instruction jumpsrc, int jumpdest, Instruction inlineDest, Stack<Variable> stack) {
		if (jumpsrc == null && inlineDest == null || jumpsrc != null && inlineDest != null)
			throw new IllegalArgumentException();

		// check if the current stack is going to be used as a canonical stack
		if (!canonicalStackForBranchJoinJumpdest.containsKey(jumpdest)) {
			// if so, check for duplicate variables in the stack, which may cause merge conflicts
			if (stack.size() != stack.stream().distinct().count()) {
				log.println("undup canonical stack @" + HexPrinter.toHex(jumpdest));
				// map duplicate variables to new ones
				if (jumpsrc != null && variableReassignments.containsKey(jumpsrc) ||
						jumpsrc == null && variableReassignmentsInline.containsKey(inlineDest)) {
					throw new IllegalStateException("reassignment does already exist");
				}
				Map<Variable, Variable> reassignments = new HashMap<>();
				Set<Variable> processedVars = new HashSet<>();
				for (int i = 0; i < stack.size(); ++i) {
					Variable var = stack.get(i);
					if (processedVars.contains(var)) {
						// duplicate variable, create new one with reassigment
						Variable undupVar = new Variable();
						stack.set(i, undupVar);
						reassignments.put(undupVar, var);
						log.println(" " + undupVar + " <- " + var);
					}
					else {
						processedVars.add(var);
					}
				}
				if (jumpsrc == null) {
					variableReassignmentsInline.put(inlineDest, reassignments);
				}
				else {
					variableReassignments.put(new Pair<>(jumpsrc, true), reassignments);
				}
			}
			// add some placeholder variable at the stack's bottom,
			// in case our canonical stack is too small to map another merged stack
			if (jumpsInv.get(jumpdest).size() > 1) {
				for (int i = 0; i < 20; ++i) {
					Variable virtualVar = new Variable();
					virtualCanonicalVars.add(virtualVar);
					stack.insertElementAt(virtualVar, 0);
				}
				sawPlaceholderVarsAtStackBottom = true;
			}
		}
	}


	private void handleStackMerging(Stack<Variable> localStack, int jumpsrc, Instruction jumpI, int jumpdest) {
		// destination already destacked, may need to map current stack
		if (!canonicalStackForBranchJoinJumpdest.containsKey(jumpdest)) {
			throw new IllegalStateException("target jumpdest processed, but no canonical stack defined");
		}
		Stack<Variable> canonicalStack = canonicalStackForBranchJoinJumpdest.get(jumpdest);
		if (localStack.size() != canonicalStack.size()) {
			log.println("Branch merge: stack size mismatch: canonical @" + HexPrinter.toHex(jumpdest) +
					" with size " + canonicalStack.size() + " vs local @" + HexPrinter.toHex(jumpsrc) + " with size " + localStack.size());
			sawMergeWithDiffStackSize = true;
		}
		Multimap<Variable, Variable> mapToCanonical = HashMultimap.create();

		int mergeSize = Math.min(localStack.size(), canonicalStack.size());
		if (mergeSize == 0) {
			log.println("Branch merge: skipped merger for empty stack");
			return;
		}
		for (int i = 1; i <= mergeSize; ++i) {
			mapToCanonical.put(localStack.get(localStack.size() - i), canonicalStack.get(canonicalStack.size() - i));
		}
		log.println("stack merging from @" + HexPrinter.toHex(jumpsrc) + " into @" + HexPrinter.toHex(jumpdest));
		mapToCanonical.asMap().forEach((variable, canonicals) ->
				canonicals.forEach(canonical -> log.println(" " + canonical + " <- " + variable)));

		if (mapToCanonical.size() != mapToCanonical.values().stream().distinct().count()) {
			throw new IllegalStateException("a canonical variable is assigned multiple times");
		}

		boolean jumpCondition = findJumpCondition(jumpI, jumpdest);
		
		// create re-assignment instructions		
		if (variableReassignments.containsKey(new Pair<>(jumpI, jumpCondition))) {
			throw new IllegalStateException("reassignment does already exist");
		}
		Map<Variable, Variable> reassignments = new LinkedHashMap<>();
		mapToCanonical.asMap().forEach((variable, canonicals) ->
				canonicals.stream().filter(canonical -> variable != canonical)
						.forEach(canonical -> reassignments.put(canonical, variable)));

		// create temporary variables if need have conflicting variable swaps
		Map<Variable, Variable> temporaries = new LinkedHashMap<>();
		Set<Variable> wasAssignedTo = new HashSet<>();
		// search all variables that are reassigned before they get assigned
		reassignments.forEach((canonical, local) -> {
			if (wasAssignedTo.contains(local)) {
				Variable tmpVar = new Variable();
				temporaries.put(local, tmpVar);
				//if (isVirtualCanonicalVar(canonical) && isVirtualCanonicalVar(local)) {
				if (isVirtualCanonicalVar(local)) {
					virtualCanonicalVars.add(tmpVar);
				}
				log.println("swap conflict for: " + canonical + " <- " + local + "; created temp variable: " + tmpVar);
			}
			wasAssignedTo.add(canonical);
		});

		if (temporaries.size() > 0) {
			// replace locals with temporaries, if there is a temporary
			reassignments.replaceAll((canonical, local) -> temporaries.getOrDefault(local, local));
			// add assignemts to temporaries at the beginning
			Map<Variable, Variable> reassignmentsWithTemps = new LinkedHashMap<>();
			temporaries.forEach((local, canonical) -> reassignmentsWithTemps.put(canonical, local));
			reassignmentsWithTemps.putAll(reassignments);
			reassignments.clear();
			reassignments.putAll(reassignmentsWithTemps);
		}

		variableReassignments.put(new Pair<>(jumpI, jumpCondition), reassignments);
	}


    // Identifies whether the positive or negative branch was taken in case of JUMPI
    // Always true otherwise
	private boolean findJumpCondition(Instruction jumpI, int jumpdest) {
		if(jumpI instanceof JumpI) {
			String targetLabel = ((JumpI) jumpI).targetLabel;
			String tag = tags.get(jumpdest);
			return tag.equals(targetLabel);
		}
		// includes Jump case
		return true;
	}


	/**
	 * Get the decompiled instructions.
	 * @return decompiled instructions.
	 */
	public Instruction[] getInstructions() {
		// create instruction flow graph, so we don't have to mess with bytecode offsets,
		// and can insert/remove instructions without breaking references
		// i.e. each instruction has prev & next instrucions
		IntStream.range(0, instructions.length)
				.filter(offset -> instructions[offset] != null)
				.forEach(offset -> {
					Instruction instr = instructions[offset];
					if (instr instanceof Jump) {
						if (dynamicJumpReplacement.containsKey(offset)) {
							List<Instruction> replcmntInstrs = dynamicJumpReplacement.get(offset);
							Instruction prev = prevNonNullItem(offset, instructions);
							prev.setNext(replcmntInstrs.get(0));
							replcmntInstrs.get(0).setPrev(prev);
							for (int i = 1; i < replcmntInstrs.size(); ++i) {
								replcmntInstrs.get(i).setPrev(replcmntInstrs.get(i - 1));
								replcmntInstrs.get(i - 1).setNext(replcmntInstrs.get(i));
								if (replcmntInstrs.get(i) instanceof BranchInstruction) {
									BranchInstruction jumpInstruction = (BranchInstruction) replcmntInstrs.get(i);
									JumpDest jumpDest = (JumpDest) instructions[dynamicJumpReplacementTargets.get(jumpInstruction)];
									jumpInstruction.addOutgoingBranch(jumpDest);
									jumpDest.addIncomingBranch(jumpInstruction);
								}
							}
							Instruction last = replcmntInstrs.get(replcmntInstrs.size() - 1);
							instructions[offset] = last;
							if (!(last instanceof Invalid)) {
								throw new IllegalStateException("assumed branch is terminated by a throw()");
							}
						}
						else {
							Jump jumpInstruction = (Jump) instr;
							// this assumes that we have no virtual jumpdests that have not been handled sparately
							if (jumps.get(offset).stream().anyMatch(target -> target >= 0)) {
								jumps.get(offset).stream().filter(target -> target >= 0)
										.forEach(targetBco -> {
											JumpDest jumpDest = (JumpDest) instructions[targetBco];
											jumpInstruction.addOutgoingBranch(jumpDest);
											jumpDest.addIncomingBranch(jumpInstruction);
										});
							}
							else {
								log.println("replacing error jump with throw @" + HexPrinter.toHex(offset));
								// error jump, replace with throw()
								instr = new Invalid().setInput(NO_VARIABLES).setOutput(NO_VARIABLES)
										.setRawInstruction(instr.getRawInstruction());
								instructions[offset] = instr;
								prevNonNullItem(offset, instructions).setNext(instr);
							}
							instr.setPrev(prevNonNullItem(offset, instructions));
						}
					}
					else if (instr instanceof JumpI) {
						instr.setPrev(prevNonNullItem(offset, instructions));
						instr.setNext(nextNonNullItem(offset, instructions));
						JumpI jumpInstruction = (JumpI) instr;
						// this assumes that we have no virtual jumpdests that have not been handled sparately
						jumps.get(offset).stream().filter(target -> target >= 0)
								.forEach(targetBco -> {
									JumpDest jumpDest = (JumpDest) instructions[targetBco];
									jumpInstruction.addOutgoingBranch(jumpDest);
									jumpDest.addIncomingBranch(jumpInstruction);
								});
						// TODO: if conditional error jump, replace with throw() logic
					}
					else if (instr instanceof JumpDest) {
						// add previous instruction if it's not a hard jump
						Instruction prev = prevNonNullItem(offset, instructions);
						if (prev instanceof Jump || prev instanceof Return
								|| prev instanceof Stop || prev instanceof SelfDestruct || prev instanceof Invalid) {
							// no prev
						}
						else {
							instr.setPrev(prev);
						}
						instr.setNext(nextNonNullItem(offset, instructions));
						// note: incoming branches are set by the originating jump instructions,
						// sice we cannot resolve those here in the current state
					}
					else if (instr instanceof Return || instr instanceof Stop
							|| instr instanceof SelfDestruct || instr instanceof Invalid) {
						instr.setPrev(prevNonNullItem(offset, instructions));
						instr.setNext(null);
					}
					else {
						// linear instruction
						instr.setPrev(prevNonNullItem(offset, instructions));
						instr.setNext(nextNonNullItem(offset, instructions));
					}
				});

		AtomicInteger intermediateLabels = new AtomicInteger(1);

		variableReassignments.forEach((pair, variableMap) -> {
			if (variableMap.entrySet().stream().noneMatch(map -> map.getKey() != map.getValue())) {
				// nothing to reassign
				return;
			}
			Instruction instruction = pair.getFirst();
			boolean jumpCondition = pair.getSecond();
			if (instruction instanceof Jump) {
				// reassign just before the jump
				// from: PREV -> JUMP -> JUMPDEST
				//   to: PREV -> reassignments -> JUMP -> JUMPDEST
				List<Instruction> injectedInstrs = new ArrayList<>();
				// original preceding instruction
				injectedInstrs.add(instruction.getPrev());
				// create reassignments
				variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
						.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
				// original jump
				injectedInstrs.add(instruction);
				// link instructions
				for (int i = 1; i < injectedInstrs.size(); ++i) {
					injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
					injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
				}
			}
			else if (instruction instanceof JumpI) {
				if (jumpCondition) {
					// reassign after the jump but before reaching the target, so need to create a new intermediate branch
					// from: JUMPI -> JUMPDEST
					//   to: JUMPI -> JUMPDEST(virtual) -> reassignments -> JUMP(virtual) -> JUMPDEST
					List<Instruction> injectedInstrs = new ArrayList<>();

					// original jumpi instruction
					JumpI origJumpi = (JumpI) instruction;
					// original jumpdest
					JumpDest origJumpdest = (JumpDest) origJumpi.getOutgoingBranches().iterator().next();
					// break up connection to insert new intermediate branch
					origJumpi.clearOutgoingBranches();
					origJumpdest.removeIncomingBranch(origJumpi);
					// create intermediate jumpdest
					int intermediateLabel = intermediateLabels.getAndIncrement();
					JumpDest intermJumpDest = new JumpDest("tmp_" + intermediateLabel);
					intermJumpDest.setInput(NO_VARIABLES).setOutput(NO_VARIABLES);
					origJumpi.clearOutgoingBranches();
					origJumpi.addOutgoingBranch(intermJumpDest);
					origJumpi.setTargetLabel(intermJumpDest.getLabel());
					intermJumpDest.addIncomingBranch(origJumpi);
					injectedInstrs.add(intermJumpDest);
					// create reassignments
					variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
							.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
					// link instructions
					for (int i = 1; i < injectedInstrs.size(); ++i) {
						injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
						injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
					}
					Instruction lastReassignment = injectedInstrs.get(injectedInstrs.size() - 1);
					// create intermediate jump
					Jump intermJump = new Jump(origJumpdest.getLabel());
					intermJump.setInput(new Variable[1]).setOutput(NO_VARIABLES);
					intermJump.setPrev(lastReassignment);
					lastReassignment.setNext(intermJump);
					// link to original jumpdest
					intermJump.addOutgoingBranch(origJumpdest);
					origJumpdest.addIncomingBranch(intermJump);
				} else {
					// For non-taken jumps
					// from: JUMPI -> ...
					//   to: JUMPI -> JUMPDEST(virtual) -> reassignments -> JUMP(virtual) -> ...
					List<Instruction> injectedInstrs = new ArrayList<>();

					// original jumpi instruction
					JumpI origJumpi = (JumpI) instruction;
					// following instruction
					JumpDest following = (JumpDest) origJumpi.getNext();

					// create reassignments
					variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
							.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));

					// link instructions
					for (int i = 1; i < injectedInstrs.size(); ++i) {
						injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
						injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
					}


					// Link the first instruction
					injectedInstrs.get(0).setPrev(origJumpi);
					origJumpi.setNext(injectedInstrs.get(0));

					// Link the last instruction
					injectedInstrs.get(injectedInstrs.size() - 1).setNext(following);
					following.setPrev(injectedInstrs.get(injectedInstrs.size() - 1));
				}
			}
			else {
				if (!(instruction.getNext() instanceof JumpDest)) {
					throw new IllegalStateException("expected JUMPDEST instruction after marked linear code");
				}
				// need to reassign in linear code, i.e. just before the jumpdest
				// from: PREV -> JUMPDEST
				//   to: PREV -> reassignments -> JUMPDEST
				List<Instruction> injectedInstrs = new ArrayList<>();
				// original instruction
				injectedInstrs.add(instruction);
				// create reassignments
				variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
						.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
				// following jumpdest
				injectedInstrs.add(instruction.getNext());
				// link instructions
				for (int i = 1; i < injectedInstrs.size(); ++i) {
					injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
					injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
				}
			}
		});

		variableReassignmentsInline.forEach((instruction, variableMap) -> {
			if (!(instruction instanceof JumpDest)) {
				throw new IllegalStateException("inline reassignment expected to be used only at JUMPDEST");
			}
			// need to reassign in linear code, i.e. just before the jumpdest
			// from: PREV -> JUMPDEST
			//   to: PREV -> reassignments -> JUMPDEST
			List<Instruction> injectedInstrs = new ArrayList<>();
			// original instruction
			injectedInstrs.add(instruction.getPrev());
			// create reassignments
			variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
					.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
			// following jumpdest
			injectedInstrs.add(instruction);
			// link instructions
			for (int i = 1; i < injectedInstrs.size(); ++i) {
				injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
				injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
			}
		});

		return instructions;
	}


	public boolean isVirtualCanonicalVar(Variable variable) {
		return virtualCanonicalVars.contains(variable);
	}

}
