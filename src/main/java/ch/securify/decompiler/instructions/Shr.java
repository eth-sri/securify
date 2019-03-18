package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;

public class Shr extends Instruction {

    @Override
    public String getStringRepresentation() {
        return getOutput()[0] + " = " + getInput()[1] + " >> " + getInput()[0];
    }

    @Override
    public void computeResultValues() {
        if (getInput()[0].hasConstantValue() && getInput()[1].hasConstantValue() &&
                getOutput()[0].getConstantValue() == Variable.VALUE_UNDEFINED) {
            BigInteger a = BigIntUtil.fromInt256(getInput()[0].getConstantValue());
            BigInteger b = BigIntUtil.fromInt256(getInput()[1].getConstantValue());
            BigInteger r = b.shiftRight(a.intValueExact());
            getOutput()[0].setConstantValue(BigIntUtil.toInt256(r));
        } else {
            getOutput()[0].setConstantValue(Variable.VALUE_ANY);
        }
    }
}
