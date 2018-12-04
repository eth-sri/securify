package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;

/**
 * IsArg predicated, checks if a variable is the argument of a function,
 * is used to express the "arg" tag present in the paper
 */
public class IsArg extends AbstractVariablePredicate {
    public IsArg(Variable x) {
        super(x);
    }

    @Override
    protected String getName() {
        return "isArg";
    }
}
