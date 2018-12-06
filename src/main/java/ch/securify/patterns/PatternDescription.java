package ch.securify.patterns;

import java.util.HashMap;

public class PatternDescription {
    private static HashMap<String, String> catIdToName= new HashMap<>();
    static {
        catIdToName.put("RecursiveCalls",  "Recursive Calls");
        catIdToName.put("InsecureCodingPatterns", "Insecure Coding Patterns");
        catIdToName.put("UnexpectedEtherFlows", "Unexpected Ether Flows");
        catIdToName.put("DependenceOnUnsafeInputs", "Dependence On Unsafe Inputs");
    }

    public PatternDescription(String categoryId, Class patternId, String patternInfo,
                              String patternDescription, Severity severity, Type type) {
        this.categoryId = categoryId;
        this.categoryName = catIdToName.get(categoryId);
        this.patternId = patternId.getSimpleName();
        this.patternInfo = patternInfo;
        this.patternDescription = patternDescription;
        this.severity = severity;
        this.type = type;
    }

    enum Severity {
        Critical,
        High,
        Medium,
        Low
    }

    enum Type {
        Security,
        Trust
    }

    String categoryId;
    String categoryName;
    String patternId;
    String patternInfo;
    String patternDescription;
    Severity severity;
    Type type;
}
