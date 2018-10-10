package ch.securify;

import java.util.TreeMap;
import ch.securify.analysis.SecurifyError;

public class SolidityResult {
    TreeMap<String, SmallPatternResult> results = new TreeMap<>();

    SecurifyError securifyError = null;
}
