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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class _VirtualMethodHead extends JumpDest implements _VirtualInstruction {

	public static final String METHOD_NAME_PREFIX_ABI = "method_abi_";
	public static final String METHOD_NAME_PREFIX_UNKNOWN = "method_private_";

	public _VirtualMethodHead(String label) {
		super(label);
	}

	@Override
	public String getStringRepresentation() {
		return getLabel() + "(" + Arrays.stream(getOutput()).map(Variable::toString).collect(Collectors.joining(", ")) + ")";
	}

	/**
	 * Get all returns of this method.
	 * @return
	 */
	public Collection<_VirtualMethodReturn> getReturnInstructions() {
		Queue<Instruction> instructionsToProcess = new LinkedList<>();
		instructionsToProcess.add(this);
		Set<Instruction> processedInstructions = new HashSet<>();

		Set<_VirtualMethodReturn> returnInstructions = new HashSet<>();

		while (!instructionsToProcess.isEmpty()) {
			Instruction instruction = instructionsToProcess.poll();
			if (processedInstructions.contains(instruction)) {
				// branch already processed
				continue;
			}
			processedInstructions.add(instruction);

			if (instruction instanceof _VirtualMethodReturn) {
				returnInstructions.add((_VirtualMethodReturn) instruction);
			}
			if (instruction.getNext() != null) {
				instructionsToProcess.add(instruction.getNext());
			}
			if (instruction instanceof BranchInstruction && !(instruction instanceof _VirtualInstruction)) {
				instructionsToProcess.addAll(((BranchInstruction) instruction).getOutgoingBranches());
			}
		}

		return returnInstructions;
	}

}
