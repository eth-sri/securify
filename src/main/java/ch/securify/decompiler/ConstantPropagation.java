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
import ch.securify.decompiler.instructions.MStore;
import ch.securify.decompiler.instructions.BranchInstruction;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.MLoad;
import ch.securify.decompiler.instructions.MSize;
import ch.securify.decompiler.instructions.MStore8;
import ch.securify.decompiler.instructions.SLoad;
import ch.securify.decompiler.instructions.SStore;
import ch.securify.decompiler.instructions.Sha3;
import ch.securify.decompiler.instructions.StaticCall;
import ch.securify.decompiler.instructions._VirtualInstruction;
import ch.securify.decompiler.instructions._VirtualMethodHead;
import ch.securify.decompiler.instructions._VirtualMethodReturn;
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;

public class ConstantPropagation {


	public static List<Instruction> propagate(List<Instruction> instructions) {
		Queue<Instruction> branchesToProcess = new LinkedList<>();
		branchesToProcess.add(instructions.get(0));
		Set<Instruction> processedInstructions = new HashSet<>();

		Map<Instruction, ProgramState> canonicalProgramStates = new HashMap<>();
		canonicalProgramStates.put(instructions.get(0), new ProgramState());

		int consecutiveDelayedBranches = 0;

		while (!branchesToProcess.isEmpty()) {
			Instruction instruction = branchesToProcess.poll();
			if (processedInstructions.contains(instruction)) {
				// branch already processed
				continue;
			}
			ProgramState programState = canonicalProgramStates.get(instruction);
			if (programState == null) {
				throw new IllegalStateException("no ProgramState available at instruction " +
						instruction.getDebugRepresentation().replaceAll("\\s+", " "));
			}
			do {
				if (instruction instanceof JumpDest) {
					if (processedInstructions.contains(instruction)) {
						// reached merger through linear flow
						// should only happen for loop branches, otherwise the local flow should be always processed first
						break; // continue with next branch
					}

					boolean unprocessedPrecedingBranches = ((JumpDest)instruction).getIncomingBranches()
							.stream().anyMatch(src -> !processedInstructions.contains(src))
							|| (instruction.getPrev() != null && !processedInstructions.contains(instruction.getPrev()));
					if (unprocessedPrecedingBranches && consecutiveDelayedBranches <= branchesToProcess.size()) {
						// postpone this branch because of unprocessed preceding instructions
						consecutiveDelayedBranches++;
						branchesToProcess.add(instruction);

						if (instruction.getPrev() != null && !canonicalProgramStates.containsKey(instruction)) {
							// jumpdest in local flow without canonical program state
							canonicalProgramStates.put(instruction,  new ProgramState(programState));
						}

						break; // continue with next branch
					}
					else {
						consecutiveDelayedBranches = 0;
					}

					if (unprocessedPrecedingBranches) {
						// we have unprocessed preceding instructions but need to continue with this branch
						// TODO: so check if we really have to wipe the memory/store contents
						// (if any mstore/sstore is used in the unprocessed part)
						Variable pollution = new Variable();
						pollution.addValueType(Variable.TYPE_ANY);
						programState.polluteStorage(pollution); // TODO: pollute with what?
						programState.polluteMemory(pollution);
					}

					if (instruction.getPrev() != null) {
						if (canonicalProgramStates.containsKey(instruction)) {
							// if this is a jumpdest in a local flow that has already
							// a canonical program state assigned from elsewhere we need to merge states
							programState.merge(canonicalProgramStates.get(instruction));
						}
					}
				}

				// handle storage writes
				if (instruction instanceof SStore) {
					Variable storeOffsetVar = instruction.getInput()[0];
					Variable valueVar = instruction.getInput()[1];
					if (storeOffsetVar.hasConstantValue()) {
						BigInteger storeOffset = BigIntUtil.fromInt256(storeOffsetVar.getConstantValue());
						programState.storage.put(storeOffset, valueVar);
					}
					else {
						// write to unknown location: clear whole storage
						programState.polluteStorage(valueVar);
					}
				}
				// handle memory writes
				else if (instruction instanceof MStore) {
					Variable memOffsetVar = instruction.getInput()[0];
					Variable valueVar = instruction.getInput()[1];
					if (memOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(memOffsetVar.getConstantValue());
						programState.heap.put(memOffset, valueVar);
						programState.updateMsize(memOffset);
						// clear memory that may overlap with the new variable
						//for (int i = 1; i < 32; ++i) {
						//	BigInteger invalidatedMemOffset = memOffset.subtract(BigInteger.valueOf(i));
						//	programState.heap.remove(invalidatedMemOffset);
						//}
					}
					else {
						// write to unknown location: clear whole memory
						// TODO: keep entry at 0x40? since that is the memory index root? (applies to all global wipes)
						programState.polluteMemory(valueVar);
					}
				}
				else if (instruction instanceof MStore8) {
					Variable memOffsetVar = instruction.getInput()[0];
					Variable valueVar = instruction.getInput()[1];
					if (memOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(memOffsetVar.getConstantValue());
						programState.heap.put(memOffset, valueVar);
						programState.updateMsize(memOffset.subtract(BigInteger.valueOf(31)));
						// don't write variable to memory, but wipe out any other affected variable // nope
						//for (int i = 0; i < 32; ++i) {
						//	BigInteger invalidatedMemOffset = memOffset.subtract(BigInteger.valueOf(i));
						//	programState.heap.remove(invalidatedMemOffset);
						//}
					}
					else {
						// write to unknown location: clear whole memory
						programState.polluteMemory(valueVar);
					}
				}
				else if (instruction instanceof Call || instruction instanceof StaticCall) {
					Variable memOffsetVar;
					Variable memLenVar;
					if (instruction instanceof Call) {
						memOffsetVar = instruction.getInput()[5];
						memLenVar = instruction.getInput()[6];
					} else {
					    assert instruction instanceof StaticCall;
						memOffsetVar = instruction.getInput()[4];
						memLenVar = instruction.getInput()[5];
					}
					if (memLenVar.hasConstantValue() && BigIntUtil.fromInt256(memLenVar.getConstantValue()).equals(BigInteger.ZERO)) {
						// zero-length target memory
					}
					else if (memOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(memOffsetVar.getConstantValue());
						BigInteger memRangeStart = memOffset;
						BigInteger memRangeEnd = memLenVar.hasConstantValue() ?
												 BigIntUtil.fromInt256(memLenVar.getConstantValue()).subtract(memOffset) : null;

						// clear memory that may overlap with the call result
						programState.heap.entrySet().removeIf(entry -> {
							// TODO: may store new variables with "call" type instead of just wiping the area
							BigInteger offset = entry.getKey();
							return (memRangeStart.compareTo(offset) <= 0 && (memRangeEnd == null || offset.compareTo(memRangeEnd) < 0));
						});
					}
					else {
						// write to unknown location: clear whole memory
						Variable pollution = new Variable();
						pollution.addValueType(Variable.TYPE_ANY);
						programState.polluteMemory(pollution); // TODO: pollute with what?
					}
				}

				// check if any input variable depends on an unprocessed instructions
				// in that case we can't know the output values
				boolean foundUnprocessedDependency = false;
				out: for (Variable input : instruction.getInput()) {
					// search all instructions that have this variable as an output.
					// if an instruction has not been processed (i.e. !processedInstructions.contains(instruction)),
					// then we set the output of the instruction being currently processed to ANY
					Queue<Instruction> backtrackBranchesToProcess = new LinkedList<>();
					Set<Instruction> backtrackProcessedInstructions = new HashSet<>();

					Instruction prevInstr = instruction.getPrev();
					while (prevInstr != null || (prevInstr = backtrackBranchesToProcess.poll()) != null) {
						if (backtrackProcessedInstructions.contains(prevInstr)) {
							prevInstr = null;
							continue;
						}
						backtrackProcessedInstructions.add(prevInstr);

						boolean ioMatch = Arrays.stream(prevInstr.getOutput()).anyMatch(outputVar -> outputVar == input);
						if (ioMatch) {
							if (!processedInstructions.contains(prevInstr)) {
								// unprocessed dependency -> set output to ANY
								foundUnprocessedDependency = true;
								break out;
							}
							prevInstr = null;
							continue;
						}

						if (prevInstr instanceof JumpDest && !(prevInstr instanceof _VirtualInstruction)) {
							backtrackBranchesToProcess.addAll(((JumpDest) prevInstr).getIncomingBranches());
						}

						prevInstr = prevInstr.getPrev();
					}
				}
				if (foundUnprocessedDependency) {
					for (Variable outputVar : instruction.getOutput()) {
						outputVar.setConstantValue(Variable.VALUE_ANY);
					}
					// TODO: pollute result types (with what?)
				}
				else {
					// if all ok, compute a constant value
					instruction.computeResultValues();
					instruction.computeResultTypes();
				}

				// handle memory reads
				if (instruction instanceof MLoad) {
					Variable memOffsetVar = instruction.getInput()[0];
					if (memOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(memOffsetVar.getConstantValue());
						Variable valueVar = programState.heap.get(memOffset);
						if (valueVar != null) {
							if (valueVar.hasConstantValue()) {
								// copy constant value over to output variable
								instruction.getOutput()[0].setConstantValue(valueVar.getConstantValue());
							}
							else {
								instruction.addMemoryInput(valueVar);
								instruction.getOutput()[0].addValueTypes(valueVar.getValueTypes());
							}
						}
					}
					else {
						// unknown location: add all variables in memory as possible dependencies
						Instruction finalInstruction = instruction;
						programState.heap.forEach((offset, variable) -> {
							finalInstruction.addMemoryInput(variable);
							finalInstruction.getOutput()[0].addValueTypes(variable.getValueTypes());
						});
					}
					// include pollution
					for (Variable pollutingVar : programState.heapPollution) {
						instruction.addMemoryInput(pollutingVar);
						instruction.getOutput()[0].addValueTypes(pollutingVar.getValueTypes());
					}
				}
				// handle memory access by sha3
				else if (instruction instanceof Sha3) {
					Variable memOffsetVar = instruction.getInput()[0];
					if (memOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(memOffsetVar.getConstantValue());

						Variable memLengthVar = instruction.getInput()[1];
						BigInteger memRangeEnd;
						if (memLengthVar.hasConstantValue()) {
							BigInteger memLength = BigIntUtil.fromInt256(memLengthVar.getConstantValue());
							memRangeEnd = memOffset.add(memLength);
						}
						else {
							memRangeEnd = null;
						}
						BigInteger memRangeStart = memOffset.subtract(BigInteger.valueOf(31));

						Instruction finalInstruction = instruction;
						programState.heap.forEach((offset, variable) -> {
							if (memRangeStart.compareTo(offset) <= 0 && (memRangeEnd == null || offset.compareTo(memRangeEnd) < 0)) {
								finalInstruction.addMemoryInput(variable);
								finalInstruction.getOutput()[0].addValueTypes(variable.getValueTypes());
							}
						});

						boolean outputIsConstant = true;
						MessageDigest digest;
						try {
							digest = MessageDigest.getInstance("SHA-256");
							for (Variable inputVariable : finalInstruction.getMemoryInputs()) {
								if (inputVariable.hasConstantValue()) {
									digest.update(inputVariable.getConstantValue());
									finalInstruction.getOutput()[0].addHashConstant(inputVariable.getConstantValue());
								} else {
									outputIsConstant = false;
								}
							}
							if (outputIsConstant) {
								finalInstruction.getOutput()[0].setConstantValue(digest.digest());
							}
						} catch (NoSuchAlgorithmException e) {
							throw new RuntimeException("No SHA-256");
						}
					}
					else {
						// unknown location: add all variables in memory as possible dependencies
						Instruction finalInstruction = instruction;
						programState.heap.forEach((offset, variable) -> {
							finalInstruction.addMemoryInput(variable);
							finalInstruction.getOutput()[0].addValueTypes(variable.getValueTypes());
						});
					}
					// include pollution
					for (Variable pollutingVar : programState.heapPollution) {
						instruction.addMemoryInput(pollutingVar);
						instruction.getOutput()[0].addValueTypes(pollutingVar.getValueTypes());
					}
				}
				// handle memory read access by call
				else if ((instruction instanceof Call) || (instruction instanceof StaticCall)) {
					Variable inputMemLengthVar = null;
					Variable inputMemOffsetVar = null;
					if (instruction instanceof Call) {
						inputMemOffsetVar = instruction.getInput()[3];
						inputMemLengthVar = instruction.getInput()[4];
					} else if(instruction instanceof StaticCall) {
						inputMemOffsetVar = instruction.getInput()[2];
						inputMemLengthVar = instruction.getInput()[3];
					}

					if (inputMemLengthVar.hasConstantValue() && BigIntUtil.fromInt256(inputMemLengthVar.getConstantValue()).equals(BigInteger.ZERO)) {
						// zero-length target memory
					}
					else if (inputMemOffsetVar.hasConstantValue()) {
						BigInteger memOffset = BigIntUtil.fromInt256(inputMemOffsetVar.getConstantValue());

						BigInteger memRangeEnd;
						if (inputMemLengthVar.hasConstantValue()) {
							BigInteger memLength = BigIntUtil.fromInt256(inputMemLengthVar.getConstantValue());
							memRangeEnd = memOffset.add(memLength);
						}
						else {
							memRangeEnd = null;
						}
						BigInteger memRangeStart = memOffset.subtract(BigInteger.valueOf(31));

						Instruction instructionF = instruction;
						programState.heap.forEach((offset, variable) -> {
							if (memRangeStart.compareTo(offset) <= 0 && (memRangeEnd == null || offset.compareTo(memRangeEnd) < 0)) {
								instructionF.addMemoryInput(variable);
							}
						});
					}
					else {
						// could be anything, independent of our local memory state
					}
				}
				// handle storage reads
				else if (instruction instanceof SLoad) {
					Variable storeOffsetVar = instruction.getInput()[0];
					if (storeOffsetVar.hasConstantValue()) {
						BigInteger storeOffset = BigIntUtil.fromInt256(storeOffsetVar.getConstantValue());
						Variable valueVar = programState.storage.get(storeOffset);
						if (valueVar != null) {
							if (valueVar.hasConstantValue()) {
								// copy constant value over to output variable
								instruction.getOutput()[0].setConstantValue(valueVar.getConstantValue());
							}
							else {
								instruction.addMemoryInput(valueVar);
								instruction.getOutput()[0].addValueTypes(valueVar.getValueTypes());
							}
						}
					}
					else {
						// unknown read location: load everything possible
						Instruction finalInstruction = instruction;
						programState.storage.forEach((offset, variable) -> {
							finalInstruction.addMemoryInput(variable);
							finalInstruction.getOutput()[0].addValueTypes(variable.getValueTypes());
						});
					}
					// include pollution
					for (Variable pollutingVar : programState.storagePollution) {
						instruction.addMemoryInput(pollutingVar);
						instruction.getOutput()[0].addValueTypes(pollutingVar.getValueTypes());
					}
				}
				// handle msize
				else if (instruction instanceof MSize) {
					if (programState.msize.signum() != -1) {
						// copy heap size value
						instruction.getOutput()[0].setConstantValue(BigIntUtil.toInt256(programState.msize));
					}
				}

				processedInstructions.add(instruction);

				if (instruction instanceof BranchInstruction
						&& !(instruction instanceof _VirtualMethodReturn)) {
					BranchInstruction src = (BranchInstruction) instruction;
					src.getOutgoingBranches().forEach(dest -> {
						branchesToProcess.add(dest);
						// fork & merge program state
						ProgramState destProgramState = canonicalProgramStates.get(dest);
						if (destProgramState == null) {
							destProgramState = new ProgramState(programState);
							canonicalProgramStates.put(dest, destProgramState);
						}
						else {
							destProgramState.merge(programState);
						}
						if (dest instanceof _VirtualMethodHead) {
							for (int i = 0; i < src.getInput().length; ++i) {
								if (dest.getOutput()[i].getConstantValue() == Variable.VALUE_UNDEFINED) {
									dest.getOutput()[i].setConstantValue(src.getInput()[i].getConstantValue());
								}
								else if (dest.getOutput()[i].hasConstantValue()) {
									dest.getOutput()[i].setConstantValue(Variable.VALUE_ANY);
								}
								dest.getOutput()[i].addValueTypes(src.getInput()[i].getValueTypes());
							}
						}
					});
				}
			} while ((instruction = instruction.getNext()) != null);
		}

		return instructions;
	}


	private static class ProgramState {
		/** heap size, according to msize(), -1 if unknown */
		private BigInteger msize = BigInteger.ZERO;
		/** heap containing variables, byte indexed */
		private Map<BigInteger, Variable> heap = new TreeMap<>();
		/** store containing variables, item indexed */
		private Map<BigInteger, Variable> storage = new TreeMap<>();

		private Set<Variable> heapPollution = new HashSet<>();
		private Set<Variable> storagePollution = new HashSet<>();

		/**
		 * Create new empty program state.
		 */
		protected ProgramState() {
			// noop
		}

		/**
		 * Copy another program state.
		 */
		protected ProgramState(ProgramState source) {
			msize = source.msize;
			heap.putAll(source.heap);
			storage.putAll(source.storage);
			heapPollution.addAll(source.heapPollution);
			storagePollution.addAll(source.storagePollution);
		}

		/**
		 * Merge another program state into this one, invalidating all conflicting data.
		 * @param other
		 */
		protected void merge(ProgramState other) {
			// merge heap size, can keep only if both are the same
			msize = msize.equals(other.msize) ? msize : BigInteger.valueOf(-1);
			// check for storage mismatch, remove entry on mismatch
			other.storage.forEach((index, variable) -> {
				if (storage.get(index) != variable) {
					storage.remove(index);
				}
			});
			// check for memory mismatch, remove entry on mismatch
			Map<BigInteger, Variable> newHeap = new TreeMap<>();
			other.heap.forEach((offset, variable) -> {
				if (heap.get(offset) == variable) {
					newHeap.put(offset, variable);
				}
			});
			heap = newHeap;

			heapPollution.addAll(other.heapPollution);
			storagePollution.addAll(other.storagePollution);
		}

		protected void updateMsize(BigInteger writeOffset) {
			if (msize.signum() == -1) {
				return;
			}
			if (writeOffset.signum() == -1) {
				msize = writeOffset.add(BigInteger.valueOf(32));
			}
			else {
				msize = msize.max(writeOffset.add(BigInteger.valueOf(32)));
			}
		}

		protected void clearMsize() {
			msize = BigInteger.valueOf(-1);
		}

		protected void polluteMemory(Variable variable) {
			clearMsize();
			heapPollution.add(variable);
		}

		protected void polluteStorage(Variable variable) {
			storagePollution.add(variable);
		}
	}


}
