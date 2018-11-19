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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class BranchInstruction extends Instruction {

	private final Set<Instruction> incomingBranches = new LinkedHashSet<>();
	private final Set<Instruction> outgoingBranches = new LinkedHashSet<>();


	public void addIncomingBranch(Instruction prev) {
		if (prev != null) {
			incomingBranches.add(prev);
		}
	}


	public Collection<Instruction> getIncomingBranches() {
		return incomingBranches;
	}


	public void removeIncomingBranch(Instruction prev) {
		incomingBranches.remove(prev);
	}


	public void clearIncomingBranches() {
		incomingBranches.clear();
	}


	public void addOutgoingBranch(Instruction next) {
		if (next != null) {
			outgoingBranches.add(next);
		}
	}


	public void clearOutgoingBranches() {
		outgoingBranches.clear();
	}


	public Collection<Instruction> getOutgoingBranches() {
		return outgoingBranches;
	}

}
