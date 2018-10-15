package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;
import ch.securify.dslpatterns.util.DSLLabel;

/**
 * Factory to create dsl predicates objects
 */
public class PredicateFactory {

    public static Follow follow(DSLLabel l1, DSLLabel l2) {
        return new Follow(l1, l2);
    }

    public static MustFollow mustFollow(DSLLabel l1, DSLLabel l2) {
        return new MustFollow(l1, l2);
    }

    public DetByVarTag detBy(Variable var, Class tag) {
        return new DetByVarTag(var, tag);
    }

    public DetByVarVar detBy(Variable var1, Variable var2) {
        return new DetByVarVar(var1, var2);
    }

    public MayDepOnLabelTag mayDepOn(DSLLabel l, Class tag) {
        return new MayDepOnLabelTag(l, tag);
    }

    public MayDepOnVarVar mayDepOn(Variable var1, Variable var2) {
        return new MayDepOnVarVar(var1, var2);
    }

    public MayDepOnVarTag mayDepOn(Variable v, Class tag) {
        return new MayDepOnVarTag(v, tag);
    }

    public MayFollow mayFollow(DSLLabel l1, DSLLabel l2) {
        return new MayFollow(l1, l2);
    }

    public IsConst isConst(Variable x) {
        return new IsConst(x);
    }
}
