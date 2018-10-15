package ch.securify.dslpatterns.util;

import ch.securify.decompiler.Variable;

/**
 * This class represents that the writer of the pattern
 * doesn't care about the value of the variable field
 */
public class VariableDC extends Variable {

    @Override
    public String getName() {
        return "_";
    }
}
