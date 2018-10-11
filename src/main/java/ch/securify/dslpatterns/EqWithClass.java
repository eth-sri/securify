package ch.securify.dslpatterns;

import ch.securify.decompiler.Variable;

public class EqWithClass extends AbstractDSLPattern {
    private Variable v1;
    private Class classtype;

    public EqWithClass(Variable v1, Class classtype) {
        this.v1 = v1;
        this.classtype = classtype;
    }

    /**
     * @return a string description of the equality
     */
    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append(v1.getName());
        sb.append(" = ");
        sb.append(classtype.getSimpleName());
        return sb.toString();
    }
}
