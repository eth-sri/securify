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

import ch.securify.decompiler.instructions._TypeInstruction;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.printer.HexPrinter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class Variable {

	private static boolean debug = true;

	public static void setDebug(boolean debug) {
		Variable.debug = debug;
	}

	private static int nextVarId;
	static { resetVarNameGenerator(); }

	private static synchronized String generateVarName() {
		StringBuilder sb = new StringBuilder();
		int varId = nextVarId;
		do {
			char letter = (char) ('a' + (varId % 26));
			sb.append(letter);
			varId /= 26;
		} while (varId > 0);
		nextVarId++;
		return sb.reverse().toString();
	}

	/**
	 * Reset the naming of Variables to start again from 'a'.
	 */
	public static void resetVarNameGenerator() {
		nextVarId = 0;
	}

	/**
	 * Create `count` new variables.
	 * @param count number of variables to create.
	 * @return array of new variables.
	 */
	public static Variable[] createNewVariables(int count) {
		Variable[] variables = new Variable[count];
		for (int i = 0; i < variables.length; i++) {
			variables[i] = new Variable();
		}
		return variables;
	}

	/**
	 * Remove `count` variables from the stack and return them.
	 * @param stack stack to pop variables from.
	 * @param count number of variables to pop from stack.
	 * @return array of popped variables (first Variable corresponds to the topmost stack item).
	 */
	public static Variable[] takeFromStack(Stack<Variable> stack, int count) {
		Variable[] variables = new Variable[count];
		for (int i = 0; i < variables.length; i++) {
			variables[i] = stack.pop();
		}
		return variables;
	}

	/**
	 * Get `count` variables from the stack.
	 * @param stack stack to peek variables from.
	 * @param count number of variables to peek from stack.
	 * @return array of variables (first Variable corresponds to the topmost stack item).
	 */
	public static Variable[] peekFromStack(Stack<Variable> stack, int count) {
		int topIndex = stack.size() - 1;
		Variable[] variables = new Variable[count];
		for (int i = 0; i < variables.length; i++) {
			variables[i] = stack.get(topIndex - i);
		}
		return variables;
	}


	private String name;

	public static final Class<Any> TYPE_ANY = Any.class;
	private Set<Class<? extends Instruction>> valueTypes = new HashSet<>();

	public static final byte[] VALUE_ANY = new byte[0];
	public static final byte[] VALUE_UNDEFINED = new byte[0];

	private byte[] constantValue = VALUE_UNDEFINED;
	private Set<byte[]> hashConstants = new HashSet<>();

	public Variable() {
		name = generateVarName();
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		if (debug) {
			return getName() +
					"{" + valueTypes.stream().map(Class::getSimpleName).collect(Collectors.joining("|")) + "}" +
					(hasConstantValue() ? ("{0x" + HexPrinter.toHex(getConstantValue()) + "}") : "{?}");
		}
		else {
			return getName();
		}
	}

	/**
	 * Get the constant value of this variable.
	 * @return value, VALUE_UNDEFINED if never set or VALUE_ANY if everything is possible.
	 */
	public byte[] getConstantValue() {
		return constantValue;
	}

	/**
	 * Whether this variable has an explicit constant value.
	 * @return true if there is a value, false if the value has not been set or is unknown.
	 */
	public boolean hasConstantValue() {
		return constantValue.length > 0;
	}

	public void setConstantValue(byte[] value) {
		constantValue = value;
	}

	public void addValueType(Class<? extends Instruction> type) {
		valueTypes.add(type);
	}

	public void addValueTypes(Collection<Class<? extends Instruction>> types) {
		types.forEach(this::addValueType);
	}

	public void addHashConstant(byte[] hashConstant) {
		hashConstants.add(hashConstant);
	}
	public Set<byte[]> getHashConstants() {
		return hashConstants;
	}

	public Set<Class<? extends Instruction>> getValueTypes() {
		return valueTypes;
	}


	private static class Any extends Instruction implements _TypeInstruction { }

}
