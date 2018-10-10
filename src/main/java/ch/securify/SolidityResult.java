package ch.securify;

import java.util.TreeMap;

import ch.securify.model.SecurifyError;

public class SolidityResult {
    TreeMap<String, SmallPatternResult> results = new TreeMap<>();

    SecurifyError securifyError = null;
}
