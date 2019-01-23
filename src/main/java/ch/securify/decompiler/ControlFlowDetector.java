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
import ch.securify.utils.ArrayUtil;
import ch.securify.utils.Resolver;
import ch.securify.utils.StackUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ControlFlowDetector {


	private static final byte[] DUMMY_DATA = new byte[0];

	/** Exit branch */
	public static final int DEST_ERROR = -1;
	public static final int DEST_EXIT = -2;

	private static final RichBranch BRANCH_ERROR = RichBranch.c(DEST_ERROR, new Stack<>());
	private static final RichBranch BRANCH_EXIT = RichBranch.c(DEST_EXIT, new Stack<>());


	private PrintStream log;


	/** Control flow graph: branch point (opcode offset) -> possible jump targets & stack state */
	private Multimap<Integer, RichBranch> richBranches;

	/** Control flow graph: branch point (opcode offset) -> possible targets */
	private Multimap<Integer, Integer> branches;

	/** Branches of conditional jumps that weren't taken (JUMPI bco -> JUMPI bco + 1) */
	private Multimap<Integer, Integer> dummyBranches;

	/** Control flow graph reversed: jump targets -> branch point */
	private Multimap<Integer, Integer> reversedBranches;


	/**
	 * Create a control flow graph.
	 * @param rawInstructions EVM instructions.
	 */
	public void computeBranches(RawInstruction[] rawInstructions, final PrintStream log) {
		this.log = log;

		richBranches = HashMultimap.create();
		dummyBranches = HashMultimap.create();

		computeBranches(rawInstructions, 0, false, new Stack<>());

		// remove rich branch data
		branches = HashMultimap.create();
		richBranches.asMap().forEach((src, richBranches) -> richBranches.forEach(richBranch -> {
			branches.put(src, richBranch.jumpDest);
		}));

		// join local branches, i.e. branches from non-jumped conditional jumps
		for (Integer jumpSrc : dummyBranches.keySet()) {
			Collection<Integer> intermediateJumpDests = dummyBranches.get(jumpSrc);
			for (Integer intermediateJumpDest : intermediateJumpDests) {
				if (branches.get(intermediateJumpDest).contains(DEST_ERROR) ||
						branches.get(intermediateJumpDest).contains(DEST_EXIT)) {
					// don't join branches if they go indirectly to the ERROR or EXIT tag
					continue;
				}
				branches.remove(jumpSrc, intermediateJumpDest);
				if (branches.get(intermediateJumpDest).size() == 0) {
					throw new IllegalStateException("missing continuation from local branch at " +
							String.format("%02X", intermediateJumpDest) + ", implied by dummybranch " +
							String.format("%02X", jumpSrc) + " -> " + String.format("%02X", intermediateJumpDest));
				}
				for (Integer contJumpDest : branches.get(intermediateJumpDest)) {
					branches.put(jumpSrc, contJumpDest);
				}
				branches.removeAll(intermediateJumpDest);
			}
		}

		// inverse CFG
		reversedBranches = HashMultimap.create();
		for (Integer jumpSrc : branches.keySet()) {
			Collection<Integer> jumpDests = branches.get(jumpSrc);
			for (Integer jumpDest : jumpDests) {
				reversedBranches.put(jumpDest, jumpSrc);
			}
		}
	}


	/**
	 * Partially executes the EVM code to map the jump instructions to potential targets,
	 * such that a control flow graph can be created.
	 * @param rawInstructions EVM instructions.
	 * @param branchStartOffset bytecode offset to start execution from.
	 * @param isLinearContinuedBranch whether this is a branch stared by a not-executed conditional jump.
	 * @param evmStack current value stack.
	 */
	private void computeBranches(RawInstruction[] rawInstructions, int branchStartOffset, boolean isLinearContinuedBranch, Stack<byte[]> evmStack) {
		for (int pc = branchStartOffset; ; pc = ArrayUtil.nextNonNullIndex(pc, rawInstructions)) {
			final int opcode = rawInstructions[pc].opcode;

			if (opcode == OpCodes.JUMP) {
				byte[] item = evmStack.pop();
				//if (item == DUMMY_DATA) throw new AssumptionViolatedException("dynamic jump");
				int jumpdestOffset = new BigInteger(1, item).intValue();

				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));

				RichBranch targetBranch = RichBranch.c(jumpdestOffset, evmStack);

				boolean alreadyVisitedDestBranch = richBranches.containsEntry(pc, targetBranch);
				if (!alreadyVisitedDestBranch) {
					if (rawInstructions[jumpdestOffset].opcode == OpCodes.JUMPDEST) {
						richBranches.put(pc, targetBranch);
						Stack<byte[]> branchedStack = StackUtil.copyStack(evmStack);
						computeBranches(rawInstructions, jumpdestOffset, false, branchedStack);
					}
					else {
						// this is basically a jump to the exception handler of the EVM
						richBranches.put(pc, BRANCH_ERROR);
						if (jumpdestOffset != 0) {
							log.println("computeBranches(): invalid jump destination " + HexPrinter.toHex(jumpdestOffset) +
									", jumping from " + HexPrinter.toHex(pc));
						}
					}
				}
				// end of current branch
				return;
			}
			else if (opcode == OpCodes.JUMPI) {
				byte[] item = evmStack.pop();
				//if (item == DUMMY_DATA) throw new AssumptionViolatedException("dynamic jump");
				int jumpdestOffset = new BigInteger(1, item).intValue();
				evmStack.pop(); // pop branch condition value

				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));

				RichBranch targetBranch = RichBranch.c(jumpdestOffset, evmStack);
				RichBranch localBranch = RichBranch.c(pc + 1, evmStack);

				boolean alreadyVisitedDestBranch = richBranches.containsEntry(pc, targetBranch);
				boolean alreadyVisitedLocalBranch = richBranches.containsEntry(pc, localBranch);

				richBranches.put(pc, localBranch);

				if (rawInstructions[pc + 1].opcode != OpCodes.JUMPDEST) {
					// only add a dummy branch if the local target is not a jumpdest
					dummyBranches.put(pc, pc + 1);
				}

				// go to other branch
				if (!alreadyVisitedDestBranch) {
					if (rawInstructions[jumpdestOffset].opcode == OpCodes.JUMPDEST) {
						// track branching branch
						richBranches.put(pc, targetBranch);
						Stack<byte[]> branchedStack = StackUtil.copyStack(evmStack);
						computeBranches(rawInstructions, jumpdestOffset, false, branchedStack);
					}
					else {
						// this is basically a jump to the exception handler of the EVM
						richBranches.put(pc, BRANCH_ERROR);
						if (jumpdestOffset != 0) {
							log.println("computeBranches(): invalid jump destination " + HexPrinter.toHex(jumpdestOffset) +
									", jumping from " + HexPrinter.toHex(pc));
						}
					}
				}

				// continue on current branch
				if (!alreadyVisitedLocalBranch) {
					Stack<byte[]> branchedStack = StackUtil.copyStack(evmStack);
					computeBranches(rawInstructions, pc + 1, true, branchedStack);
				}

				return;
			}
			else if (opcode == OpCodes.STOP) {
				// end of execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				richBranches.put(pc, BRANCH_EXIT);
				return;
			}
			else if (opcode == OpCodes.RETURN) {
				// end of execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				richBranches.put(pc, BRANCH_EXIT);
				return;
			}
			else if (opcode == OpCodes.REVERT) {
				// end of execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				richBranches.put(pc, BRANCH_ERROR);
				return;
			}
			else if (opcode == OpCodes.SELFDESTRUCT) {
				// end of execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				richBranches.put(pc, BRANCH_EXIT);
				return;
			}
			else if (OpCodes.isInvalid(opcode)) {
				// end of execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				richBranches.put(pc, BRANCH_ERROR);
				return;
			}
			else if (opcode == OpCodes.JUMPDEST && (pc != branchStartOffset || isLinearContinuedBranch)) {
				// jumpdest in the middle of linear execution, possible incoming branch
				// add intermediate branch node, but continue with local execution
				addExecutionBranch(branchStartOffset, RichBranch.c(pc, evmStack));
				branchStartOffset = pc; // "new" current branch starting at current PC
				continue;
			}
			else if ((opcode == OpCodes.AND)) {
				// Compute the AND operation on the top two elements of the stack
				byte[] first = evmStack.pop();
				byte[] second = evmStack.pop();
				int resultLength = Math.max(first.length, second.length);
				byte[] result = new byte[resultLength];
				for (int idx = 0; idx < result.length; idx++) {
					int firstIdx = first.length - resultLength + idx;
					byte firstByte = firstIdx >= 0 ? first[firstIdx] : (byte)0;
					int secondIdx = second.length - resultLength + idx;
					byte secondByte = secondIdx >= 0 ? second[secondIdx] : (byte)0;
					result[idx] = (byte) (firstByte & secondByte);
				}
				evmStack.push(result);
				continue;
			}

			// pop items according to current instruction
			for (int i = OpCodes.getPopCount(opcode); i > 0; --i) {
				evmStack.pop();
			}

			// push new items according to current instruction
			int stackIndex;
			if (OpCodes.isPush(opcode) > -1) {
				// push exact value
				evmStack.push(rawInstructions[pc].data);
			}
			else if ((stackIndex = OpCodes.isDup(opcode)) > -1) {
				// push exact value
				evmStack.push(evmStack.get(evmStack.size() - stackIndex - 1));
			}
			else if ((stackIndex = OpCodes.isSwap(opcode)) > -1) {
				// swap values
				int stackIndexBtm = evmStack.size() - stackIndex - 1;
				byte[] otherVal = evmStack.get(stackIndexBtm);
				byte[] topVal = evmStack.pop();
				evmStack.push(otherVal);
				evmStack.set(stackIndexBtm, topVal);
			}
			else {
				for (int i = OpCodes.getPushCount(opcode); i > 0; --i) {
					// push dummy value
					evmStack.push(DUMMY_DATA);
				}
			}
		}
	}


	private void addExecutionBranch(int branchStartOffset, RichBranch outgoingJumpOffset) {
		if (branchStartOffset != outgoingJumpOffset.jumpDest) {
			// put in local linear execution (i.e. edge from incoming jump to outgoing jump) into the flow
			richBranches.put(branchStartOffset, outgoingJumpOffset);
		}
	}


	/**
	 * Get the computed control flow graph.
	 * @return Multimap that maps from jump instructions to potential jump destinations.
	 */
	public Multimap<Integer, Integer> getBranches() {
		return branches;
	}


	/**
	 * Get the reversed computed control flow graph, i.e. edges pointing from jump destinations to jump source.
	 * @return Multimap that maps from jump destinations to jump instructions.
	 */
	public Multimap<Integer, Integer> getBranchesReversed() {
		return reversedBranches;
	}


	public int getQuickestJumpTowardsExit(int src, Collection<Integer> returnJumps, Resolver<Integer, Boolean> isMethodHeadResolver) {
		Multimap<Integer, Integer> branches = getBranches();

		Map<Integer, Integer> predecessors = new HashMap<>();

		Map<Integer, Integer> distances = new HashMap<>();
		branches.asMap().forEach((u, dsts) -> dsts.forEach(dst -> distances.put(dst, Integer.MAX_VALUE)));
		distances.put(src, 0);

		Queue<Integer> q = new PriorityQueue<>(Comparator.comparingInt(distances::get));
		q.addAll(distances.keySet());

		//log.println("[DIJ] start @" + toHex(src) + " towards " + toHex(returnJumps, ", "));

		if (q.peek() != src)
			throw new IllegalStateException("first node is not start node (bug)");

		while (!q.isEmpty()) {
			Integer node = q.poll();

			//log.println("[DIJ] processing @" + toHex(node));

			if (distances.get(node) == Integer.MAX_VALUE) {
				//log.println("[DIJ] ended");
				break;
			}

			branches.get(node).stream().filter(q::contains).forEach(dst -> {
				int newDistance = distances.get(node) + (isMethodHeadResolver.resolve(dst) ? 100_000 : 1);
				if (newDistance < distances.get(dst)) {
					distances.put(dst, newDistance);
					predecessors.put(dst, node);
				}
			});

			Queue<Integer> p = new PriorityQueue<>(Comparator.comparingInt(distances::get));
			p.addAll(q);
			q = p;
		}

		AtomicInteger mindist = new AtomicInteger(Integer.MAX_VALUE);
		AtomicInteger returnJump = new AtomicInteger(-1);
		distances.forEach((node, distance) -> {
			if (returnJumps.contains(node)) {
				if (distance < mindist.get()) {
					mindist.set(distance);
					returnJump.set(node);
				}
			}
		});

		Integer pn = returnJump.get();
		while (predecessors.get(pn) != null && predecessors.get(pn) != src) {
			pn = predecessors.get(pn);
		}

		if (predecessors.get(pn) != null) {
			if (!branches.get(src).contains(pn)) {
				throw new IllegalStateException("dijkstra failed or something");
			}

			return pn;
		}

		throw new IllegalStateException("no back path found with dijkstra");
	}


	/**
	 * Check whether a jump instruction at the given bytecode offset is a method return.
	 * @param offset bytecode offset of the jump instruction.
	 * @return true if the jump is a method return.
	 */
	public static boolean isJumpMethodReturn(int offset, RawInstruction[] rawInstructions) {
		if (rawInstructions[offset].opcode != OpCodes.JUMP)
			throw new IllegalArgumentException("Instruction at offset is no JUMP");

		// assumption: a JUMP is a method return iff he instruction just before is not a PUSH
		RawInstruction prevInstruction = ArrayUtil.prevNonNullItem(offset, rawInstructions);
		return OpCodes.isPush(prevInstruction.opcode) == -1;
	}


	/**
	 * Get all branches reachable from a given source.
	 * @param controlFlowGraph
	 * @param source
	 * @return set of reachable branches
	 */
	public static Set<Integer> getAllReachableBranches(Multimap<Integer, Integer> controlFlowGraph, Integer source) {
		// find log if all paths from `jumpdest` lead to error
		Set<Integer> reached = new HashSet<>();
		Queue<Integer> bfs = new LinkedList<>();
		bfs.add(source);
		while (!bfs.isEmpty()) {
			controlFlowGraph.get(bfs.poll()).stream()
					.filter(b -> !reached.contains(b))
					.forEach(b -> { bfs.add(b); reached.add(b); });
		}
		return reached;
	}


	/**
	 * Check if the given jump destination (bytecode offset) is an ERROR or EXIT, not a real jumpdest.
	 * @param jumpdest bytecode offset.
	 * @return
	 */
	public static boolean isVirtualJumpDest(int jumpdest) {
		return jumpdest < 0;
	}


	private static class RichBranch {
		private final int jumpDest;
		private final String stackState;

		private RichBranch(int jumpDest, String stackState) {
			this.jumpDest = jumpDest;
			this.stackState = stackState;
		}

		@Override
		public int hashCode() {
			return jumpDest ^ stackState.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof RichBranch))
				return false;
			RichBranch other = (RichBranch) obj;
			return jumpDest == other.jumpDest && stackState.equals(other.stackState);
		}

		private static RichBranch c(int jumpDest, Stack<byte[]> stack) {
			return new RichBranch(jumpDest,
					stack.stream()
							.skip(Math.max(0, stack.size() - 20))
							.map((item) -> item.length == 0 ? "?" : HexPrinter.toHex(item))
							.collect(Collectors.joining(",")));
		}
	}

}
