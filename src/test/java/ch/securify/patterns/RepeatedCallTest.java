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

public class RepeatedCallTest {

    @Test
    public void isSafe() throws IOException {
        String hexViolation = "src/test/resources/solidity/repeated-calls-tn.bin.hex";
        HelperInstructionPattern helperInstructionPattern = new HelperInstructionPattern(hexViolation, new RepeatedCall());
        assertEquals(0, helperInstructionPattern.pattern.violations.size());
        assertEquals(0, helperInstructionPattern.pattern.warnings.size());
        assertEquals(1, helperInstructionPattern.pattern.safe.size());
    }

    @Test
    public void isSafe2() throws IOException {
        String hexViolation = "src/test/resources/solidity/repeated-calls-tn2.bin.hex";
        HelperInstructionPattern helperInstructionPattern = new HelperInstructionPattern(hexViolation, new RepeatedCall());
        assertEquals(0, helperInstructionPattern.pattern.violations.size());
        assertEquals(0, helperInstructionPattern.pattern.warnings.size());
        assertEquals(2, helperInstructionPattern.pattern.safe.size());
    }

    @Test
    public void isViolation() throws IOException {
        String hexViolation = "src/test/resources/solidity/repeated-calls-tp.bin.hex";
        HelperInstructionPattern helperInstructionPattern = new HelperInstructionPattern(hexViolation, new RepeatedCall());
        assertEquals(1, helperInstructionPattern.pattern.violations.size());
        assertEquals(0, helperInstructionPattern.pattern.warnings.size());
        assertEquals(1, helperInstructionPattern.pattern.safe.size());
    }
}
