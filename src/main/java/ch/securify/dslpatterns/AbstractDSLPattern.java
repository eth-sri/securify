package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.List;

/**
 * This class is the superclass of all possible patterns written in the DSL language
 */
public abstract class AbstractDSLPattern {

    /**
     * @return a string description of the specific pattern
     */
    public String getStringRepresentation() {
        return "AbstractDSLPattern";
    }

    public abstract List<Variable> getVariables();
    public abstract List<DSLLabel> getLabels();
}
