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


package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.printer.HexPrinter;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class JumpI extends BranchInstruction {

	public String targetLabel;

	public JumpI(String targetLabel) {
		setTargetLabel(targetLabel);
	}

	public void setTargetLabel(String targetLabel) {
		if (targetLabel == null) {
			throw new IllegalArgumentException("JumpI Instruction targetLabel is null");
		}
		this.targetLabel = targetLabel;
	}

	@Override
	public String getStringRepresentation() {
		Instruction merger = getMergeInstruction();
		return "if " + getCondition() + ": goto " + targetLabel +
				(merger != null && merger.getRawInstruction() != null ?
				 (" [merge @" + HexPrinter.toHex(merger.getRawInstruction().offset) + "]") : "");
	}

	public Variable getCondition() {
		return getInput()[1];
	}

	/**
	 * Get the Instruction at the jump target.
	 * @return Instruction, or null if it's an 'exit'.
	 */
	public Instruction getTargetInstruction() {
		return getOutgoingBranches().size() > 0 ? getOutgoingBranches().iterator().next() : null;
	}

	/**
	 * Get the Instruction where the branches created by this conditional jump merge.
	 * @return Instruction at the merge point, null if the branches do not merge,
	 */
	public Instruction getMergeInstruction() {
		Instruction branchA = getNext();
		Instruction branchB = getTargetInstruction();
		if (branchA == null || branchB == null)
			return null;

		// scan first branch (linear)
		Set<Instruction> firstBranchInstrs = getAllReachableInstructions(branchA);
		firstBranchInstrs.add(this);

		// scan second branch (jump target)
		return getFirstMutualInstruction(firstBranchInstrs, branchB);
	}

	private Set<Instruction> getAllReachableInstructions(Instruction start) {
		Set<Instruction> reachable = new HashSet<>();
		reachable.add(start);
		Queue<Instruction> bfs = new LinkedList<>();
		bfs.add(start);
		while (!bfs.isEmpty()) {
			Instruction i = bfs.poll();
			// add next instruction
			Instruction next = i.getNext();
			if (next != null && !reachable.contains(next)) {
				reachable.add(next);
				bfs.add(next);
			}
			// check for additional jump destinations
			if (i instanceof BranchInstruction && !(i instanceof _VirtualInstruction)) {
				((BranchInstruction)i).getOutgoingBranches().forEach(jdest -> {
					if (!reachable.contains(jdest)) {
						reachable.add(jdest);
						bfs.add(jdest);
					}
				});
			}
		}
		return reachable;
	}

	private Instruction getFirstMutualInstruction(Set<Instruction> scanned, Instruction start) {
		Set<Instruction> reachable = new HashSet<>();
		Queue<Instruction> bfs = new LinkedList<>();
		bfs.add(start);
		while (!bfs.isEmpty()) {
			Instruction i = bfs.poll();
			if (scanned.contains(i)) {
				// found merger
				if (i instanceof JumpDest) {
					// real merge
					return i;
				}
				else {
					// reached common instruction that is no jumpdest, so it's probably not an if-else block
					return null;
				}
			}
			// add next instruction
			Instruction next = i.getNext();
			if (next != null && !reachable.contains(next)) {
				reachable.add(next);
				bfs.add(next);
			}
			// check for additional jump destinations
			if (i instanceof BranchInstruction && !(i instanceof _VirtualInstruction)) {
				((BranchInstruction)i).getOutgoingBranches().forEach(jdest -> {
					if (!reachable.contains(jdest)) {
						reachable.add(jdest);
						bfs.add(jdest);
					}
				});
			}
		}
		return null;
	}


}
