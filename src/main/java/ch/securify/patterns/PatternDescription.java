package ch.securify.patterns;

public class PatternDescription {
    public PatternDescription(String categoryId, String categoryName, Class patternId, String patternInfo,
                              String patternDescription, Severity severity, Type type) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
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
