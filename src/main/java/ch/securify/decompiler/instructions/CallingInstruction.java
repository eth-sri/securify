package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;

public abstract class CallingInstruction extends Instruction {
    public abstract int getInputMemoryOffset();

    public abstract int getInputMemorySize();

    public boolean isBuiltInContractCall() {
        Variable toAddrVar = this.getInput()[1];
        if (toAddrVar.hasConstantValue()) {
            BigInteger toAddr = BigIntUtil.fromInt256(toAddrVar.getConstantValue());
            // is call to built-in contract
            final int firstPrecompiledContractAddress = 1, lastPrecompiledContractAddress = 8;
            for (int i = firstPrecompiledContractAddress; i <= lastPrecompiledContractAddress; i++) {
                if (toAddr.equals(BigInteger.valueOf(i))) {
                    return true;
                }
            }
        }
        return false;
    }
}
