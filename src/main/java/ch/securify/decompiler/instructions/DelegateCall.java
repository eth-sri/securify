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

public class DelegateCall extends CallingInstruction implements _TypeInstruction {

    @Override
    public String getStringRepresentation() {
        return getOutput()[0] + " = delegatecall(gas: " + getInput()[0] + ", to_addr: " + getInput()[1] + ", " +
                "in_offset: " + getInput()[2] + ", in_size: " + getInput()[3] + ", " +
                "out_offset: " + getInput()[4] + ", out_size: " + getInput()[5] + ")";
    }

    @Override
    public int getInputMemoryOffset() {
        return 2;
    }

    @Override
    public int getInputMemorySize() {
        return 3;
    }

    @Override
    public Variable getValue() {
        // no use for this yet, could (maybe) be implemented by using the Callvalue opcode in the dataflow and returning its output
        throw new UnsupportedOperationException("Not Implemented");
    }
}
