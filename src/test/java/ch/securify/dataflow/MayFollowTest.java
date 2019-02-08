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

package ch.securify.dataflow;

import org.junit.Test;

import ch.securify.analysis.Status;
import ch.securify.decompiler.instructions.Call;
import ch.securify.decompiler.instructions.Instruction;
import ch.securify.decompiler.instructions.StaticCall;
import ch.securify.patterns.DAOConstantGas;
import ch.securify.patterns.HelperInstructionPattern;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MayFollowTest {

    @Test
    public void isViolation() throws IOException {
        String hexViolation = "src/test/resources/solidity/repeated-calls-tp2.bin.hex";
        HelperDataFlowTest helper = new HelperDataFlowTest(hexViolation);
        
        boolean foundCall = false;
        for(Instruction instr : helper.instructions) {
        	if((instr instanceof Call) || (instr instanceof StaticCall)) {
        		foundCall = true;
        		Instruction prev = instr.getPrev();
        		
        		// Test detection of circular control flow
        		int s = helper.dataflow.mayFollow(instr, prev);
        		assertEquals(s, Status.SATISFIABLE);
        		s = helper.dataflow.mayFollow(prev, instr);
        		assertEquals(s, Status.SATISFIABLE);        		
        	}
        }
        assertEquals(foundCall, true);
    }
}