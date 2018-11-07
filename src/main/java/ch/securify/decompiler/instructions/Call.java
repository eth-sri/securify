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
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;

public class Call extends Instruction implements _TypeInstruction {

	@Override
	public String getStringRepresentation() {
		return getOutput()[0] + " = call(gas: " + getInput()[0] + ", to_addr: " + getInput()[1] + ", " +
				"value: " + getInput()[2] + ", in_offset: " + getInput()[3] + ", in_size: " + getInput()[4] + ", " +
				"out_offset: " + getInput()[5] + ", out_size: " + getInput()[6] + ")";
	}

	public boolean isBuiltInContractCall() {
		Variable toAddrVar = getInput()[1];
		if (toAddrVar.hasConstantValue()) {
			BigInteger toAddr = BigIntUtil.fromInt256(toAddrVar.getConstantValue());
			if (toAddr.equals(BigInteger.valueOf(1)) || toAddr.equals(BigInteger.valueOf(2)) ||
					toAddr.equals(BigInteger.valueOf(3)) || toAddr.equals(BigInteger.valueOf(4))) {
				// is call to built-in contract
				return true;
			}
		}
		return false;
	}

	/**
	 * @return the amount connected with the call
	 */
	public Variable getAmount() {
		return getInput()[2];
	}

	/**
	 * @return the variable returned
	 */
	public Variable getReturnVar() {
		return getOutput()[0];
	}



}
