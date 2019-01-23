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

public class _VirtualAssignment extends Instruction implements _VirtualInstruction {

	public _VirtualAssignment(Variable assignee, Variable source) {
		setInput(source);
		setOutput(assignee);
	}

	@Override
	public String getStringRepresentation() {
		return getOutput()[0] + " = " + getInput()[0];
	}

	@Override
	public void computeResultValues() {
		if (getInput()[0].hasConstantValue() && getOutput()[0].getConstantValue() == Variable.VALUE_UNDEFINED) {
			getOutput()[0].setConstantValue(getInput()[0].getConstantValue());
		}
		else if (getInput()[0].hasConstantValue() && getOutput()[0].hasConstantValue()
				&& BigIntUtil.fromInt256(getOutput()[0].getConstantValue()).equals(BigIntUtil.fromInt256(getInput()[0].getConstantValue()))) {
			// same output value as already assigned
		}
		else {
			getOutput()[0].setConstantValue(Variable.VALUE_ANY);
		}
	}

}
