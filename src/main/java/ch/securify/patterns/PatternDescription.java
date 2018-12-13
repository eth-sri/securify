package ch.securify.patterns;

import java.util.HashMap;

class CatIdToName {
    static HashMap<String, String> map = new HashMap<>();
    static {
        map.put("RecursiveCalls",  "Recursive Calls");
        map.put("InsecureCodingPatterns", "Insecure Coding Patterns");
        map.put("UnexpectedEtherFlows", "Unexpected Ether Flows");
        map.put("DependenceOnUnsafeInputs", "Dependence On Unsafe Inputs");
    }
}

public class PatternDescription {

    public PatternDescription(String categoryId, Class patternId, String patternInfo,
                              String patternDescription, Severity severity, Type type) {
        this.categoryId = categoryId;
        this.categoryName = CatIdToName.map.get(categoryId);
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
        Trust,
        Design
    }

    // fields used in JSON export
    private String categoryId;
    private String categoryName;
    private String patternId;
    private String patternInfo;
    private String patternDescription;
    Severity severity;
    Type type;
}
