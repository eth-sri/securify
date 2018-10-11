package ch.securify.dslpatterns;

import java.util.List;

public class Or extends AbstractListDSLPattern {

    public Or(List<AbstractDSLPattern> patterns) {
        super(patterns);
    }

    @Override
    protected String getPatternName() {
        return "or";
    }
}
