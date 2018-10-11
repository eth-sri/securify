package ch.securify.dslpatterns;

import java.util.List;

/**
 * Normal or between patterns
 */
public class Or extends AbstractListDSLPattern {

    public Or(List<AbstractDSLPattern> patterns) {
        super(patterns);
    }

    @Override
    protected String getPatternName() {
        return "or";
    }
}
