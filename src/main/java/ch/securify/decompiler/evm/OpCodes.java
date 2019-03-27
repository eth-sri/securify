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

import ch.securify.decompiler.printer.HexPrinter;
import ch.securify.utils.ReflectionUtil;

public class OpCodes {

	// Arithmetic Operations and Stop
	public static final int STOP = 0x00;
	public static final int ADD = 0x01;
	public static final int MUL = 0x02;
	public static final int SUB = 0x03;
	public static final int DIV = 0x04;
	public static final int SDIV = 0x05;
	public static final int MOD = 0x06;
	public static final int SMOD = 0x07;
	public static final int ADDMOD = 0x08;
	public static final int MULMOD = 0x09;
	public static final int EXP = 0x0a;
	public static final int SIGNEXTEND = 0x0b;

	// Comparison & Bitwise Logic Operations
	public static final int LT = 0x10;
	public static final int GT = 0x11;
	public static final int SLT = 0x12;
	public static final int SGT = 0x13;
	public static final int EQ = 0x14;
	public static final int ISZERO = 0x15;
	public static final int AND = 0x16;
	public static final int OR = 0x17;
	public static final int XOR = 0x18;
	public static final int NOT = 0x19;
	public static final int BYTE = 0x1a;
	public static final int SHL = 0x1b;
	public static final int SHR = 0x1c;
	public static final int SAR = 0x1d;

	// SHA3
	public static final int SHA3 = 0x20;

	// Environmental Information
	public static final int ADDRESS = 0x30;
	public static final int BALANCE = 0x31;
	public static final int ORIGIN = 0x32;
	public static final int CALLER = 0x33;
	public static final int CALLVALUE = 0x34;
	public static final int CALLDATALOAD = 0x35;
	public static final int CALLDATASIZE = 0x36;
	public static final int CALLDATACOPY = 0x37;
	public static final int CODESIZE = 0x38;
	public static final int CODECOPY = 0x39;
	public static final int GASPRICE = 0x3a;
	public static final int EXTCODESIZE = 0x3b;
	public static final int EXTCODECOPY = 0x3c;
	public static final int RETURNDATASIZE = 0x3d;
	public static final int RETURNDATACOPY = 0x3e;
	public static final int EXTCODEHASH = 0x3f;

	// Block Information
	public static final int BLOCKHASH = 0x40;
	public static final int COINBASE = 0x41;
	public static final int TIMESTAMP = 0x42;
	public static final int NUMBER = 0x43;
	public static final int DIFFICULTY = 0x44;
	public static final int GASLIMIT = 0x45;

	// Stack, Memory, Storage and Flow Operations
	public static final int POP = 0x50;
	public static final int MLOAD = 0x51;
	public static final int MSTORE = 0x52;
	public static final int MSTORES = 0x53;
	public static final int SLOAD = 0x54;
	public static final int SSTORE = 0x55;
	public static final int JUMP = 0x56;
	public static final int JUMPI = 0x57;
	public static final int PC = 0x58;
	public static final int MSIZE = 0x59;
	public static final int GAS = 0x5a;
	public static final int JUMPDEST = 0x5b;

	/**
	 * PUSH opcodes.
	 * @param bytes [1..32]
	 * @return
	 */
	public static int PUSH(int bytes) {
		return 0x60 + bytes - 1;
	}

	/**
	 * Check if opcode is a push instruction.
	 * @param opcode
	 * @return number of bytes to push, -1 if opcode is not a push instruction.
	 */
	public static int isPush(int opcode) {
		if (0x60 <= opcode && opcode <= 0x7f)
			return opcode - 0x60 + 1;
		else
			return -1;
	}

	/**
	 * DUP opcodes.
	 * @param index [1..16]
	 * @return
	 */
	public static int DUP(int index) {
		return 0x80 + index - 1;
	}

	/**
	 * Check if opcode is a duplication instruction.
	 * @param opcode
	 * @return stack index to duplicate [0..15], -1 if opcode is not a duplication instruction.
	 */
	public static int isDup(int opcode) {
		if (0x80 <= opcode && opcode <= 0x8f)
			return opcode - 0x80;
		else
			return -1;
	}

	/**
	 * SWAP opcodes.
	 * @param index [1..16]
	 * @return
	 */
	public static int SWAP(int index) {
		return 0x80 + index - 1;
	}

	/**
	 * Check if opcode is a swap instruction.
	 * @param opcode
	 * @return stack index to swap with [1..16], -1 if opcode is not a swap instruction.
	 */
	public static int isSwap(int opcode) {
		if (0x90 <= opcode && opcode <= 0x9f)
			return opcode - 0x90 + 1;
		else
			return -1;
	}

	// Logging Operations
	public static final int LOG0 = 0xa0;
	public static final int LOG1 = 0xa1;
	public static final int LOG2 = 0xa2;
	public static final int LOG3 = 0xa3;
	public static final int LOG4 = 0xa4;

	// System operations
	public static final int CREATE = 0xf0;
	public static final int CALL = 0xf1;
	public static final int CALLCODE = 0xf2;
	public static final int RETURN = 0xf3;
	public static final int DELEGATECALL = 0xf4;
	public static final int CREATE2 = 0xf5;
	public static final int STATICCALL = 0xfa;
	public static final int REVERT = 0xfd;
	public static final int INVALID = 0xfe;
	public static final int SELFDESTRUCT = 0xff;


	/**
	 * Get the name of the operation.
	 * @param opcode
	 * @return operation name, null if unknown opcode
	 */
	public static String getOpName(int opcode) {
		String opname = ReflectionUtil.getConstantNameByValue(OpCodes.class, opcode);
		if (opname != null) {
			return opname;
		}
		else {
			int skip;
			if ((skip = OpCodes.isPush(opcode)) > -1) {
				return "PUSH" + skip;
			}
			else if ((skip = OpCodes.isDup(opcode)) > -1) {
				return "DUP" + (skip + 1);
			}
			else if ((skip = OpCodes.isSwap(opcode)) > -1) {
				return "SWAP" + skip;
			}
		}
		return "INVALID";
	}


	/**
	 *
	 * @param opcode
	 * @return
	 */
	public static boolean isInvalid(int opcode) {
		return "INVALID".equals(getOpName(opcode)) || opcode == INVALID;
	}

	/**
	 * Indicate whether the opcode ends the execution
	 * @param opcode
	 * @return whether the opcode belongs to the corresponding set
	 */
	public static boolean endsExecution(int opcode) {
		return isInvalid(opcode) ||
				opcode == OpCodes.REVERT ||
				opcode == OpCodes.SELFDESTRUCT ||
				opcode == OpCodes.STOP ||
                                opcode == OpCodes.RETURN;
	}

	/**
	 * Get an invalid opcode.
	 * @return
	 */
	public static int getInvalid() {
		return 0xfe;
	}


	/**
	 * Check how many stack items are popped by the given instruction.
	 * @param opcode instruction opcode.
	 * @return number of popped items.
	 */
	public static int getPopCount(int opcode) {
		switch (opcode) {
			case STOP: return 0;
			case ADD: return 2;
			case MUL: return 2;
			case SUB: return 2;
			case DIV: return 2;
			case SDIV: return 2;
			case MOD: return 2;
			case SMOD: return 2;
			case ADDMOD: return 3;
			case MULMOD: return 3;
			case EXP: return 2;
			case SIGNEXTEND: return 2;
			case LT: return 2;
			case GT: return 2;
			case SLT: return 2;
			case SGT: return 2;
			case EQ: return 2;
			case ISZERO: return 1;
			case AND: return 2;
			case OR: return 2;
			case XOR: return 2;
			case NOT: return 1;
			case BYTE: return 2;
			case SHL: return 2;
			case SHR: return 2;
			case SAR: return 2;
			case SHA3: return 2;
			case ADDRESS: return 0;
			case BALANCE: return 1;
			case ORIGIN: return 0;
			case CALLER: return 0;
			case CALLVALUE: return 0;
			case CALLDATALOAD: return 1;
			case CALLDATASIZE: return 0;
			case CALLDATACOPY: return 3;
			case CODESIZE: return 0;
			case CODECOPY: return 3;
			case GASPRICE: return 0;
			case EXTCODESIZE: return 1;
			case EXTCODECOPY: return 4;
			case RETURNDATASIZE: return 0;
			case RETURNDATACOPY: return 3;
			case EXTCODEHASH: return 1;
			case BLOCKHASH: return 1;
			case COINBASE: return 0;
			case TIMESTAMP: return 0;
			case NUMBER: return 0;
			case DIFFICULTY: return 0;
			case GASLIMIT: return 0;
			case POP: return 1;
			case MLOAD: return 1;
			case MSTORE: return 2;
			case MSTORES: return 2;
			case SLOAD: return 1;
			case SSTORE: return 2;
			case JUMP: return 1;
			case JUMPI: return 2;
			case PC: return 0;
			case MSIZE: return 0;
			case GAS: return 0;
			case JUMPDEST: return 0;
			case LOG0: return 2;
			case LOG1: return 3;
			case LOG2: return 4;
			case LOG3: return 5;
			case LOG4: return 6;
			case CREATE: return 3;
			case CREATE2: return 4;
			case CALL: return 7;
			case CALLCODE: return 7;
			case RETURN: return 2;
			case DELEGATECALL: return 6;
			case STATICCALL: return 6;
			case REVERT: return 2;
			// undefined
			case INVALID: throw new AssertionError();
			case SELFDESTRUCT: return 1;
		}
		if (isPush(opcode) > -1) {
			return 0;
		}
		else if (isSwap(opcode) > -1) {
			return 0;
		}
		else if (isDup(opcode) > -1) {
			return 0;
		}
		throw new IllegalArgumentException("unknown opcode: " + HexPrinter.toHex(opcode));
	}


	/**
	 * Check how many items are pushed onto the stack by the given instruction.
	 * @param opcode instruction opcode.
	 * @return number of pushed items.
	 */
	public static int getPushCount(int opcode) {
		switch (opcode) {
			case STOP: return 0;
			case ADD: return 1;
			case MUL: return 1;
			case SUB: return 1;
			case DIV: return 1;
			case SDIV: return 1;
			case MOD: return 1;
			case SMOD: return 1;
			case ADDMOD: return 1;
			case MULMOD: return 1;
			case EXP: return 1;
			case SIGNEXTEND: return 1;
			case LT: return 1;
			case GT: return 1;
			case SLT: return 1;
			case SGT: return 1;
			case EQ: return 1;
			case ISZERO: return 1;
			case AND: return 1;
			case OR: return 1;
			case XOR: return 1;
			case NOT: return 1;
			case BYTE: return 1;
			case SHL: return 1;
			case SHR: return 1;
			case SAR: return 1;
			case SHA3: return 1;
			case ADDRESS: return 1;
			case BALANCE: return 1;
			case ORIGIN: return 1;
			case CALLER: return 1;
			case CALLVALUE: return 1;
			case CALLDATALOAD: return 1;
			case CALLDATASIZE: return 1;
			case CALLDATACOPY: return 0;
			case CODESIZE: return 1;
			case CODECOPY: return 0;
			case GASPRICE: return 1;
			case EXTCODESIZE: return 1;
			case EXTCODECOPY: return 0;
			case RETURNDATASIZE: return 1;
			case RETURNDATACOPY: return 0;
			case EXTCODEHASH: return 1;
			case BLOCKHASH: return 1;
			case COINBASE: return 1;
			case TIMESTAMP: return 1;
			case NUMBER: return 1;
			case DIFFICULTY: return 1;
			case GASLIMIT: return 1;
			case POP: return 0;
			case MLOAD: return 1;
			case MSTORE: return 0;
			case MSTORES: return 0;
			case SLOAD: return 1;
			case SSTORE: return 0;
			case JUMP: return 0;
			case JUMPI: return 0;
			case PC: return 1;
			case MSIZE: return 1;
			case GAS: return 1;
			case JUMPDEST: return 0;
			case LOG0: return 0;
			case LOG1: return 0;
			case LOG2: return 0;
			case LOG3: return 0;
			case LOG4: return 0;
			case CREATE: return 1;
			case CREATE2: return 1;
			case CALL: return 1;
			case CALLCODE: return 1;
			case RETURN: return 0;
			case DELEGATECALL: return 1;
			case STATICCALL: return 1;
			case REVERT: return 0;
			// undefined
			case INVALID: throw new AssertionError();
			case SELFDESTRUCT: return 0;
		}
		if (isPush(opcode) > -1) {
			return 1;
		}
		else if (isSwap(opcode) > -1) {
			return 0;
		}
		else if (isDup(opcode) > -1) {
			return 1;
		}
		throw new IllegalArgumentException("unknown opcode: " + HexPrinter.toHex(opcode));
	}


	/**
	 * The the instruction length in bytes.
	 * @param opcode instruction opcode.
	 * @return number of bytes this instruction has.
	 */
	public static int getSize(int opcode) {
		int size;
		if ((size = isPush(opcode)) > -1) {
			return 1 + size;
		}
		else {
			return 1;
		}
	}

}
