package ch.securify.dslpatterns;

import java.util.List;

public class And extends AbstractListDSLPattern {

    public And(List<AbstractDSLPattern> patterns) {
        super(patterns);
    }

    @Override
    protected String getPatternName() {
        return "and";
    }
}
