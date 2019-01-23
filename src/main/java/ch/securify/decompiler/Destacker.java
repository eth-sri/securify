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
import ch.securify.utils.DevNullPrintStream;
import ch.securify.utils.Resolver;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.Invalid;
import ch.securify.decompiler.instructions.Jump;
import ch.securify.decompiler.instructions.JumpDest;
import ch.securify.decompiler.instructions.JumpI;
import ch.securify.decompiler.instructions.Return;
import ch.securify.decompiler.instructions.Stop;
import ch.securify.decompiler.instructions.SelfDestruct;
import ch.securify.decompiler.instructions._VirtualAssignment;
import ch.securify.decompiler.instructions._VirtualMethodHead;
import ch.securify.decompiler.instructions._VirtualMethodInvoke;
import ch.securify.decompiler.instructions._VirtualMethodReturn;
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.printer.HexPrinter;
import ch.securify.utils.StackUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

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
import static ch.securify.decompiler.printer.HexPrinter.toHex;
import static ch.securify.utils.ArrayUtil.nextNonNullIndex;
import static ch.securify.utils.ArrayUtil.nextNonNullItem;
import static ch.securify.utils.ArrayUtil.prevNonNullIndex;
import static ch.securify.utils.ArrayUtil.prevNonNullItem;

public class Destacker {

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
	private Map<Integer, Map<Variable, Variable>> variableReassignments;
	private Map<Integer, Map<Variable, Variable>> variableReassignmentsInline;

	public boolean sawMergeWithDiffStackSize = false;

	private static boolean DEBUG = false;

	/**
	 * Decompile the bytecode.
	 * @param rawInstructions EVM instructions.
	 * @param instructionFactory InstructionFactory to create instances for decompiled instructions.
	 * @param jumps maps jump instructions to their jump destinations.
	 * @param methodDetector method offsets to recognize methods.
	 */
	public void decompile(RawInstruction[] rawInstructions, InstructionFactory instructionFactory, Multimap<Integer, Integer> jumps,
			Multimap<Integer, Integer> controlFlowGraph, MethodDetector methodDetector, final PrintStream log) {
	    if (DEBUG)
		    this.log = log;
	    else
	        this.log = new DevNullPrintStream();

		this.rawInstructions = rawInstructions;
		this.jumps = jumps;
		this.controlFlowGraph = controlFlowGraph;
		this.instructionFactory = instructionFactory;
		this.methodHeads = methodDetector.getMethods().keySet();
		this.methodDetector = methodDetector;

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

		Variable.resetVarNameGenerator();

		decompile(0, new Stack<>(), 0);
	}


	/**
	 * Partially executes the EVM code.
	 * @param branchStartOffset bytecode offset where the branch starts.
	 * @param evmStack Variable stack at the beginning of the current branch.
	 * @param currentMethod bytecode offset of the currently decompiling method.
	 */
	private void decompile(int branchStartOffset, Stack<Variable> evmStack, int currentMethod) {
		for (int pc = branchStartOffset; ; pc = nextNonNullIndex(pc, rawInstructions)) {
			RawInstruction rawInstruction = rawInstructions[pc];
			final int opcode = rawInstruction.opcode;

			if (instructions[pc] != null) {
				int prevInstrOpcode = prevNonNullItem(pc, rawInstructions).opcode;
				if (pc != branchStartOffset && opcode == OpCodes.JUMPDEST && prevInstrOpcode != OpCodes.JUMP) {
					log.println("linearly reached code that was already processed @" + HexPrinter.toHex(pc));
					// need to do stack merging
					handleStackMerging(evmStack, prevNonNullIndex(pc, rawInstructions), pc);
					return;
				}

				throw new IllegalStateException("Instruction @" + toHex(pc) + " was already processed");
			}

			//log.print("[DS@" + toHex(currentMethod) + "]" + (currentMethod == 0 ? "" : "  ") + " decompiling @" + toHex(pc) + " " + OpCodes.getOpName(rawInstructions[pc].opcode));

			instructions[pc] = instructionFactory.createAndApply(rawInstruction, evmStack);

			//log.println(" > " + evmStack.size());

			if (opcode == OpCodes.JUMP) {
				boolean isMethodReturn = ControlFlowDetector.isJumpMethodReturn(pc, rawInstructions);

				if (!isMethodReturn && jumps.get(pc).size() != 1) {
					// only allow multiple targets for a method return
					throw new AssumptionViolatedException("Non-return jump has multiple possible targets");
				}

				int destMethodBco = jumps.get(pc).iterator().next();
				boolean isMethodCall = methodHeads.contains(destMethodBco);

				if (isMethodCall) {
					int destMethodArgumentCount = methodDetector.getArgumentCountForMethod(destMethodBco);
					int destMethodReturnVarCount = methodDetector.getReturnVarCountForMethod(destMethodBco);

					// jump is method call
					if (!argumentsForMethod.containsKey(destMethodBco)) {
						// ... and target method has not been decompiled already
						// create new stack for the method call, that contains only the return jump address and the arguments
						// (this should detect operand stack underflows more easily)
						Stack<Variable> subStack = new Stack<>();

						Variable returnDestVar = evmStack.get(evmStack.size() - destMethodArgumentCount - 1);
						subStack.push(returnDestVar);
						StackUtil.pushAll(subStack, Variable.createNewVariables(destMethodArgumentCount));

						Variable[] methodArguments = Variable.peekFromStack(subStack, destMethodArgumentCount);
						argumentsForMethod.put(destMethodBco, methodArguments);

						// decompile method
						decompile(destMethodBco, subStack, destMethodBco);
					}

					// verify that next instruction is a jumpdest that can only be reached by method returns
					if (rawInstructions[pc + 1].opcode != OpCodes.JUMPDEST) {
						throw new AssumptionViolatedException(toHex(pc) + " is a method call jump, but " +
								toHex(pc + 1) + " is no jumpdest");
					}
					else {
						boolean allAreReturnJumps = jumpsInv.get(pc + 1).stream()
								.allMatch(jumpSrc -> ControlFlowDetector.isJumpMethodReturn(jumpSrc, rawInstructions));
						if (!allAreReturnJumps) {
							throw new AssumptionViolatedException(toHex(pc) + " is a method call jump, but " +
									toHex(pc + 1) + " is no method return jumpdest");
						}
					}

					// store argument variables
					argumentsForMethodCall.put(pc, Variable.peekFromStack(evmStack, destMethodArgumentCount));

					// apply method's stack effects
					// remove arguments & return jump address
					StackUtil.pop(evmStack, destMethodArgumentCount + 1);

					// push return values
					Variable[] returnVars = Variable.createNewVariables(destMethodReturnVarCount);
					StackUtil.pushAllRev(evmStack, returnVars);

					// store returned variables
					returnVarsForMethodCall.put(pc, returnVars);

					// continue on current branch
					continue;
				}
				else if (isMethodReturn) {
					// jump is a method return
					if (returnVarsForMethod.containsKey(currentMethod)) {
						// TODO: declare return vars for each return statement (cross-check that all returns have the same amount of vars)
						throw new AssumptionViolatedException(
								"Method @" + toHex(currentMethod) + " has multiple returns");
					}
					// store return variables
					returnVarsForMethod.put(currentMethod,
							Variable.peekFromStack(evmStack, methodDetector.getReturnVarCountForMethod(currentMethod)));
					// end of current branch
				}
				else {
					// jump is not a method call/return
					if (jumps.get(pc).size() != 1) {
						// this should have been checked already
						throw new AssumptionViolatedException("Non-return jump has multiple possible targets");
					}
					int jumpdest = jumps.get(pc).iterator().next();
					if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
						if (instructions[jumpdest] == null) {
							Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
							ensureUniqueCanonicalStack(pc, jumpdest, branchedStack);
							decompile(jumpdest, branchedStack, currentMethod);
						}
						else {
							// destination already destacked, may need to map current stack
							handleStackMerging(evmStack, pc, jumpdest);
						}
					}
					// end of current branch
				}
				return;
			}
			else if (opcode == OpCodes.JUMPI) {
				if (jumps.get(pc).size() != 1) {
					throw new AssumptionViolatedException("conditional jump @" + toHex(pc) + " has multiple jump targets");
				}
				int jumpdest = jumps.get(pc).iterator().next();
				if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
					if (instructions[jumpdest] == null) {
						Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
						ensureUniqueCanonicalStack(pc, jumpdest, branchedStack);
						decompile(jumpdest, branchedStack, currentMethod);
					}
					else {
						// destination already destacked, may need to map current stack
						handleStackMerging(evmStack, pc, jumpdest);
					}
				}
				// continue on current branch
				continue;
			}
			else if (OpCodes.endsExecution(opcode)) {
				return;
			}
			else if (opcode == OpCodes.JUMPDEST && pc != currentMethod && !canonicalStackForBranchJoinJumpdest.containsKey(pc)) {
				// set canonical stack for jumpdest
				Stack<Variable> branchedStack = StackUtil.copyStack(evmStack);
				ensureUniqueCanonicalStack(-1, pc, branchedStack);
				canonicalStackForBranchJoinJumpdest.put(pc, branchedStack);
			}
		}
	}


	private void ensureUniqueCanonicalStack(int jumpsrc, int jumpdest, Stack<Variable> stack) {
		// check if the current stack is going to be used as a canonical stack
		if (!canonicalStackForBranchJoinJumpdest.containsKey(jumpdest)) {
			// if so, check for duplicate variables in the stack, which may cause merge conflicts
			if (stack.size() != stack.stream().distinct().count()) {
				log.println("undup canonical stack @" + toHex(jumpdest));
				// map duplicate variables to new ones
				if (jumpsrc != -1 && variableReassignments.containsKey(jumpsrc) || jumpsrc == -1 && variableReassignmentsInline.containsKey(jumpsrc)) {
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
				if (jumpsrc == -1) {
					variableReassignmentsInline.put(jumpdest, reassignments);
				}
				else {
					variableReassignments.put(jumpsrc, reassignments);
				}
			}
		}
	}


	private void handleStackMerging(Stack<Variable> localStack, int jumpsrc, int jumpdest) {
		// destination already destacked, may need to map current stack
		if (!canonicalStackForBranchJoinJumpdest.containsKey(jumpdest)) {
			throw new IllegalStateException("target jumpdest processed, but no canonical stack defined");
		}
		Stack<Variable> canonicalStack = canonicalStackForBranchJoinJumpdest.get(jumpdest);
		if (localStack.size() != canonicalStack.size()) {
			sawMergeWithDiffStackSize = true;
			// can apparently happen for shared error handling code
			/*if (controlFlowGraph.containsKey(jumpdest)) {
				// find out if all paths from `jumpdest` lead to error
				Set<Integer> reachable = ControlFlowDetector.getAllReachableBranches(controlFlowGraph, jumpdest);
				// check that it does not contain a valid exit point
				boolean allPathsError = !reachable.contains(ControlFlowDetector.DEST_EXIT);
				if (allPathsError) {
					// ignore stack mismatch
					log.println("[ignored] Branch merge: stack size mismatch: canonical @" + toHex(jumpdest) +
							" with size " + canonicalStack.size() + " vs local @" + toHex(jumpsrc) + " with size " + localStack.size());
					return;
				}
			}*/
			// so check if all paths lead to error, and if so just don't merge the stacks, since it doesn't matter anyway (?)
			log.println("Branch merge: stack size mismatch: canonical @" + toHex(jumpdest) +
					" with size " + canonicalStack.size() + " vs local @" + toHex(jumpsrc) + " with size " + localStack.size());
		}
		Multimap<Variable, Variable> mapToCanonical = HashMultimap.create();

		for (int i = 1, n = Math.min(localStack.size(), canonicalStack.size()); i <= n; ++i) {
			mapToCanonical.put(localStack.get(localStack.size() - i), canonicalStack.get(canonicalStack.size() - i));
		}
		log.println("stack merging from @" + toHex(jumpsrc) + " into @" + toHex(jumpdest));
		mapToCanonical.asMap().forEach((variable, canonicals) ->
				canonicals.forEach(canonical -> log.println(" " + canonical + " <- " + variable)));

		if (mapToCanonical.size() != mapToCanonical.values().stream().distinct().count()) {
			throw new IllegalStateException("a canonical variable is assigned multiple times");
		}

		// create re-assignemt instructions
		if (variableReassignments.containsKey(jumpsrc)) {
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

		variableReassignments.put(jumpsrc, reassignments);
	}


	/**
	 * Get the decompiled instructions.
	 * @return decompiled instructions.
	 */
	public Instruction[] getInstructions() {
		Resolver<RawInstruction, String> labelResolver = instructionFactory.getLabelResolver();

		log.println("Generating virtual method instructions...");
		// convert jumps and jumpdests to method statements
		IntStream.range(0, instructions.length)
				.filter(offset -> instructions[offset] != null)
				.forEach(offset -> {
					if (rawInstructions[offset].opcode == OpCodes.JUMP &&
							ControlFlowDetector.isJumpMethodReturn(offset, rawInstructions)) {
						// this JUMP is a method return
						Instruction methodReturn = new _VirtualMethodReturn()
								.setRawInstruction(rawInstructions[offset])
								.setInput(getReturnVarsForMethod(methodDetector.getMethodBcoForReturnJump(offset)))
								.setOutput(NO_VARIABLES);
						instructions[offset] = methodReturn;
					}
					else if (rawInstructions[offset].opcode == OpCodes.JUMPDEST && methodHeads.contains(offset)) {
						// this JUMPDEST is the begin of a method
						Instruction methodHead = new _VirtualMethodHead(labelResolver.resolve(rawInstructions[offset]))
								.setRawInstruction(rawInstructions[offset])
								.setInput(NO_VARIABLES)
								.setOutput(getArgumentsForMethod(offset));
						instructions[offset] = methodHead;
					}
					else if (rawInstructions[offset].opcode == OpCodes.JUMP &&
							methodHeads.contains(Iterables.getFirst(jumps.get(offset), -42))) {
						// this JUMP is a method call/invocation
						Collection<Integer> dests = jumps.get(offset);
						if (dests == null || dests.size() != 1)
							throw new IllegalStateException();
						int methodBco = dests.iterator().next();
						Instruction methodCall = new _VirtualMethodInvoke(labelResolver.resolve(rawInstructions[methodBco]))
								.setRawInstruction(rawInstructions[offset])
								.setInput(getArgumentsForMethodCall(offset))
								.setOutput(getReturnVarsForMethodCall(offset));
						instructions[offset] = methodCall;
					}
				});

		// create instruction flow graph, so we don't have to mess with bytecode offsets,
		// and can insert/remove instructions without breaking references
		// i.e. each instruction has prev & next instrucions
		IntStream.range(0, instructions.length)
				.filter(offset -> instructions[offset] != null)
				.forEach(offset -> {
					Instruction instr = instructions[offset];
					if (instr instanceof Jump) {
						instr.setPrev(prevNonNullItem(offset, instructions));
						Jump jumpInstruction = (Jump) instr;

						if (jumps.get(offset).stream().anyMatch(target -> target >= 0)) {
							jumps.get(offset).stream().filter(target -> target >= 0)
									.forEach(target -> jumpInstruction.addOutgoingBranch(instructions[target]));
						}
						else {
							log.println("replacing error jump with throw @" + toHex(offset));
							// error jump, replace with throw()
							instr = new Invalid().setInput(NO_VARIABLES).setOutput(NO_VARIABLES)
									.setRawInstruction(instr.getRawInstruction());
							instructions[offset] = instr;
							Instruction prev = prevNonNullItem(offset, instructions);
							prev.setNext(instr);
							instr.setPrev(prev);
						}

						if (instr instanceof _VirtualMethodInvoke) {
							instr.setNext(nextNonNullItem(offset, instructions));
						}
					}
					else if (instr instanceof JumpI) {
						instr.setPrev(prevNonNullItem(offset, instructions));
						instr.setNext(nextNonNullItem(offset, instructions));
						JumpI jumpInstruction = (JumpI) instr;
						jumps.get(offset).stream().filter(target -> target >= 0)
								.forEach(target -> jumpInstruction.addOutgoingBranch(instructions[target]));
					}
					else if (instr instanceof JumpDest) {
						// add previous instruction if it's not a hard jump
						Instruction prev = prevNonNullItem(offset, instructions);
						if ((prev instanceof Jump && !(prev instanceof _VirtualMethodInvoke)) || prev instanceof Return
								|| prev instanceof Stop || prev instanceof SelfDestruct || prev instanceof Invalid) {
							// no prev
						}
						else {
							instr.setPrev(prev);
						}
						JumpDest jumpdestInstruction = (JumpDest) instr;
						jumpsInv.get(offset).forEach(source -> jumpdestInstruction.addIncomingBranch(instructions[source]));

						instr.setNext(nextNonNullItem(offset, instructions));
					}
					else if (instr instanceof Return || instr instanceof Stop || instr instanceof SelfDestruct || instr instanceof Invalid) {
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

		variableReassignments.forEach((bco, variableMap) -> {
			if (variableMap.entrySet().stream().noneMatch(map -> map.getKey() != map.getValue())) {
				// nothing to reassign
				return;
			}
			if (rawInstructions[bco].opcode == OpCodes.JUMP) {
				// reassign just before the jump
				// from: PREV -> JUMP -> JUMPDEST
				//   to: PREV -> reassignments -> JUMP -> JUMPDEST
				List<Instruction> injectedInstrs = new ArrayList<>();
				// original preceding instruction
				injectedInstrs.add(instructions[bco].getPrev());
				// create reassignments
				variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
						.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
				// original jump
				injectedInstrs.add(instructions[bco]);
				// link instructions
				for (int i = 1; i < injectedInstrs.size(); ++i) {
					injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
					injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
				}
			}
			else if (rawInstructions[bco].opcode == OpCodes.JUMPI) {
				// reassign after the jump but before reaching the target, so need to create a new intermediate branch
				// from: JUMPI -> JUMPDEST
				//   to: JUMPI -> JUMPDEST(virtual) -> reassignments -> JUMP(virtual) -> JUMPDEST
				List<Instruction> injectedInstrs = new ArrayList<>();

				// original jumpi instruction
				JumpI origJumpi = (JumpI) instructions[bco];
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
			}
			else {
				if (nextNonNullItem(bco, rawInstructions).opcode != OpCodes.JUMPDEST) {
					throw new IllegalStateException("expected JUMPDEST instruction after marked linear code");
				}
				// need to reassign in linear code, i.e. just before the jumpdest
				// from: PREV -> JUMPDEST
				//   to: PREV -> reassignments -> JUMPDEST
				List<Instruction> injectedInstrs = new ArrayList<>();
				// original instruction
				injectedInstrs.add(instructions[bco]);
				// create reassignments
				variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
						.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
				// following jumpdest
				injectedInstrs.add(instructions[bco].getNext());
				// link instructions
				for (int i = 1; i < injectedInstrs.size(); ++i) {
					injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
					injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
				}
			}
		});

		variableReassignmentsInline.forEach((bco, variableMap) -> {
			if (rawInstructions[bco].opcode != OpCodes.JUMPDEST) {
				throw new IllegalStateException("inline reassignment expected to be used only at JUMPDEST");
			}
			// need to reassign in linear code, i.e. just before the jumpdest
			// from: PREV -> JUMPDEST
			//   to: PREV -> reassignments -> JUMPDEST
			List<Instruction> injectedInstrs = new ArrayList<>();
			// original instruction
			injectedInstrs.add(instructions[bco].getPrev());
			// create reassignments
			variableMap.entrySet().stream().filter(map -> map.getKey() != map.getValue())
					.forEach(map -> injectedInstrs.add(new _VirtualAssignment(map.getKey(), map.getValue())));
			// following jumpdest
			injectedInstrs.add(instructions[bco]);
			// link instructions
			for (int i = 1; i < injectedInstrs.size(); ++i) {
				injectedInstrs.get(i).setPrev(injectedInstrs.get(i - 1));
				injectedInstrs.get(i - 1).setNext(injectedInstrs.get(i));
			}
		});

		return instructions;
	}


	/**
	 * Get the argument Variables for a given method.
	 * @param methodBco method bytecode offset.
	 * @return array of argument Variables.
	 */
	public Variable[] getArgumentsForMethod(int methodBco) {
		return argumentsForMethod.get(methodBco);
	}


	/**
	 * Get the return Variables for a given method.
	 * @param methodBco method bytecode offset.
	 * @return array of return Variables.
	 */
	public Variable[] getReturnVarsForMethod(int methodBco) {
		return returnVarsForMethod.get(methodBco);
	}


	/**
	 * Get the Variables used for a method call.
	 * @param jumpBco bytecode offset of the jump instruction representing the method call.
	 * @return array of argument Variables.
	 */
	public Variable[] getArgumentsForMethodCall(int jumpBco) {
		return argumentsForMethodCall.get(jumpBco);
	}


	/**
	 * Get the Variables returned from a method call.
	 * @param jumpBco bytecode offset of the jump instruction representing the method call.
	 * @return array of return Variables.
	 */
	public Variable[] getReturnVarsForMethodCall(int jumpBco) {
		return returnVarsForMethodCall.get(jumpBco);
	}

}
