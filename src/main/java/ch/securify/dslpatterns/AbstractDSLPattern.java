package ch.securify.dslpatterns;

/**
 * This class is the superclass of all possible patterns written in the DSL language
 */
public class AbstractDSLPattern {

    /**
     * @return a string description of the specific pattern
     */
    public String getStringRepresentation() {
        return "AbstractDSLPattern";
    }
}
