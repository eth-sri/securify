package ch.securify.dslpatterns.predicates;

import ch.securify.dslpatterns.AbstractDSLPattern;

/**
 * Abstract class that represents predicates expressed in the DSL language,
 * like {@link Follow}, {@link MustFollow}...
 */
public class AbstractPredicate extends AbstractDSLPattern {

    public String getStringRepresentation() {
        return "AbstractPredicate";
    }
}
