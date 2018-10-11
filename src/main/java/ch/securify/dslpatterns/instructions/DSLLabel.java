package ch.securify.dslpatterns.instructions;

public class DSLLabel {
    private String name;

    private static int nextLabelId;
    static { resetLabelNameGenerator(); }

    private static synchronized String generateLabelName() {
        StringBuilder sb = new StringBuilder();


        int varId = nextLabelId;
        do {
            char letter = (char) ('a' + (varId % 26));
            sb.append(letter);
            varId /= 26;
        } while (varId > 0);
        nextLabelId++;
        sb.append("-L"); //it's a label and not a variable
        return sb.reverse().toString();
    }

    /**
     * Reset the naming of Variables to start again from 'a'.
     */
    public static void resetLabelNameGenerator() {
        nextLabelId = 0;
    }

    public DSLLabel() {
        name = generateLabelName();
    }

    public String getName() {
        return name;
    }
}
