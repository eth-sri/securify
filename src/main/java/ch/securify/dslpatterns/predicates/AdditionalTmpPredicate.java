package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This represents the predicate that is added to handle the {@link ch.securify.dslpatterns.All}
 * in the datalog translation
 */
public class AdditionalTmpPredicate extends AbstractPredicate {

    private List<Variable> vars = new ArrayList<>();
    private List<DSLLabel> labels = new ArrayList<>();
    private String name;

    public AdditionalTmpPredicate(String name, List<Variable> vars, List<DSLLabel> labels) {
        this.name = name;
        this.vars.addAll(vars);
        this.labels.addAll(labels);
    }

    public String getStringRepresentation() {
        return "AdditionalTmpPredicate (?)";
    }

    @Override
    public String getDatalogStringRep(DSLAnalysis analyzer) {
        StringBuilder sb = new StringBuilder();

        sb.append(name);
        sb.append("( ");

        if(!vars.isEmpty()) {
            sb.append(vars.get(0).getName());
            for(int i = 1; i < vars.size(); i++) {
                sb.append(", ");
                sb.append(vars.get(i).getName());
            }
        }

        if(!labels.isEmpty()) {
            if(!vars.isEmpty())
                sb.append(" , ");
            sb.append(labels.get(0).getName());
            for(int i = 1; i < labels.size(); i++) {
                sb.append(", ");
                sb.append(labels.get(i).getName());
            }
        }

        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<DSLLabel> getLabels() {
        return new HashSet<>(labels);
    }

    @Override
    public Set<Variable> getVariables() {
        return new HashSet<>(vars);
    }
}
