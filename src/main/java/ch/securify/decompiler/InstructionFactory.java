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
import ch.securify.utils.Resolver;
import ch.securify.decompiler.instructions.Byte;
import ch.securify.decompiler.evm.RawInstruction;
import ch.securify.decompiler.instructions.*;
import java.util.Stack;

public class InstructionFactory {


	private Resolver<RawInstruction, String> jumpResolver;
	private Resolver<RawInstruction, String> labelResolver;


	/**
	 * Create an Instruction factory.
	 */
	public InstructionFactory() { }


	/**
	 * Set a callback to resolve the target label/tag for a specific jump instruction.
	 * @param jumpResolver
	 * @return this InstructionFactory
	 */
	public InstructionFactory setJumpResolver(Resolver<RawInstruction, String> jumpResolver) {
		this.jumpResolver = jumpResolver;
		return this;
	}


	/**
	 * Set a callback to resolve the label/tag name for a specific jump destination.
	 * @param labelResolver
	 * @return this InstructionFactory
	 */
	public InstructionFactory setLabelResolver(Resolver<RawInstruction, String> labelResolver) {
		this.labelResolver = labelResolver;
		return this;
	}


	public Resolver<RawInstruction, String> getLabelResolver() {
		return labelResolver;
	}


	/**
	 * Create an instruction instance for a given raw EVM instruction and apply its effects on the stack.
	 * @param rawInstruction
	 * @param stack
	 * @return instruction instance.
	 */
	public Instruction createAndApply(RawInstruction rawInstruction, Stack<Variable> stack) {
		Instruction instruction = createInstance(rawInstruction, stack);

		if (instruction == null) {
			instruction = new _UnknownInstruction();
		}

		instruction.setRawInstruction(rawInstruction);

		if (OpCodes.isInvalid(rawInstruction.opcode)) {
			// skip stack manipulation for invalid instruction
			instruction.setInput(Instruction.NO_VARIABLES);
			instruction.setOutput(Instruction.NO_VARIABLES);
			return instruction;
		}

		if (instruction.getInput() == null) {
			// take input variables from the stack
			Variable[] input = Variable.takeFromStack(stack, OpCodes.getPopCount(rawInstruction.opcode));
			instruction.setInput(input);
		}

		Variable[] output = instruction.getOutput();
		if (output == null) {
			// create output variables
			output = Variable.createNewVariables(OpCodes.getPushCount(rawInstruction.opcode));
			instruction.setOutput(output);
		}

		// push result variable onto the stack
		for (int i = output.length - 1; i >= 0; --i) {
			stack.push(output[i]);
		}

		return instruction;
	}


	/**
	 * Creates an instruction instance for the given EVM instruction.
	 * @param rawInstruction
	 * @param stack current stack, may be modified.
	 * @return
	 */
	private Instruction createInstance(RawInstruction rawInstruction, Stack<Variable> stack) {
		switch (rawInstruction.opcode) {
			case OpCodes.STOP: return new Stop();
			case OpCodes.ADD: return new Add();
			case OpCodes.MUL: return new Mul();
			case OpCodes.SUB: return new Sub();
			case OpCodes.DIV: return new Div();
			case OpCodes.SDIV: return new SDiv();
			case OpCodes.MOD: return new Mod();
			case OpCodes.SMOD: return new SMod();
			case OpCodes.ADDMOD: return new AddMod();
			case OpCodes.MULMOD: return new MulMod();
			case OpCodes.EXP: return new Exp();
			case OpCodes.SIGNEXTEND: return new SignExtend();
			case OpCodes.LT: return new Lt();
			case OpCodes.GT: return new Gt();
			case OpCodes.SLT: return new Slt();
			case OpCodes.SGT: return new Sgt();
			case OpCodes.EQ: return new Eq();
			case OpCodes.ISZERO: return new IsZero();
			case OpCodes.AND: return new And();
			case OpCodes.OR: return new Or();
			case OpCodes.XOR: return new Xor();
			case OpCodes.NOT: return new Not();
			case OpCodes.BYTE: return new Byte();
			case OpCodes.SHL: return new Shl();
			case OpCodes.SHR: return new Shr();
			case OpCodes.SHA3: return new Sha3();
			case OpCodes.ADDRESS: return new Address();
			case OpCodes.BALANCE: return new Balance();
			case OpCodes.ORIGIN: return new Origin();
			case OpCodes.CALLER: return new Caller();
			case OpCodes.CALLVALUE: return new CallValue();
			case OpCodes.CALLDATALOAD: return new CallDataLoad();
			case OpCodes.CALLDATASIZE: return new CallDataSize();
			case OpCodes.CALLDATACOPY: return new CallDataCopy();
			case OpCodes.CODESIZE: return new CodeSize();
			case OpCodes.CODECOPY: return new CodeCopy();
			case OpCodes.GASPRICE: return new GasPrice();
			case OpCodes.EXTCODESIZE: return new ExtCodeSize();
			case OpCodes.EXTCODECOPY: return new ExtCodeCopy();
			case OpCodes.RETURNDATASIZE: return new ReturnDataSize();
			case OpCodes.RETURNDATACOPY: return new ReturnDataCopy();
			case OpCodes.EXTCODEHASH: return new Extcodehash();
			case OpCodes.BLOCKHASH: return new BlockHash();
			case OpCodes.COINBASE: return new Coinbase();
			case OpCodes.TIMESTAMP: return new BlockTimestamp();
			case OpCodes.NUMBER: return new BlockNumber();
			case OpCodes.DIFFICULTY: return new Difficulty();
			case OpCodes.GASLIMIT: return new GasLimit();
			case OpCodes.POP: return new Pop();
			case OpCodes.MLOAD: return new MLoad();
			case OpCodes.MSTORE: return new MStore();
			case OpCodes.MSTORES: return new MStore8();
			case OpCodes.SLOAD: return new SLoad();
			case OpCodes.SSTORE: return new SStore();
			case OpCodes.JUMP: return new Jump(jumpResolver.resolve(rawInstruction));
			case OpCodes.JUMPI: return new JumpI(jumpResolver.resolve(rawInstruction));
			case OpCodes.PC: return new Pc();
			case OpCodes.MSIZE: return new MSize();
			case OpCodes.GAS: return new Gas();
			case OpCodes.JUMPDEST: return new JumpDest(labelResolver.resolve(rawInstruction));
			case OpCodes.LOG0: return new Log0();
			case OpCodes.LOG1: return new Log1();
			case OpCodes.LOG2: return new Log2();
			case OpCodes.LOG3: return new Log3();
			case OpCodes.LOG4: return new Log4();
			case OpCodes.CREATE: return new Create();
			case OpCodes.CALL: return new Call();
			case OpCodes.CALLCODE: return new CallCode();
			case OpCodes.RETURN: return new Return();
			case OpCodes.DELEGATECALL: return new DelegateCall();
			case OpCodes.CREATE2: return new Create2();
			case OpCodes.STATICCALL: return new StaticCall();
			case OpCodes.REVERT: return new Revert();
			case OpCodes.INVALID: return new Invalid();
			case OpCodes.SELFDESTRUCT: return new SelfDestruct();
		}
		int pos;
		if (OpCodes.isPush(rawInstruction.opcode) > -1) {
			return new Push(rawInstruction.data);
		}
		else if ((pos = OpCodes.isSwap(rawInstruction.opcode)) > -1) {
			int stackIndexBtm = stack.size() - pos - 1;
			Variable otherVal = stack.get(stackIndexBtm);
			Variable topVal = stack.pop();
			stack.push(otherVal);
			stack.set(stackIndexBtm, topVal);
			return new Swap();
		}
		else if ((pos = OpCodes.isDup(rawInstruction.opcode)) > -1) {
			return new Dup(stack.get(stack.size() - 1 - pos));
		}

		return new Invalid();
	}

	public static String returnRawName(RawInstruction rawInstruction) {
		switch (rawInstruction.opcode) {
			case OpCodes.STOP: return Stop.class.getSimpleName();
			case OpCodes.ADD: return Add.class.getSimpleName();
			case OpCodes.MUL: return Mul.class.getSimpleName();
			case OpCodes.SUB: return Sub.class.getSimpleName();
			case OpCodes.DIV: return Div.class.getSimpleName();
			case OpCodes.SDIV: return SDiv.class.getSimpleName();
			case OpCodes.MOD: return Mod.class.getSimpleName();
			case OpCodes.SMOD: return SMod.class.getSimpleName();
			case OpCodes.ADDMOD: return AddMod.class.getSimpleName();
			case OpCodes.MULMOD: return MulMod.class.getSimpleName();
			case OpCodes.EXP: return Exp.class.getSimpleName();
			case OpCodes.SIGNEXTEND: return SignExtend.class.getSimpleName();
			case OpCodes.LT: return Lt.class.getSimpleName();
			case OpCodes.GT: return Gt.class.getSimpleName();
			case OpCodes.SLT: return Slt.class.getSimpleName();
			case OpCodes.SGT: return Sgt.class.getSimpleName();
			case OpCodes.EQ: return Eq.class.getSimpleName();
			case OpCodes.ISZERO: return IsZero.class.getSimpleName();
			case OpCodes.AND: return And.class.getSimpleName();
			case OpCodes.OR: return Or.class.getSimpleName();
			case OpCodes.XOR: return Xor.class.getSimpleName();
			case OpCodes.NOT: return Not.class.getSimpleName();
			case OpCodes.BYTE: return Byte.class.getSimpleName();
			case OpCodes.SHL: return Shl.class.getSimpleName();
			case OpCodes.SHR: return Shr.class.getSimpleName();
			case OpCodes.SAR: return Sar.class.getSimpleName();
			case OpCodes.SHA3: return Sha3.class.getSimpleName();
			case OpCodes.ADDRESS: return Address.class.getSimpleName();
			case OpCodes.BALANCE: return Balance.class.getSimpleName();
			case OpCodes.ORIGIN: return Origin.class.getSimpleName();
			case OpCodes.CALLER: return Caller.class.getSimpleName();
			case OpCodes.CALLVALUE: return CallValue.class.getSimpleName();
			case OpCodes.CALLDATALOAD: return CallDataLoad.class.getSimpleName();
			case OpCodes.CALLDATASIZE: return CallDataSize.class.getSimpleName();
			case OpCodes.CALLDATACOPY: return CallDataCopy.class.getSimpleName();
			case OpCodes.CODESIZE: return CodeSize.class.getSimpleName();
			case OpCodes.CODECOPY: return CodeCopy.class.getSimpleName();
			case OpCodes.GASPRICE: return GasPrice.class.getSimpleName();
			case OpCodes.EXTCODESIZE: return ExtCodeSize.class.getSimpleName();
			case OpCodes.EXTCODECOPY: return ExtCodeCopy.class.getSimpleName();
			case OpCodes.RETURNDATASIZE: return ReturnDataSize.class.getSimpleName();
			case OpCodes.RETURNDATACOPY: return ReturnDataCopy.class.getSimpleName();
			case OpCodes.EXTCODEHASH: return Extcodehash.class.getSimpleName();
			case OpCodes.BLOCKHASH: return BlockHash.class.getSimpleName();
			case OpCodes.COINBASE: return Coinbase.class.getSimpleName();
			case OpCodes.TIMESTAMP: return BlockTimestamp.class.getSimpleName();
			case OpCodes.NUMBER: return BlockNumber.class.getSimpleName();
			case OpCodes.DIFFICULTY: return Difficulty.class.getSimpleName();
			case OpCodes.GASLIMIT: return GasLimit.class.getSimpleName();
			case OpCodes.POP: return Pop.class.getSimpleName();
			case OpCodes.MLOAD: return MLoad.class.getSimpleName();
			case OpCodes.MSTORE: return MStore.class.getSimpleName();
			case OpCodes.MSTORES: return MStore8.class.getSimpleName();
			case OpCodes.SLOAD: return SLoad.class.getSimpleName();
			case OpCodes.SSTORE: return SStore.class.getSimpleName();
			case OpCodes.JUMP: return Jump.class.getSimpleName();
			case OpCodes.JUMPI: return JumpI.class.getSimpleName();
			case OpCodes.PC: return Pc.class.getSimpleName();
			case OpCodes.MSIZE: return MSize.class.getSimpleName();
			case OpCodes.GAS: return Gas.class.getSimpleName();
			case OpCodes.JUMPDEST: return JumpDest.class.getSimpleName();
			case OpCodes.LOG0: return Log0.class.getSimpleName();
			case OpCodes.LOG1: return Log1.class.getSimpleName();
			case OpCodes.LOG2: return Log2.class.getSimpleName();
			case OpCodes.LOG3: return Log3.class.getSimpleName();
			case OpCodes.LOG4: return Log4.class.getSimpleName();
			case OpCodes.CREATE: return Create.class.getSimpleName();
			case OpCodes.CALL: return Call.class.getSimpleName();
			case OpCodes.CALLCODE: return CallCode.class.getSimpleName();
			case OpCodes.RETURN: return Return.class.getSimpleName();
			case OpCodes.DELEGATECALL: return DelegateCall.class.getSimpleName();
			case OpCodes.CREATE2: return Create2.class.getSimpleName();
			case OpCodes.STATICCALL: return StaticCall.class.getSimpleName();
			case OpCodes.REVERT: return Revert.class.getSimpleName();
			case OpCodes.INVALID: return Invalid.class.getSimpleName();
			case OpCodes.SELFDESTRUCT: return SelfDestruct.class.getSimpleName();

		}
		if (OpCodes.isPush(rawInstruction.opcode) > -1) {
			return Push.class.getSimpleName();
		}
		else if (OpCodes.isSwap(rawInstruction.opcode) > -1) {
			return Swap.class.getSimpleName();
		}
		else if (OpCodes.isDup(rawInstruction.opcode) > -1) {
			return Dup.class.getSimpleName();
		}

		return Invalid.class.getSimpleName();
	}

}
