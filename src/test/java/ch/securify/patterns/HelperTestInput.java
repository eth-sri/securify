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

package ch.securify.patterns;

import ch.securify.CompilationHelpers;
import ch.securify.Main;
import ch.securify.analysis.AbstractDataflow;
import ch.securify.analysis.DataflowFactory;
import ch.securify.decompiler.instructions.Instruction;

import java.io.IOException;
import java.util.List;

public class HelperTestInput {
    List<Instruction> instructions;
    List<List<Instruction>> methodBodies;
    AbstractDataflow dataflow;


    public HelperTestInput(String hexFilename) throws IOException {
        byte[] bin = CompilationHelpers.extractBinaryFromHexFile(hexFilename);
        instructions = Main.decompileContract(bin);
        methodBodies = Main.splitInstructionsIntoMethods(instructions);
        dataflow = DataflowFactory.getDataflow(instructions);
    }
}
