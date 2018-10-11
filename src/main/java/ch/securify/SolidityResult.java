package ch.securify;

import java.util.TreeMap;
import ch.securify.analysis.SecurifyErrors;

public class SolidityResult {
    TreeMap<String, SmallPatternResult> results = new TreeMap<>();

    SecurifyErrors securifyErrors = null;
}
