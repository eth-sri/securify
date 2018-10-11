package ch.securify.dslpatterns;

public class Implies extends AbstractDSLPattern {

    private AbstractDSLPattern lhs, rhs;

    public Implies(AbstractDSLPattern lhs, AbstractDSLPattern rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    /**
     * @return a string description of the Implies
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.getStringRepresentation());
        sb.append(" => ");
        sb.append(rhs.getStringRepresentation());
        return sb.toString();
    }
}
