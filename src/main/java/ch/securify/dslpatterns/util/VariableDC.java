package ch.securify.dslpatterns.util;

import ch.securify.decompiler.Variable;

/**
 * This class represents that the writer of the pattern
 * doesn't care about the value of the variable field
 */
public class VariableDC extends Variable {

    /**
     * Checks if the variable is null or an instance of {@link VariableDC}
     * @param var the variable
     * @return true if it's a valid variable
     */
    public static boolean isValidVariable(Variable var)
    {
        if(var == null || var instanceof VariableDC)
            return false;
        return true;
    }

    @Override
    public String getName() {
        return "_";
    }
}
