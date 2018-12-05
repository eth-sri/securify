package ch.securify.dslpatterns.util;

import ch.securify.decompiler.Variable;

/**
 * This class represents the variable that gets introduced in the translation process
 * to translate the Arg tag (represented by {@link ch.securify.dslpatterns.tags.DSLArg}
 */
public class VariableArg extends Variable {

    @Override
    public String getName() {
        return "VarRepArg";
    }
}