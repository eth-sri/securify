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

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


public class LockedEtherTest {

    @Test
    public void isViolation() throws IOException {
        String hex = "src/test/resources/solidity/LockedEther.bin.hex";
        ContractPatternTest instructionPatternTest = new ContractPatternTest(hex, new LockedEther());
        // as many violations as there are functions in the smart contract
        assertEquals(2, instructionPatternTest.pattern.violations.size());
    }
    
    @Test
    public void isNoViolationDelegate() throws IOException {
        String hex = "src/test/resources/solidity/LockedEtherDelegate.bin.hex";
        ContractPatternTest instructionPatternTest = new ContractPatternTest(hex, new LockedEther());
        // as many violations as there are functions in the smart contract
        assertEquals(0, instructionPatternTest.pattern.violations.size());
        assertEquals(0, instructionPatternTest.pattern.warnings.size());
        assertEquals(1, instructionPatternTest.pattern.safe.size());
    }
}