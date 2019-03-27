package ch.securify.decompiler.instructions;

public class Extcodehash extends Instruction implements _TypeInstruction {

	@Override
	public String getStringRepresentation() {
		return getOutput()[0] + " = extcodehash(" + getInput()[0] + ")";
	}

}
