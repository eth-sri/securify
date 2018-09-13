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


package ch.securify.decompiler.evm;

import ch.securify.decompiler.InstructionFactory;

/**
 * Wrapper for an EVM bytecode instruction.
 */
public class RawInstruction {

	public final int opcode;
	public final byte[] data;
	public final int offset;
	public final int instrNumber;

	/**
	 * @param opcode op code.
	 * @param data payload data (used for push operations).
	 * @param offset bytecode position.
	 * @param instrNumber instruction index.
	 */
	public RawInstruction(int opcode, byte[] data, int offset, int instrNumber) {
		this.opcode = opcode;
		this.data = data;
		this.offset = offset;
		this.instrNumber = instrNumber;
	}

	@Override
	public String toString() {
		return "" + this.instrNumber + "(" + this.offset + "): " + InstructionFactory.returnRawName(this);
	}

}
