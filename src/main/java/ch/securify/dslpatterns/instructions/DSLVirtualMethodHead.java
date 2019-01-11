package ch.securify.dslpatterns.instructions;

import ch.securify.analysis.DSLAnalysis;
import ch.securify.dslpatterns.util.DSLLabel;

public class DSLVirtualMethodHead extends AbstractDSLInstruction {

    public DSLVirtualMethodHead(DSLLabel label) {
        super(label);
    }

    @Override
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        sb.append("virtualMethodHead(");
        sb.append(label.getName());
        sb.append(")");

        return sb.toString();
    }

    @Override
    public String getDatalogStringRepDC(DSLAnalysis analyzer) {
        return getDatalogStringRep(analyzer);
    }

    @Override
    public DSLVirtualMethodHead getCopy() {
        return new DSLVirtualMethodHead(getLabel());
    }


}