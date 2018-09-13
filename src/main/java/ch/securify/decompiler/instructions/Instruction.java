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
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.printer.HexPrinter;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class Instruction implements Cloneable {


	public static final Variable[] NO_VARIABLES = new Variable[0];

	private RawInstruction rawInstruction;

	private Variable[] input, output;

	private Set<Variable> memInput = new HashSet<>();

	private Set<Instruction> dependsOn = new HashSet<>();

	private Instruction prev;
	private Instruction next;

	private String comment;


	/**
	 * Set the raw instruction on which this Instruction instance is based on.
	 * @param rawInstruction
	 * @return this Instruction instance.
	 */
	public Instruction setRawInstruction(RawInstruction rawInstruction) {
		this.rawInstruction = rawInstruction;
		return this;
	}


	public RawInstruction getRawInstruction() {
		return rawInstruction;
	}


	/**
	 * Set the input variables from stack. First variable is the top most on the stack.
	 * @param input Variables
	 * @return this Instruction instance.
	 */
	public Instruction setInput(Variable... input) {
		this.input = input;
		return this;
	}


	/**
	 * Set the output variables to stack. First variable is the top most on the stack.
	 * @param output Variables
	 * @return this Instruction instance.
	 */
	public Instruction setOutput(Variable... output) {
		this.output = output;
		return this;
	}


	public Variable[] getInput() {
		return input;
	}


	public Variable[] getOutput() {
		return output;
	}


	/**
	 * Get a human-readable format of this instruction.
	 * @return e.g. "a = b + c"
	 */
	public String getStringRepresentation() {
		StringBuilder sb = new StringBuilder();

		String sep = "";
		for (Variable var : output) {
			sb.append(sep);
			sb.append(var.getName());
			sep = ", ";
		}

		sb.append(" <- ");

		sep = "";
		for (Variable var : input) {
			sb.append(sep);
			sb.append(var.getName());
			sep = ", ";
		}

		return sb.toString();
	}


	/**
	 * Get a verbose human-readable format of this instruction.
	 * @return e.g. "D8: a = b + c  // comment"
	 */
	public String getDebugRepresentation() {
		StringBuilder sb = new StringBuilder();
		if (getRawInstruction() != null) {
			sb.append(HexPrinter.toHex(getRawInstruction().offset)).append(":  \t");
		}
		else {
			sb.append("...  \t");
		}
		sb.append(getStringRepresentation());
		if (getMemoryInputs().size() > 0) {
			sb.append(" [");
			String sep = "";
			for (Variable variable : getMemoryInputs()) {
				sb.append(sep).append(variable);
				sep = ", ";
			}
			sb.append("]");
		}
		if (getComment() != null) {
			sb.append("\t // ").append(getComment());
		}
		return sb.toString();
	}


	/**
	 * Add an Instruction whose result this Instruction depends on.
	 * @param instruction
	 */
	public void addDependency(Instruction instruction) {
		dependsOn.add(instruction);
	}


	/**
	 * Get all Instructions whose results this Instruction depends on.
	 * @return
	 */
	public Set<Instruction> getDependencies() {
		return dependsOn;
	}


	/**
	 * Set a custom comment for this Instruction.
	 * @param comment arbitrary single-line text.
	 * @return this Instruction instance.
	 */
	public Instruction setComment(String comment) {
		this.comment = comment;
		return this;
	}


	public String getComment() {
		return comment;
	}


	public Instruction getPrev() {
		return prev;
	}


	public void setPrev(Instruction prev) {
		this.prev = prev;
	}


	public Instruction getNext() {
		return next;
	}


	public void setNext(Instruction next) {
		this.next = next;
	}


	public void addMemoryInput(Variable inputVar) {
		memInput.add(inputVar);
	}


	public Collection<Variable> getMemoryInputs() {
		return memInput;
	}


	/**
	 * Compute the symbolic values of the result variables.
	 */
	public void computeResultValues() {
		for (Variable variable : getOutput()) {
			variable.setConstantValue(Variable.VALUE_ANY);
		}
	}


	public void computeResultTypes() {
		for (Variable input : getInput()) {
			if (input == null)
				continue;
			for (Class<? extends Instruction> inputType : input.getValueTypes()) {
				for (Variable output : getOutput()) {
					output.addValueType(inputType);
				}
			}
		}
		if (this instanceof _TypeInstruction) {
			for (Variable output : getOutput()) {
				output.addValueType(getClass());
			}
		}

		if (this instanceof _ReturnsAddress) {
		    for (Variable output : getOutput()) {
                output.addValueType(_AddressType.class);
            }
		}
	}


	@Override
	public Instruction clone() {
		try {
			return (Instruction) super.clone();
		}
		catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}


	@Override
	public String toString() {
		return getStringRepresentation();
	}

}
