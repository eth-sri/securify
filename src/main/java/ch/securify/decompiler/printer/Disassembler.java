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


package ch.securify.decompiler.printer;

import ch.securify.decompiler.evm.OpCodes;
import ch.securify.decompiler.EvmParser;

import java.io.PrintStream;

public class Disassembler {


	/**
	 * Disassemble the bytecode and print the EVM instructions in a readable format.
	 * @param bytecode EVM bytecode.
	 */
	public static void disassemble(byte[] bytecode) {
		disassemble(bytecode, System.out);
	}


	/**
	 * Disassemble the bytecode and print the EVM instructions in a readable format.
	 * @param bytecode EVM bytecode.
	 * @param outputStream Stream to print the output to.
	 */
	public static void disassemble(byte[] bytecode, PrintStream outputStream) {
		outputStream.println("-- START");
		EvmParser.parse(bytecode, new EvmParser.OnOperationParsedCallback() {
			int tag = 1;
			public void onOperationParsed(int offset, int instrNumber, int opcode, byte[] payload) {
				if (opcode == OpCodes.JUMPDEST) {
					outputStream.println("-- tag " + tag);
					tag++;
				}

				outputStream.print(String.format("%02X: ", offset));

				outputStream.print(String.format("%02X ", opcode));

				String opName = OpCodes.getOpName(opcode);
				if (opName != null) {
					outputStream.print(opName);

					if (OpCodes.isPush(opcode) > -1) {
						outputStream.print(" 0x");
						for (byte data : payload) {
							int val = data & 0xFF;
							outputStream.print(String.format("%02X", val));
						}
					}
					outputStream.println();
				}
				else {
					outputStream.println("unknown operation " + String.format("%02X", opcode));
				}
			}
		});
		outputStream.println("-- EOF");
	}


}
