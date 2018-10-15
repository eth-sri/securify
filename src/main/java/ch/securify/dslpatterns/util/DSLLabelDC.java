package ch.securify.dslpatterns.util;

/**
 * This class represents that the writer of the pattern
 * doesn't care about the value of the label field
 */
public class DSLLabelDC extends DSLLabel {

    @Override
    public String getName() {
        return "_";
    }
}
