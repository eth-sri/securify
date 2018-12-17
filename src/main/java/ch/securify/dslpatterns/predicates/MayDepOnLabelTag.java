package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The MayDepOn predicate with input a label and a tag represented by a class
 */
public class MayDepOnLabelTag extends AbstractPredicate {
    private DSLLabel l;
    private Class tag;

    public MayDepOnLabelTag(DSLLabel l, Class tag) {
        this.l = l;
        this.tag = tag;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("mayDepOn(");
        sb.append(l.getName());
        sb.append(" , ");
        sb.append(tag.getSimpleName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        Set<DSLLabel> labels = new HashSet<>(1);
        if(DSLLabel.isValidLabel(l))
            labels.add(l);
        return labels;
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        sb.append("mayDepOn(");
        sb.append(l.getName());
        sb.append(" , ");
        sb.append(analyzer.getCode(tag));
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<Class> getTags() {
        HashSet tagSet = new HashSet<Class>();
        tagSet.add(tag);
        return tagSet;
    }

    public DSLLabel getLabel() {
        return l;
    }

    public Class getTag() {
        return tag;
    }
}