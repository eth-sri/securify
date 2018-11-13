package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Patterns that contain a list of arguments, e.g. {@link And}, {@link Or}
 */
public abstract class AbstractListDSLPattern extends AbstractDSLPattern {
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

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>();
        patterns.forEach((patt) -> vars.addAll(patt.getVariables()));
        return vars;
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>();
        patterns.forEach((patt) -> labels.addAll(patt.getLabels()));
        return labels;
    }

    protected String getPatternName() {
        return "AbstractListDSLPattern";
    }

    public List<AbstractDSLPattern> getPatterns() {
        return patterns;
    }


}
