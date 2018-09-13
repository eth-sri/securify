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

import ch.securify.decompiler.instructions.JumpDest;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions._VirtualInstruction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

public class DependencyResolver {


	/**
	 * Create a dependency graph for the instructions.
	 * @param instructions
	 */
	public static void resolveDependencies(List<Instruction> instructions) {
	    // public values remain unresolved...
		resolveDependencies(instructions, true);
	}


	/**
	 * Create a dependency graph for the instructions.
	 * @param instructions
	 */
	public static void resolveDependencies(List<Instruction> instructions, boolean ignoreUnresolved) {
		instructions.forEach(instruction -> {
					// determine dependencies for this instruction
					Stream.concat(Arrays.stream(instruction.getInput()), instruction.getMemoryInputs().stream())
							.filter(Objects::nonNull)
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
								if (!foundDependency && !ignoreUnresolved) {
									throw new IllegalStateException("Dependency resolver reached method head. " +
											"Should have resolved all dependencies by now (but didn't for '" + inputVar + "'). " +
											"Possible use of undeclared variable in this scope.");
								}
							});
				}
		);
	}


	/**
	 * Get all dependencies of this instruction, transitively.
	 * @param source
	 * @return All dependencies
	 */
	public static List<Instruction> getDependencies(Instruction source) {
		List<Instruction> dependencies = new ArrayList<>();

		Queue<Instruction> dependenciesToProcess = new LinkedList<>();
		dependenciesToProcess.add(source);

		Set<Instruction> processedInstructions = new HashSet<>();

		while (!dependenciesToProcess.isEmpty()) {
			Instruction dependency = dependenciesToProcess.poll();
			if (processedInstructions.contains(dependency)) {
				continue;
			}
			processedInstructions.add(dependency);
			dependencies.add(dependency);
			dependenciesToProcess.addAll(dependency.getDependencies());
		}

		return dependencies;
	}


}
