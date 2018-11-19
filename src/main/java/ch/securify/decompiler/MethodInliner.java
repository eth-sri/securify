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

import ch.securify.decompiler.instructions.BranchInstruction;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.Jump;
import ch.securify.decompiler.instructions.JumpDest;
import ch.securify.decompiler.instructions._VirtualAssignment;
import ch.securify.decompiler.instructions._VirtualInstruction;
import ch.securify.decompiler.instructions._VirtualMethodHead;
import ch.securify.decompiler.instructions._VirtualMethodInvoke;
import ch.securify.decompiler.instructions._VirtualMethodReturn;
import ch.securify.utils.StackUtil;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

public class MethodInliner {


	private Map<_VirtualMethodHead, List<Instruction>> methods;

	private final AtomicInteger exitpointId = new AtomicInteger(1);

	private final PrintStream log;


	private MethodInliner(PrintStream log) {
		this.log = log;
	}


	public static List<Instruction> inline(List<Instruction> instructions, PrintStream log) {
		return new MethodInliner(log).process(instructions);
	}


	private List<Instruction> process(List<Instruction> instructions) {
		if (instructions.stream().noneMatch(instruction -> instruction instanceof _VirtualMethodHead)) {
			// no methods found, so nothing to be inlined
			log.println("[MINL] no methods found");
			return instructions;
		}

		methods = new HashMap<>();

		int i = 0;
		// skip to first method
		while (!(instructions.get(i) instanceof _VirtualMethodHead)) {
			i++;
		}
		List<Instruction> initInstructions = instructions.subList(0, i);
		while (i < instructions.size()) {
			List<Instruction> methodBody = new ArrayList<>();

			_VirtualMethodHead methodHead = (_VirtualMethodHead) instructions.get(i);
			methodBody.add(methodHead);
			i++;

			while (i < instructions.size() && !(instructions.get(i) instanceof _VirtualMethodHead)) {
				methodBody.add(instructions.get(i));
				i++;
			}

			methods.put(methodHead, methodBody);
		}

		log.println("[MINL] found " + methods.size() + " methods");

		// TODO: may need to copy all methods because later processed methods may use methods that have already inlined stuff
		// (should not be a problem, worst case just generates more code, e.g. inlines recursive method once)

		methods.forEach((methodHead, methodBody) -> processMethod(methodBody, StackUtil.create(methodHead)));

		// stitch new code together
		List<Instruction> inlinedProgram = new ArrayList<>(initInstructions);
		methods.forEach((methodHead, methodBody) -> inlinedProgram.addAll(methodBody));

		return inlinedProgram;
	}


	/**
	 * Scan method for method calls to inline.
	 * @param methodBody
	 * @param callstack
	 */
	private void processMethod(List<Instruction> methodBody, Stack<_VirtualMethodHead> callstack) {
		log.println("[MINL] processing method: " + callstack.peek().getLabel());

		// search for method invocations to inline them
		for (int i = 0; i < methodBody.size(); ++i) {
			Instruction instruction = methodBody.get(i);
			if (!(instruction instanceof _VirtualMethodInvoke)) {
				continue;
			}

			_VirtualMethodInvoke methodCall = (_VirtualMethodInvoke) instruction;

			_VirtualMethodHead invokedMethod = (_VirtualMethodHead) methodCall.getOutgoingBranches().iterator().next();
			List<Instruction> inlinedMethodBody = getInlinedMethodBody(methodCall, StackUtil.copyStack(callstack));
			if (inlinedMethodBody == null) {
				// recursive call, could not inline method
			}
			else {
				// replace method invocation instruction with inlined method body instructions
				Instruction prev = instruction.getPrev();
				Instruction next = instruction.getNext();

				methodBody.remove(i);
				methodBody.addAll(i, inlinedMethodBody);
				if (inlinedMethodBody.size() > 1) {
					inlinedMethodBody.get(0).setComment("start of inlined method " + invokedMethod.getLabel());
					inlinedMethodBody.get(inlinedMethodBody.size() - 1)
							.setComment("end of inlined method " + invokedMethod.getLabel());
				}
				else {
					inlinedMethodBody.get(0).setComment("inlined method " + invokedMethod.getLabel());
				}

				prev.setNext(inlinedMethodBody.get(0));
				inlinedMethodBody.get(0).setPrev(prev);

				next.setPrev(inlinedMethodBody.get(inlinedMethodBody.size() - 1));
				inlinedMethodBody.get(inlinedMethodBody.size() - 1).setNext(next);

				i += inlinedMethodBody.size() - 1; // skip inlined method
			}
		}
	}


	/**
	 * Copy a method body and prepare it to be inlined.
	 * @param methodCall
	 * @param callstack
	 * @return
	 */
	private List<Instruction> getInlinedMethodBody(_VirtualMethodInvoke methodCall, Stack<_VirtualMethodHead> callstack) {
		_VirtualMethodHead invokedMethod = (_VirtualMethodHead) methodCall.getOutgoingBranches().iterator().next();
		log.println("[MINL] inlining method " + invokedMethod.getLabel());
		if (callstack.contains(invokedMethod)) {
			log.println("[MINL] cannot inline recursive method " + invokedMethod.getLabel());
			return null;
		}
		callstack.push(invokedMethod);

		List<Instruction> methodBody = methods.get(invokedMethod);
		List<Instruction> copiedMethodBody = new ArrayList<>();

		Map<Instruction, Instruction> copyMap = new HashMap<>();
		Map<Variable, Variable> variableTranslation = new HashMap<>();
		for (int i = 0; i < invokedMethod.getOutput().length; ++i) {
			variableTranslation.put(invokedMethod.getOutput()[i], methodCall.getInput()[i]);
		}

		// check if this method has only one method return that is at the end
		Collection<_VirtualMethodReturn> returnInstructions = invokedMethod.getReturnInstructions();
		boolean simpleReturn = returnInstructions.size() == 1 &&
				returnInstructions.iterator().next() == methodBody.get(methodBody.size() - 1);

		if (simpleReturn) {
			_VirtualMethodReturn methodReturn = returnInstructions.iterator().next();
			for (int i = 0; i < methodReturn.getInput().length; ++i) {
				variableTranslation.put(methodReturn.getInput()[i], methodCall.getOutput()[i]);
			}
		}

		// copy instructions
		methodBody.forEach(instruction -> {
			Instruction copiedInstruction = instruction.clone();
			copiedInstruction.setInput(translateVars(variableTranslation, instruction.getInput()));
			copiedInstruction.setOutput(translateVars(variableTranslation, instruction.getOutput()));
			copiedMethodBody.add(copiedInstruction);

			copyMap.put(instruction, copiedInstruction);
		});

		// reassigments for return statements
		Map<_VirtualMethodReturn, List<Instruction>> returnReassignments = new HashMap<>();

		// link copied instructions
		copiedMethodBody.forEach(instruction -> {
			instruction.setPrev(copyMap.get(instruction.getPrev()));
			instruction.setNext(copyMap.get(instruction.getNext()));

			if (instruction instanceof BranchInstruction && !(instruction instanceof _VirtualInstruction)) {
				BranchInstruction bInstruction = (BranchInstruction) instruction;
				// relink incoming branches
				Collection<Instruction> incomingBranches = new ArrayList<>(bInstruction.getIncomingBranches());
				bInstruction.clearIncomingBranches();
				incomingBranches.forEach(incomingBranch -> bInstruction.addIncomingBranch(copyMap.get(incomingBranch)));
				// relink outgoing branches
				Collection<Instruction> outgoingBranches = new ArrayList<>(bInstruction.getOutgoingBranches());
				bInstruction.clearOutgoingBranches();
				outgoingBranches.forEach(outgoingBranch -> bInstruction.addOutgoingBranch(copyMap.get(outgoingBranch)));
			}
			else if (instruction instanceof _VirtualMethodReturn) {
				// create variable reassignments
				Variable[] targetVars = methodCall.getOutput();
				Variable[] returnVars = instruction.getInput();
				List<Instruction> reassignments = new ArrayList<>();
				for (int i = 0; i < targetVars.length; ++i) {
					if (targetVars[i] != returnVars[i]) {
						reassignments.add(new _VirtualAssignment(targetVars[i], returnVars[i]));
					}
				}
				returnReassignments.put((_VirtualMethodReturn) instruction, reassignments);
			}
		});

		if (simpleReturn) {
			// handle simple method returns
			_VirtualMethodReturn methodReturn = (_VirtualMethodReturn) copiedMethodBody.get(copiedMethodBody.size() - 1);
			List<Instruction> reassignments = returnReassignments.get(methodReturn);

			Instruction prev = methodReturn.getPrev();
			for (Instruction curr : reassignments) {
				curr.setPrev(prev);
				prev.setNext(curr);
				prev = curr;
			}

			// replace return with reassignments
			copiedMethodBody.remove(copiedMethodBody.size() - 1);
			copiedMethodBody.addAll(reassignments);
		}
		else {
			// create method exit point
			JumpDest exitPoint = new JumpDest("end_of_" + invokedMethod.getLabel() + "__" + exitpointId.getAndIncrement());
			exitPoint.setInput(Instruction.NO_VARIABLES);
			exitPoint.setOutput(Instruction.NO_VARIABLES);

			// handle method returns
			returnReassignments.forEach((methodReturn, reassignments) -> {
				// link reassignment instructions
				Instruction prev = methodReturn.getPrev();
				for (Instruction curr : reassignments) {
					curr.setPrev(prev);
					prev.setNext(curr);
					prev = curr;
				}

				// create return jump
				Jump exitJump = new Jump(exitPoint.getLabel());
				exitJump.setInput(Instruction.NO_VARIABLES);
				exitJump.setOutput(Instruction.NO_VARIABLES);

				exitJump.setPrev(prev);
				prev.setNext(exitJump);

				exitJump.addOutgoingBranch(exitPoint);
				exitPoint.addIncomingBranch(exitJump);

				// insert reassignments & exit jump into method body
				int returnPos = copiedMethodBody.indexOf(methodReturn);
				// replace return with exit jump
				copiedMethodBody.set(returnPos, exitJump);
				// insert reassignments ahead of return
				copiedMethodBody.addAll(returnPos, reassignments);
			});

			// append method exit point
			copiedMethodBody.add(exitPoint);
		}
		// remove method head
		copiedMethodBody.remove(0);

		// check inlined method for further nested inlining
		processMethod(copiedMethodBody, callstack);

		return copiedMethodBody;
	}


	/**
	 * Map original variables to new variables, using the given variable translation if known, creates a new one if not.
	 * @param variableMap known variable translations.
	 * @param variables original variables to be mapped.
	 * @return mapped variables.
	 */
	private static Variable[] translateVars(Map<Variable, Variable> variableMap, Variable[] variables) {
		Variable[] translated = new Variable[variables.length];
		for (int i = 0; i < variables.length; i++) {
			Variable variable = variables[i];
			if (!variableMap.containsKey(variable)) {
				variableMap.put(variable, new Variable());
			}
			translated[i] = variableMap.get(variable);
		}
		return translated;
	}


}
