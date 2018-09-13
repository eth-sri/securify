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

import java.math.BigInteger;

import ch.securify.decompiler.Variable;
import ch.securify.utils.BigIntUtil;

public class And extends Instruction {

    @Override
    public String getStringRepresentation() {
        return getOutput()[0] + " = " + getInput()[0] + " & " + getInput()[1];
    }

    @Override
    public void computeResultValues() {
        if (getInput()[0].hasConstantValue() && getInput()[1].hasConstantValue()) {
            byte[] a = getInput()[0].getConstantValue();
            byte[] b = getInput()[1].getConstantValue();
            byte[] result = new byte[Math.min(a.length, b.length)];
            for (int i = 0; i < result.length; ++i) {
                result[i] = (byte) (a[i + (a.length - result.length)] & b[i + (b.length - result.length)]);
            }
            getOutput()[0].setConstantValue(result);
            return;
        }

        // One operand is an address and the other has a constant value. Check if last _AddressType.addressLength bytes of the constant value are 0.
        // If yes, then the result of the And operation is 0, because addresses occupy _AddressType.addressLength bytes in EVM.
        boolean flag1, flag2;
        if ((flag1 = getInput()[0].hasConstantValue() && getInput()[1].getValueTypes().contains(_AddressType.class)) ||
            (flag2 = getInput()[1].hasConstantValue() && getInput()[0].getValueTypes().contains(_AddressType.class))){
            byte[] b;
            if (flag1) {
                b = getInput()[0].getConstantValue();
            } else {
                // assume(flag2);
                b = getInput()[1].getConstantValue();
            }
            BigInteger bInt = BigIntUtil.fromInt256(b);
            if (bInt.equals(BigInteger.ZERO)) {
                getOutput()[0].setConstantValue(BigIntUtil.toInt256(BigInteger.ZERO));
                return;
            }
            if (b.length > _AddressType.addressLength - 1) {
                boolean isZero = true;
                for (int i = 0; i < _AddressType.addressLength; i++) {
                    if (b[b.length - 1 - i] != 0) {
                        isZero = false;
                    }
                }
                if (isZero) {
                    getOutput()[0].setConstantValue(BigIntUtil.toInt256(BigInteger.ZERO));
                    return;
                }
            }
        }

        getOutput()[0].setConstantValue(Variable.VALUE_ANY);

    }

}
