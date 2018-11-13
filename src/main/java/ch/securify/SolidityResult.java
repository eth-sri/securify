package ch.securify;

import java.util.TreeMap;

public class SolidityResult {
    TreeMap<String, SmallPatternResult> results;

    SolidityResult() {
        results = new TreeMap<>();
    }
}
