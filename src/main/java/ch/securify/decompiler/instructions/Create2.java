package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;

public class Create2 extends Instruction implements _TypeInstruction {

    @Override
    public String getStringRepresentation() {
        return getOutput()[0] + " = create2(endowment: " + getInput()[0] + ", memoffset: " + getInput()[1] +
                ", length: " + getInput()[2] + ", salt:" + getInput()[3] + ")";
    }

    public void computeResultTypes() {
        for (Variable output : getOutput()) {
            output.addValueType(getClass());
        }
    }

}
