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
import ch.securify.utils.StackUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class MethodDetector {

	private PrintStream log;

	private RawInstruction[] rawInstructions;
	private Multimap<Integer, Integer> jumps;
	private Multimap<Integer, Integer> jumpsInv;
	private ControlFlowDetector cfg;
	private Map<Integer, MethodInfo> methods;

	private int[] belongsToMethod;
	private Map<Integer, Integer> argumentCountForMethod, returnVarCountForMethod;

	private Multimap<Integer, Integer> returnJumpBcoForMethod;
	private Map<Integer, Integer> methodBcoForReturnJump;

	private Map<Integer, Integer> variableStackSizeAtBeginOfMethod;
	private Map<Integer, Integer> stackAccessDepthForCallstackSize;


	private static class MethodCall {
		private int callSrc, methodHead;
		private MethodCall(int callSrc, int methodHead) {
			this.callSrc = callSrc;
			this.methodHead = methodHead;
		}
	}


	/**
	 * Reaolve methods and their properties.
	 * @param rawInstructions EVM visited.
	 * @param jumps maps jump visited to their jump destinations.
	 * @param cfg control flow graph.
	 * @param methods method information to recognize corresponding instructions.
	 */
	public void detect(RawInstruction[] rawInstructions, Multimap<Integer, Integer> jumps,
			ControlFlowDetector cfg, Map<Integer, MethodInfo> methods, PrintStream log) {
		this.log = log;

		this.rawInstructions = rawInstructions;
		this.jumps = jumps;
		this.cfg = cfg;
		this.methods = methods;

		this.argumentCountForMethod = new HashMap<>();
		this.returnVarCountForMethod = new HashMap<>();
		this.belongsToMethod = new int[rawInstructions.length];
		for (int i = 0; i < belongsToMethod.length; ++i) belongsToMethod[i] = -1;

		this.variableStackSizeAtBeginOfMethod = new HashMap<>();
		this.stackAccessDepthForCallstackSize = new HashMap<>();

		this.returnJumpBcoForMethod = HashMultimap.create();
		this.methodBcoForReturnJump = new HashMap<>();

		this.jumpsInv = HashMultimap.create();
		jumps.asMap().forEach((src, dsts) -> dsts.forEach(dst -> jumpsInv.put(dst, src)));

		argumentCountForMethod.put(0, 0);
		returnVarCountForMethod.put(0, 0);
		variableStackSizeAtBeginOfMethod.put(0, 0);
		stackAccessDepthForCallstackSize.put(0, 0);
		detect(0, 0, new Stack<>());
	}


	/**
	 * Partially executes the EVM code.
	 * @param branchStartOffset bytecode offset where the branch starts.
	 * @param operandStackSize Variable stack size at the beginning of the current branch.
	 * @param callStack call stack (method calls).
	 */
	private void detect(int branchStartOffset, int operandStackSize, Stack<MethodCall> callStack) {
		detect(branchStartOffset, branchStartOffset, operandStackSize, callStack);
	}


	/**
	 * Partially executes the EVM code.
	 * @param branchStartOffset bytecode offset where the branch starts.
	 * @param pc bytecode offset to start processing.
	 * @param operandStackSize Variable stack size at the beginning of the current branch.
	 * @param callStack call stack (method calls).
	 */
	private void detect(int branchStartOffset, int pc, int operandStackSize, Stack<MethodCall> callStack) {
		if (pc < branchStartOffset) {
			throw new IllegalArgumentException("pc < branchStartOffset");
		}

		final int pcStart = pc;
		if (methods.containsKey(pcStart)) {
			variableStackSizeAtBeginOfMethod.put(pcStart, operandStackSize);
			stackAccessDepthForCallstackSize.put(callStack.size(), operandStackSize);

			if (callStack.peek().methodHead != pcStart) {
				throw new IllegalStateException("processing method head @" + HexPrinter.toHex(pcStart) +
						", but topmost callstack item is different method @" + HexPrinter.toHex(callStack.peek().methodHead));
			}
		}

		for (; ; pc = ArrayUtil.nextNonNullIndex(pc, rawInstructions)) {
			RawInstruction rawInstruction = rawInstructions[pc];
			final int opcode = rawInstruction.opcode;

			//log.println("[MD@" + toHex(callStack.size() == 0 ? 0x00 : callStack.peek().methodHead) + "]" +
			//		new String(new char[callStack.size()]).replace("\0", "  ") +
			//		" processing @" + toHex(pc) + ": " + OpCodes.getOpName(opcode) + " < pre " + operandStackSize);

			int currentMethodBco = callStack.size() == 0 ? 0 : callStack.peek().methodHead;

			if (belongsToMethod[pc] != -1) {
                if (belongsToMethod[pc] != currentMethodBco) {
                    throw new AssumptionViolatedException("Instruction @" + HexPrinter.toHex(pc) + " is part of method @" +
                            HexPrinter.toHex(currentMethodBco) + " as well as " + HexPrinter.toHex(belongsToMethod[pc]));
                }

                int prevInstrOpcode = ArrayUtil.prevNonNullItem(pc, rawInstructions).opcode;
                if (opcode == OpCodes.JUMPDEST && prevInstrOpcode != OpCodes.JUMP) {
                    //log.println("[MD] linearly reached code that was already processed @" + toHex(pc));
                    return;
                }

                throw new IllegalStateException("Instruction @" + HexPrinter.toHex(pc) + " was already processed");
            }

			belongsToMethod[pc] = currentMethodBco;

			if (!OpCodes.isInvalid(opcode)) {
				// assumption: if a callee wants to read an argument it never swaps it ontop (only duplicates it)
				// (i.e. swap instruction doesn't count as a read access)
				int depth, lowestAccessedStackIndex;
				if ((depth = OpCodes.isDup(opcode)) != -1) {
					lowestAccessedStackIndex = operandStackSize - depth - 1;
				}
				else if (opcode != OpCodes.JUMP || !ControlFlowDetector.isJumpMethodReturn(pc, rawInstructions)) {
					// do not count stack access of the method return jump instruction
					lowestAccessedStackIndex = operandStackSize - OpCodes.getPopCount(opcode);
					// note: getPopCount() for a swap is always 0
				}
				else {
					lowestAccessedStackIndex = operandStackSize;
				}
				if (lowestAccessedStackIndex < stackAccessDepthForCallstackSize.get(callStack.size())) {
					stackAccessDepthForCallstackSize.put(callStack.size(), lowestAccessedStackIndex);
				}
			}

			if (!OpCodes.isInvalid(opcode)) {
				operandStackSize = operandStackSize - OpCodes.getPopCount(opcode) + OpCodes.getPushCount(opcode);
			}

			if (opcode == OpCodes.JUMP) {
				// check if this is a return jump
				final int pcf = pc;
				Collection<MethodInfo> methodCandidates = methods.values().stream().filter(methodInfo -> methodInfo.returns.contains(pcf)).collect(Collectors.toList());
				if (methodCandidates.size() > 1) {
					throw new AssumptionViolatedException("Jump @" + HexPrinter.toHex(pc) + " is a return instruction for multiple methods");
				}
				boolean isMethodReturn = methodCandidates.size() == 1;
				if (isMethodReturn) {
					//log.println("[MD]   method return @" + toHex(pc));
					// method return
					if (callStack.size() < 1) {
						throw new IllegalStateException("call stack underflow");
					}

					MethodInfo method = methodCandidates.iterator().next();
					if (currentMethodBco != method.getHead()) {
						throw new IllegalStateException("currently tracked method @" + HexPrinter.toHex(currentMethodBco) +
								" is not equal to the reconstructed method @" + HexPrinter.toHex(method.getHead()));
					}

					// determine method argument variables depending on stack read access
					int lowestStackReadAccess = stackAccessDepthForCallstackSize.get(callStack.size());
					int stackSizeAtMethodCall = variableStackSizeAtBeginOfMethod.get(currentMethodBco);
					int methodArgumentCount = stackSizeAtMethodCall - lowestStackReadAccess;
					argumentCountForMethod.put(currentMethodBco, methodArgumentCount);

					// determine return variables depending on stack state relative to the method entry
					// var count = difference between current stack size and the original stack size minus the argument count
					int methodReturnVarsCount = operandStackSize - stackSizeAtMethodCall + methodArgumentCount + 1;
					returnVarCountForMethod.put(currentMethodBco, methodReturnVarsCount);

					// save method-entry-return assocs
					returnJumpBcoForMethod.put(currentMethodBco, pc);
					methodBcoForReturnJump.put(pc, currentMethodBco);

					int callSrc = callStack.peek().callSrc;
					callStack.pop();

					// end of method
					int returnAddress = ArrayUtil.nextNonNullIndex(callSrc, rawInstructions);
					//log.println("[MD]   returning to @" + toHex(returnAddress));
					if (belongsToMethod[returnAddress] == -1) {
						// continue on callee branch only
						detect(returnAddress, operandStackSize, StackUtil.copyStack(callStack));
					}
					else {
						//log.println("[MD]   already visited @" + toHex(returnAddress));
					}
					return;
				}

				if (jumps.get(pc).size() != 1) {
					// only allow multiple targets for a method return
					throw new AssumptionViolatedException("Non-return jump @" + HexPrinter.toHex(pc) +
							" has multiple possible targets: @" + HexPrinter.toHex(jumps.get(pc), ", @"));
				}

				int jumpdest = jumps.get(pc).iterator().next();
				if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
					boolean isMethodCall = methods.containsKey(jumpdest);
					if (belongsToMethod[jumpdest] != -1 && belongsToMethod[jumpdest] != currentMethodBco && !isMethodCall) {
						// should not be
						throw new AssumptionViolatedException("Jump from @" + HexPrinter.toHex(pc) +
								" in method " + HexPrinter.toHex(currentMethodBco) + " to @" + HexPrinter.toHex(jumpdest) +
								" which was determined as being part of another method @" + HexPrinter.toHex(belongsToMethod[jumpdest]));
					}
					else if (belongsToMethod[jumpdest] == -1) {
						// target code not processed
						if (isMethodCall) {
							//log.println("[MD]   method call to @" + toHex(jumpdest));
							// this is a method call
							callStack.push(new MethodCall(pc, jumpdest));
						}
						detect(jumpdest, operandStackSize, StackUtil.copyStack(callStack));
					}
					else if (isMethodCall) {
						// target method already processed, continue on local branch
						if (argumentCountForMethod.containsKey(jumpdest) && returnVarCountForMethod.containsKey(jumpdest)) {
							// apply method's stack effect to current operand stack
							operandStackSize += returnVarCountForMethod.get(jumpdest) - argumentCountForMethod.get(jumpdest) - 1;
						}
						else {
							// would apply method's stack effect; but target method is not yet decompiled (e.g. in case of recursion)
							throw new IllegalStateException("requesting effect of partially processed method (endless recursion?)");
						}
						continue;
					}
				}
				// end of current branch
				return;
			}
			else if (opcode == OpCodes.JUMPI) {
				if (jumps.get(pc).size() != 1) {
					throw new AssumptionViolatedException("contitional jump @" + HexPrinter.toHex(pc) + " has multiple jump targets");
				}
				int jumpdest = jumps.get(pc).iterator().next();
				if (!ControlFlowDetector.isVirtualJumpDest(jumpdest)) {
					if (belongsToMethod[jumpdest] != -1 && belongsToMethod[jumpdest] != currentMethodBco) {
						throw new AssumptionViolatedException("Jump from @" + HexPrinter.toHex(pc) +
								" in method " + HexPrinter.toHex(currentMethodBco) + " to @" + HexPrinter.toHex(jumpdest) +
								" which was determined as being part of another method @" + HexPrinter.toHex(belongsToMethod[jumpdest]));
					}
					else if (belongsToMethod[jumpdest] == -1) {
						// decide which path leads faster to the exit (method return) and call that one first
						int linear = pc + 1;
						int branchToTakeFirst;
						if (currentMethodBco <= 0) {
							branchToTakeFirst = linear;
						}
						else {
							MethodInfo method = methods.get(currentMethodBco);
							branchToTakeFirst = cfg.getQuickestJumpTowardsExit(pc, method.returns, bco -> methods.containsKey(bco));
						}

						if (branchToTakeFirst == jumpdest) {
							// remote branch first
							detect(jumpdest, operandStackSize, StackUtil.copyStack(callStack));
							if (belongsToMethod[linear] == -1) {
								detect(branchStartOffset, linear, operandStackSize, StackUtil.copyStack(callStack));
							}
						}
						else {
							// local branch first
							detect(branchStartOffset, linear, operandStackSize, StackUtil.copyStack(callStack));
							if (belongsToMethod[jumpdest] == -1) {
								detect(jumpdest, operandStackSize, StackUtil.copyStack(callStack));
							}
						}
						return;
					}
				}
				// continue on current branch
				continue;
			}
			else if (OpCodes.endsExecution(opcode)) {
				return;
			}
		}
	}


	/**
	 * Get the number of argument Variables for a given method.
	 * @param methodBco method bytecode offset.
	 * @return number of argument Variables.
	 */
	public int getArgumentCountForMethod(int methodBco) {
		if (!methods.containsKey(methodBco))
			throw new IllegalArgumentException("Requesting argument count for non-method " + HexPrinter.toHex(methodBco));
		if (!argumentCountForMethod.containsKey(methodBco))
			throw new IllegalArgumentException("Requesting argument count for method " + HexPrinter.toHex(methodBco) +
					" that was not decompiled. (Possible endless recursion?)");
		return argumentCountForMethod.get(methodBco);
	}


	/**
	 * Get the number of return Variables for a given method.
	 * @param methodBco method bytecode offset.
	 * @return number of return Variables.
	 */
	public int getReturnVarCountForMethod(int methodBco) {
		if (!methods.containsKey(methodBco))
			throw new IllegalArgumentException("Requesting return variable count for non-method " + HexPrinter.toHex(methodBco));
		if (!returnVarCountForMethod.containsKey(methodBco))
			throw new IllegalArgumentException("Requesting return variable count for method " + HexPrinter.toHex(methodBco) +
					" that was not decompiled. (Possible endless recursion?)");
		return returnVarCountForMethod.get(methodBco);
	}


	/**
	 * Get the return jump(s) for a given method.
	 * @param methodBco method bytecode offset.
	 * @return list of return jumps (bytecode offsets).
	 */
	public Collection<Integer> getReturnJumpBcoForMethod(int methodBco) {
		return returnJumpBcoForMethod.get(methodBco);
	}


	/**
	 * Get the method for a given return jump.
	 * @param returnBco return jump bytecode offset.
	 * @return method bytecode offset.
	 */
	public int getMethodBcoForReturnJump(int returnBco) {
		return methodBcoForReturnJump.get(returnBco);
	}


	public Map<Integer, MethodInfo> getMethods() {
		return methods;
	}


	public static class MethodInfo {

		/** Begin of method (bytecode offset) */
		public final int head;
		/** Return jumps (bytecode offsets) */
		public final Set<Integer> returns;
		/** Method call sources (bytecode offsets) */
		public final Set<Integer> calls;
		/** Return jump destinations (bytecode offsets) */
		public final Set<Integer> returnDests;

		public MethodInfo(int head) {
			this.head = head;
			this.returns = new TreeSet<>();
			this.calls = new TreeSet<>();
			this.returnDests = new TreeSet<>();
		}

		public int getHead() {
			return head;
		}

		@Override
		public int hashCode() {
			return head;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof MethodInfo && ((MethodInfo)o).head == this.head;
		}
	}


}
