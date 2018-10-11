package ch.securify.dslpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * Patterns that contain a list of arguments, e.g. {@link And}, {@link Or}
 */
public class AbstractListDSLPattern extends AbstractDSLPattern {
    protected List<AbstractDSLPattern> patterns;

    public AbstractListDSLPattern(List<AbstractDSLPattern> patterns) {
        this.patterns = new ArrayList<AbstractDSLPattern>(patterns);
    }

    /**
     * @return a string description of the list patterns
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();

        //add first one if size is compatible
        if(!patterns.isEmpty())
            sb.append(patterns.get(0).getStringRepresentation());

        //iterate over the rest of them, if present
        for(int i = 1; i < patterns.size(); i++) {
            sb.append(" ");
            sb.append(getPatternName());
            sb.append(" ");
            sb.append(patterns.get(i).getStringRepresentation());
        }

        return sb.toString();
    }

    protected String getPatternName() {
        return "AbstractListDSLPattern";
    }
}
