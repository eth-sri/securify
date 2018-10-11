package ch.securify.dslpatterns;

public class Not extends AbstractDSLPattern {

    private AbstractDSLPattern negatedPattern;

    public Not(AbstractDSLPattern negatedPattern) {
        this.negatedPattern = negatedPattern;
    }

    /**
     * @return a string description of the negated pattern
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("!(");
        sb.append(negatedPattern.getStringRepresentation());
        sb.append(")");
        return sb.toString();
    }
}
