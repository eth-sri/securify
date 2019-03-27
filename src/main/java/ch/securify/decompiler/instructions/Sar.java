package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;

public class Sar extends Instruction {

    @Override
    public String getStringRepresentation() {
        return getOutput()[0] + " = " + getInput()[1] + " >> " + getInput()[0];
    }

    @Override
    public void computeResultValues() {
        if (getInput()[0].hasConstantValue() && getInput()[1].hasConstantValue() &&
                getOutput()[0].getConstantValue() == Variable.VALUE_UNDEFINED) {
        	BigInteger r;
            BigInteger a = BigIntUtil.fromUint256(getInput()[0].getConstantValue());
            BigInteger b = BigIntUtil.fromInt256(getInput()[1].getConstantValue());
            if(a.compareTo(BigInteger.valueOf(256)) >= 0) {
            	if(b.signum() == -1) {
            		r = BigInteger.valueOf(-1);
            	}else {
            		r = BigInteger.ZERO;
            	}
            } else if(b.signum() != -1) {
                r = b.shiftRight(a.intValueExact());
            } else  {
            	r = b;
            	for (int i = 0; i < a.intValueExact(); i++) {
            		r.shiftRight(1);
            		r.setBit(256);
				}
            }
            getOutput()[0].setConstantValue(BigIntUtil.toInt256(r));
        } else {
            getOutput()[0].setConstantValue(Variable.VALUE_ANY);
        }
    }
}
