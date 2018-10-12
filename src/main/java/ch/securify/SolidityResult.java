package ch.securify;

import java.util.TreeMap;
import ch.securify.analysis.SecurifyErrors;

public class SolidityResult {
    private static final SolidityResult INSTANCE = new SolidityResult();
    private SolidityResult(){}

    public static SolidityResult getInstance(){
        return INSTANCE;
    }

    TreeMap<String, SmallPatternResult> results = new TreeMap<>();

    SecurifyErrors securifyErrors = new SecurifyErrors();
}