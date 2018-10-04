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


package ch.securify.analysis;

import java.io.IOException;
import java.util.List;

import ch.securify.decompiler.Variable;
import ch.securify.decompiler.instructions.Instruction;

public class Dataflow extends AbstractDataflow {
    public AbstractDataflow mustExplicitDataflow;
    public AbstractDataflow mayImplicitDataflow;

    public Dataflow(List<Instruction> instructions) {
        try {
            mustExplicitDataflow = new MustExplicitDataflow(instructions);
            mayImplicitDataflow = new MayImplicitDataflow(instructions);
        } catch(IOException | InterruptedException e){
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    @Override
    public void dispose() throws IOException, InterruptedException {
        mustExplicitDataflow.dispose();
        mayImplicitDataflow.dispose();
    }

    @Override
    public int mayFollow(Instruction instr1, Instruction instr2) {
        int s = mayImplicitDataflow.mayFollow(instr1, instr2);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int varMayDepOn(Instruction instr1, Variable lhs, Object type) {
        int s = mayImplicitDataflow.varMayDepOn(instr1, lhs, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int memoryMayDepOn(Instruction instr1, int offset, Object type) {
        int s = mayImplicitDataflow.memoryMayDepOn(instr1, offset, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int memoryMayDepOn(Instruction instr, Object type) {
        int s = mayImplicitDataflow.memoryMayDepOn(instr, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int instrMayDepOn(Instruction instr, Object type) {
        int s = mayImplicitDataflow.instrMayDepOn(instr, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int mustPrecede(Instruction instr1, Instruction instr2) {
        int s = mustExplicitDataflow.mustPrecede(instr1, instr2);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int varMustDepOn(Instruction instr1, Variable lhs, Object type) {
        int s = mustExplicitDataflow.varMustDepOn(instr1, lhs, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    public int memoryMustDepOn(Instruction instr1, int offset, Object type) {
        int s = mustExplicitDataflow.memoryMustDepOn(instr1, offset, type);
        if (s == Status.UNKNOWN)
            throw new TimeoutException();
        return s;
    }

    @Override
    protected void deriveFollowsPredicates() { throw new UnsupportedOperationException(); }

    @Override
    protected void deriveIfPredicates() { throw new UnsupportedOperationException(); }

    @Override
    protected void createSLoadRule(Instruction instr, Variable index, Variable var) { throw new UnsupportedOperationException(); }

    @Override
    protected void createMLoadRule(Instruction instr, Variable offset, Variable var) { throw new UnsupportedOperationException(); }
}
