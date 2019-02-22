package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.HashSet;
import java.util.Set;

/**
 * Mimics the same hashmap inside {@link ch.securify.analysis.DSLAnalysis}
 * Maps a constant into a variable introduced to represent that constant in the offset of a storage operation
 */
public class OffsetToStorageVar extends AbstractPredicate {
    private Variable offsetVar, storageVar;

    public OffsetToStorageVar(Variable offsetVar, Variable storageVar) {
        this.offsetVar = offsetVar;
        this.storageVar = storageVar;
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("offsetToStorageVar(");
        sb.append(offsetVar.getName());
        sb.append(" , ");
        sb.append(storageVar.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public Set<Variable> getVariables() {
        Set<Variable> vars = new HashSet<>(2);
        if(VariableDC.isValidVariable(offsetVar))
            vars.add(offsetVar);
        if(VariableDC.isValidVariable(storageVar))
            vars.add(storageVar);
        return vars;
    }
}
