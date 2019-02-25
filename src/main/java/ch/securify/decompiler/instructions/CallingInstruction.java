package ch.securify.decompiler.instructions;

import ch.securify.decompiler.Variable;
import ch.securify.utils.BigIntUtil;

import java.math.BigInteger;

public abstract class CallingInstruction extends Instruction {
    public abstract int getInputMemoryOffset();

    public abstract int getInputMemorySize();

    public abstract Variable getValue();

    public boolean isBuiltInContractCall() {
        Variable toAddrVar = this.getInput()[1];
        if (toAddrVar.hasConstantValue()) {
            BigInteger toAddr = BigIntUtil.fromInt256(toAddrVar.getConstantValue());
            // is call to built-in contract
            final BigInteger firstPrecompiledContractAddress = BigInteger.valueOf(1);
            final BigInteger lastPrecompiledContractAddress = BigInteger.valueOf(8);
            return toAddr.compareTo(firstPrecompiledContractAddress) >= 0 &&
                    toAddr.compareTo(lastPrecompiledContractAddress) <= 0;
        }
        return false;
    }
}
