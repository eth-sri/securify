package ch.securify.dslpatterns.predicates;

import ch.securify.decompiler.Variable;

/**
 * The HasValue predicate, takes as input two variable, the first one is the one containing the constant
 * the second one is the one to which the constant will be binded
 */
public class HasValue extends AbstractPredicate {

        private Variable var;
        private Variable constant;

        public HasValue(Variable var, Variable constant) {
            this.var = var;
            this.constant = constant;
        }

        @Override
        public String getStringRepresentation() {
            StringBuilder sb = new StringBuilder();
            sb.append("hasValue(");
            sb.append(var.getName());
            sb.append(" , ");
            sb.append(constant.getName());
            sb.append(")");

            return sb.toString();
        }
}
