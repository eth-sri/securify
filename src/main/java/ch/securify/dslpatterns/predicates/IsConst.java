package ch.securify.dslpatterns.predicates;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;
import ch.securify.dslpatterns.util.VariableDC;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The IsConst predicate, takes as input a variable
 */
public class IsConst extends AbstractVariablePredicate {

    public IsConst(Variable x) {
        super(x);
    }

    @Override
    protected String getName() {
        return "isConst";
    }
}
