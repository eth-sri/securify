package ch.securify.dslpatterns.predicates;

import ch.securify.dslpatterns.instructions.DSLLabel;

public class PredicateFactory {

    public static Follow follow(DSLLabel l1, DSLLabel l2) {
        return new Follow(l1, l2);
    }

    public static MustFollow mustFollow(DSLLabel l1, DSLLabel l2) {
        return new MustFollow(l1, l2);
    }
}
